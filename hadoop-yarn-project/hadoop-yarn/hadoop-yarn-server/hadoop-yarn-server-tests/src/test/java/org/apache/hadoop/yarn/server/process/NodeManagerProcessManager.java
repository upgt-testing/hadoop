/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.process;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process manager for NodeManager nodes. This class handles the lifecycle
 * of a NodeManager process running in a separate JVM.
 *
 * <p>The NodeManager process is started using the
 * {@link org.apache.hadoop.yarn.server.process.launcher.NodeManagerProcessLauncher}
 * entry point. Health checks are performed by querying the ResourceManager
 * for this NodeManager's status via YarnClient.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * YarnConfiguration conf = new YarnConfiguration();
 * File workDir = new File("/tmp/nm0");
 * NodeManagerProcessManager nm = new NodeManagerProcessManager(
 *     conf, "/opt/hadoop-3.4.0", workDir, 0);
 * nm.start();
 * // ... use the NM ...
 * nm.stop();
 * </pre>
 *
 * @see ProcessNodeManager
 * @see ResourceManagerProcessManager
 */
public class NodeManagerProcessManager extends ProcessNodeManager {

  private static final Logger LOG =
      LoggerFactory.getLogger(NodeManagerProcessManager.class);

  /** Name of the NodeManager process launcher class */
  private static final String NM_LAUNCHER_CLASS =
      "org.apache.hadoop.yarn.server.process.launcher.NodeManagerProcessLauncher";

  /** RPC address of this NodeManager */
  private InetSocketAddress rpcAddress;

  /** Web UI address of this NodeManager */
  private InetSocketAddress webAddress;

  /** NodeId assigned to this NM (host:port), set after registration */
  private NodeId nodeId;

  /**
   * Constructs a new NodeManagerProcessManager.
   *
   * @param nodeConfig Configuration for this NM
   * @param hadoopHome Path to Hadoop distribution
   * @param workDir Working directory for this NM
   * @param nodeIndex Index of this NM
   */
  public NodeManagerProcessManager(YarnConfiguration nodeConfig,
      String hadoopHome, File workDir, int nodeIndex) {
    super(nodeConfig, hadoopHome, workDir, nodeIndex);

    // Extract addresses from configuration
    String rpcAddressStr = nodeConfig.get(YarnConfiguration.NM_ADDRESS);
    if (rpcAddressStr != null) {
      this.rpcAddress = parseAddress(rpcAddressStr);
    }

    String webAddressStr = nodeConfig.get(YarnConfiguration.NM_WEBAPP_ADDRESS);
    if (webAddressStr != null) {
      this.webAddress = parseAddress(webAddressStr);
    }
  }

  @Override
  public void start() throws IOException {
    LOG.info("Starting NodeManager process {} (Hadoop home: {})",
        nodeIndex, hadoopHome);

    if (process != null && process.isAlive()) {
      throw new IOException(
          "NodeManager process " + nodeIndex + " is already running");
    }

    // Build classpath
    List<String> classpath = buildClasspath();
    String classpathStr = String.join(File.pathSeparator, classpath);

    // Build command line
    List<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home") + "/bin/java");

    // JVM options
    command.add("-Xmx1024m");
    command.add("-Xms512m");
    command.add("-XX:+HeapDumpOnOutOfMemoryError");

    // System properties
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dyarn.log.dir=" + new File(workDir, "logs").getAbsolutePath());

    // Native library path
    File nativeLibDir = new File(hadoopHome, "lib/native");
    if (nativeLibDir.exists()) {
      command.add("-Djava.library.path=" + nativeLibDir.getAbsolutePath());
    }

    // Classpath
    command.add("-cp");
    command.add(classpathStr);

    // Main class
    command.add(NM_LAUNCHER_CLASS);

    // Arguments to launcher
    command.add("--config-dir");
    command.add(new File(workDir, "conf").getAbsolutePath());
    command.add("--nm-index");
    command.add(String.valueOf(nodeIndex));

    LOG.info("Starting NM process with command: {}", command);

    // Start the process
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(false);

    try {
      process = pb.start();
      LOG.info("NodeManager process {} started", nodeIndex);

      // Start output reader threads
      startOutputReaderThreads("NM-" + nodeIndex);

      // Wait for NM to be ready (registered with RM)
      waitForProcessReady(DEFAULT_STARTUP_TIMEOUT_MS);

      LOG.info("NodeManager process {} is ready", nodeIndex);
    } catch (IOException | TimeoutException e) {
      LOG.error("Failed to start NodeManager process {}", nodeIndex, e);
      killProcess();
      throw new IOException(
          "Failed to start NodeManager process " + nodeIndex, e);
    }
  }

  @Override
  public void stop() throws IOException {
    LOG.info("Stopping NodeManager process {}", nodeIndex);

    if (process == null || !process.isAlive()) {
      LOG.info("NodeManager process {} is not running", nodeIndex);
      return;
    }

    // Try graceful shutdown first
    try {
      LOG.info("Attempting graceful shutdown of NM process {}", nodeIndex);
      // TODO: Use admin interface to shutdown gracefully
      // For now, we'll use process.destroy() which sends SIGTERM
      process.destroy();

      boolean exited = process.waitFor(DEFAULT_SHUTDOWN_TIMEOUT_MS,
          TimeUnit.MILLISECONDS);
      if (exited) {
        LOG.info("NodeManager process {} stopped gracefully", nodeIndex);
        return;
      }

      LOG.warn("NodeManager process {} did not stop gracefully, " +
          "forcing shutdown", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Interrupted while waiting for NM process {} to stop",
          nodeIndex);
    }

    // Force kill if graceful shutdown failed
    killProcess();
  }

  @Override
  public boolean isHealthy() throws IOException {
    if (!isProcessAlive()) {
      return false;
    }

    // Check health via socket connectivity to RPC port
    if (rpcAddress != null) {
      try (Socket socket = new Socket()) {
        // Try to connect to the RPC address with a short timeout
        socket.connect(rpcAddress, 1000);
        LOG.debug("NM process {} health check passed, RPC port {} is listening",
            nodeIndex, rpcAddress.getPort());
        return true;
      } catch (IOException e) {
        LOG.debug("NM process {} health check failed: RPC port {} not accessible: {}",
            nodeIndex, rpcAddress.getPort(), e.getMessage());
        return false;
      }
    }

    LOG.debug("NM process {} has no RPC address configured yet", nodeIndex);
    return false;
  }

  @Override
  public InetSocketAddress getRpcAddress() {
    return rpcAddress;
  }

  @Override
  protected String getNodeType() {
    return "NodeManager";
  }

  /**
   * Gets the web UI address of this NodeManager.
   *
   * @return the web address
   */
  public InetSocketAddress getWebAddress() {
    return webAddress;
  }

  /**
   * Gets the NodeId assigned to this NM after registration.
   *
   * @return the NodeId, or null if not registered yet
   */
  public NodeId getNodeId() {
    return nodeId;
  }

  /**
   * Parses a host:port address string into an InetSocketAddress.
   *
   * @param addressStr The address string (e.g., "localhost:8042")
   * @return the parsed address
   */
  private InetSocketAddress parseAddress(String addressStr) {
    String[] parts = addressStr.split(":");
    if (parts.length == 2) {
      return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    } else {
      LOG.warn("Invalid address format: {}", addressStr);
      return null;
    }
  }

  /**
   * Sets the RPC address for this NM. This is useful for testing or when
   * the address is determined dynamically.
   *
   * @param rpcAddress the RPC address to set
   */
  public void setRpcAddress(InetSocketAddress rpcAddress) {
    this.rpcAddress = rpcAddress;
  }

  /**
   * Sets the web UI address for this NM.
   *
   * @param webAddress the web address to set
   */
  public void setWebAddress(InetSocketAddress webAddress) {
    this.webAddress = webAddress;
  }
}

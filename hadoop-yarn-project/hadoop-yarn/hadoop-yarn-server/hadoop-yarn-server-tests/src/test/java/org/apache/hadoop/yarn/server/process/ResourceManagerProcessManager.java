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

import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process manager for ResourceManager nodes. This class handles the lifecycle
 * of a ResourceManager process running in a separate JVM.
 *
 * <p>The ResourceManager process is started using the
 * {@link org.apache.hadoop.yarn.server.process.launcher.ResourceManagerProcessLauncher}
 * entry point. Health checks are performed by testing socket connectivity
 * to the RPC port.</p>
 *
 * <p>Supports both standalone and HA (High Availability) modes.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * YarnConfiguration conf = new YarnConfiguration();
 * File workDir = new File("/tmp/rm0");
 * ResourceManagerProcessManager rm = new ResourceManagerProcessManager(
 *     conf, "/opt/hadoop-3.3.6", workDir, 0, false, null);
 * rm.start();
 * // ... use the RM ...
 * rm.stop();
 * </pre>
 *
 * @see ProcessNodeManager
 * @see NodeManagerProcessManager
 */
public class ResourceManagerProcessManager extends ProcessNodeManager {

  private static final Logger LOG =
      LoggerFactory.getLogger(ResourceManagerProcessManager.class);

  /** Name of the ResourceManager process launcher class */
  private static final String RM_LAUNCHER_CLASS =
      "org.apache.hadoop.yarn.server.process.launcher.ResourceManagerProcessLauncher";

  /** Whether this RM is part of an HA setup */
  private final boolean haEnabled;

  /** The RM ID in HA mode (e.g., "rm1", "rm2"), null in standalone mode */
  private final String rmId;

  /** RPC address of this ResourceManager */
  private InetSocketAddress rpcAddress;

  /**
   * Constructs a new ResourceManagerProcessManager for standalone mode.
   *
   * @param nodeConfig Configuration for this RM
   * @param hadoopHome Path to Hadoop distribution
   * @param workDir Working directory for this RM
   * @param nodeIndex Index of this RM
   */
  public ResourceManagerProcessManager(YarnConfiguration nodeConfig,
      String hadoopHome, File workDir, int nodeIndex) {
    this(nodeConfig, hadoopHome, workDir, nodeIndex, false, null);
  }

  /**
   * Constructs a new ResourceManagerProcessManager with HA support.
   *
   * @param nodeConfig Configuration for this RM
   * @param hadoopHome Path to Hadoop distribution
   * @param workDir Working directory for this RM
   * @param nodeIndex Index of this RM
   * @param haEnabled Whether HA is enabled
   * @param rmId The RM ID in HA mode (e.g., "rm1")
   */
  public ResourceManagerProcessManager(YarnConfiguration nodeConfig,
      String hadoopHome, File workDir, int nodeIndex,
      boolean haEnabled, String rmId) {
    super(nodeConfig, hadoopHome, workDir, nodeIndex);
    this.haEnabled = haEnabled;
    this.rmId = rmId;

    // Extract RPC address from configuration
    if (haEnabled && rmId != null) {
      // HA mode: yarn.resourcemanager.address.{rmId}
      String key = YarnConfiguration.RM_ADDRESS + "." + rmId;
      String addressStr = nodeConfig.get(key);
      if (addressStr != null) {
        this.rpcAddress = parseAddress(addressStr);
      } else {
        LOG.warn("RM HA address not found for key: {}", key);
      }
    } else {
      // Standalone mode: yarn.resourcemanager.address
      String addressStr = nodeConfig.get(YarnConfiguration.RM_ADDRESS);
      if (addressStr != null) {
        this.rpcAddress = parseAddress(addressStr);
      }
    }
  }

  @Override
  public void start() throws IOException {
    LOG.info("Starting ResourceManager process {} (Hadoop home: {})",
        nodeIndex, hadoopHome);

    if (process != null && process.isAlive()) {
      throw new IOException(
          "ResourceManager process " + nodeIndex + " is already running");
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
    command.add(RM_LAUNCHER_CLASS);

    // Arguments to launcher
    command.add("--config-dir");
    command.add(new File(workDir, "conf").getAbsolutePath());
    command.add("--rm-index");
    command.add(String.valueOf(nodeIndex));

    if (haEnabled) {
      command.add("--ha-enabled");
      if (rmId != null) {
        command.add("--rm-id");
        command.add(rmId);
      }
    }

    LOG.info("Starting RM process with command: {}", command);

    // Start the process
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(false);

    try {
      process = pb.start();
      LOG.info("ResourceManager process {} started", nodeIndex);

      // Start output reader threads
      startOutputReaderThreads("RM-" + nodeIndex);

      // Wait for RM to be ready
      waitForProcessReady(DEFAULT_STARTUP_TIMEOUT_MS);

      LOG.info("ResourceManager process {} is ready", nodeIndex);
    } catch (IOException | TimeoutException e) {
      LOG.error("Failed to start ResourceManager process {}", nodeIndex, e);
      killProcess();
      throw new IOException(
          "Failed to start ResourceManager process " + nodeIndex, e);
    }
  }

  @Override
  public void stop() throws IOException {
    LOG.info("Stopping ResourceManager process {}", nodeIndex);

    if (process == null || !process.isAlive()) {
      LOG.info("ResourceManager process {} is not running", nodeIndex);
      return;
    }

    // Try graceful shutdown first
    try {
      LOG.info("Attempting graceful shutdown of RM process {}", nodeIndex);
      // TODO: Use RMAdminProtocol to shutdown gracefully
      // For now, we'll use process.destroy() which sends SIGTERM
      process.destroy();

      boolean exited = process.waitFor(DEFAULT_SHUTDOWN_TIMEOUT_MS,
          TimeUnit.MILLISECONDS);
      if (exited) {
        LOG.info("ResourceManager process {} stopped gracefully", nodeIndex);
        return;
      }

      LOG.warn("ResourceManager process {} did not stop gracefully, " +
          "forcing shutdown", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Interrupted while waiting for RM process {} to stop",
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
    try (Socket socket = new Socket()) {
      // Try to connect to the RPC address with a short timeout
      socket.connect(rpcAddress, 1000);
      LOG.debug("RM process {} health check passed, RPC port {} is listening",
          nodeIndex, rpcAddress.getPort());
      return true;
    } catch (IOException e) {
      LOG.debug("RM process {} health check failed: RPC port {} not accessible: {}",
          nodeIndex, rpcAddress.getPort(), e.getMessage());
      return false;
    }
  }

  @Override
  public InetSocketAddress getRpcAddress() {
    return rpcAddress;
  }

  @Override
  protected String getNodeType() {
    return "ResourceManager";
  }

  /**
   * Gets whether this RM is in HA mode.
   *
   * @return true if HA is enabled
   */
  public boolean isHAEnabled() {
    return haEnabled;
  }

  /**
   * Gets the RM ID in HA mode.
   *
   * @return the RM ID, or null if not in HA mode
   */
  public String getRmId() {
    return rmId;
  }

  /**
   * Parses a host:port address string into an InetSocketAddress.
   *
   * @param addressStr The address string (e.g., "localhost:8032")
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
   * Sets the RPC address for this RM. This is useful for testing or when
   * the address is determined dynamically.
   *
   * @param rpcAddress the RPC address to set
   */
  public void setRpcAddress(InetSocketAddress rpcAddress) {
    this.rpcAddress = rpcAddress;
  }
}

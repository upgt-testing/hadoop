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
package org.apache.hadoop.hdfs.server.process;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.ipc.RPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages a NameNode process running in a separate JVM.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Building NameNode-specific classpath</li>
 *   <li>Launching NameNode with proper configuration</li>
 *   <li>Health monitoring via ClientProtocol RPC</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Configuration conf = new Configuration();
 * conf.set(DFS_NAMENODE_RPC_ADDRESS_KEY, "localhost:9000");
 *
 * NameNodeProcessManager manager = new NameNodeProcessManager(
 *     conf, "/opt/hadoop-3.3.5", new File("/tmp/nn0"), 0);
 *
 * manager.start();
 * // ... use namenode ...
 * manager.stop();
 * </pre>
 */
public class NameNodeProcessManager extends ProcessNodeManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(NameNodeProcessManager.class);

  /** RPC address of this NameNode */
  private InetSocketAddress rpcAddress;

  /** ClientProtocol proxy for health checks */
  private ClientProtocol clientProxy;

  /**
   * Constructor for NameNodeProcessManager.
   *
   * @param nodeConfig Configuration for this NameNode
   * @param hadoopHome Path to Hadoop distribution directory
   * @param workDir Working directory for this NameNode
   * @param nodeIndex Index of this NameNode
   */
  public NameNodeProcessManager(Configuration nodeConfig, String hadoopHome,
                                 File workDir, int nodeIndex) {
    super(nodeConfig, hadoopHome, workDir, nodeIndex);

    // Extract RPC address from configuration
    String rpcAddressStr = nodeConfig.get(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY);
    if (rpcAddressStr != null) {
      String[] parts = rpcAddressStr.split(":");
      String host = parts[0];
      int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9000; // Default NN RPC port
      this.rpcAddress = new InetSocketAddress(host, port);
    } else {
      throw new IllegalArgumentException(
          "NameNode RPC address not configured: " +
          DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY);
    }

    LOG.info("Created NameNodeProcessManager for node {} at {}",
        nodeIndex, rpcAddress);
  }

  @Override
  protected String getNodeType() {
    return "NameNode";
  }

  @Override
  public InetSocketAddress getRpcAddress() {
    return rpcAddress;
  }

  @Override
  protected List<String> buildClasspath() throws IOException {
    List<String> classpath = new ArrayList<>();

    File hadoopHomeDir = new File(hadoopHome);

    // 1. Hadoop common JARs
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common/lib"));

    // 2. HDFS JARs
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs/lib"));

    // 3. Configuration directory
    File confDir = new File(workDir, "conf");
    classpath.add(confDir.getAbsolutePath());

    LOG.debug("Built classpath with {} entries for NameNode {}",
        classpath.size(), nodeIndex);

    return classpath;
  }

  @Override
  protected List<String> buildCommand(List<String> classpath) {
    List<String> command = new ArrayList<>();

    // 1. Java executable
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());

    // 2. JVM options
    command.add("-Xmx1g"); // Heap size
    command.add("-Xms512m");

    // 3. System properties
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-namenode.log");
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.id.str=nn" + nodeIndex);
    command.add("-Dhadoop.root.logger=INFO,console");

    // 4. Classpath
    command.add("-cp");
    command.add(String.join(File.pathSeparator, classpath));

    // 5. Main class - using NameNode directly
    command.add("org.apache.hadoop.hdfs.server.namenode.NameNode");

    // 6. Arguments - none for regular startup
    // (configuration is passed via config files, not command line)

    LOG.debug("Built command with {} elements for NameNode {}",
        command.size(), nodeIndex);

    return command;
  }

  @Override
  protected boolean checkRpcHealth() throws IOException {
    try {
      // Create RPC proxy if not already created
      if (clientProxy == null) {
        clientProxy = createClientProxy();
      }

      // Try a simple RPC call - getFileInfo on root
      clientProxy.getFileInfo("/");

      return true;

    } catch (IOException e) {
      // RPC not ready yet or connection failed
      LOG.trace("RPC health check failed for NameNode {}: {}",
          nodeIndex, e.getMessage());

      // Close and recreate proxy on next attempt
      closeClientProxy();

      throw e;
    }
  }

  /**
   * Create a ClientProtocol RPC proxy to the NameNode.
   *
   * @return ClientProtocol proxy
   * @throws IOException if proxy creation fails
   */
  private ClientProtocol createClientProxy() throws IOException {
    LOG.debug("Creating ClientProtocol proxy to NameNode {} at {}",
        nodeIndex, rpcAddress);

    // Create a minimal configuration for RPC client
    Configuration rpcConf = new Configuration(nodeConfig);

    // Set the NameNode address for the proxy
    String nameservice = "localhost:" + rpcAddress.getPort();
    rpcConf.set("fs.defaultFS", "hdfs://" + nameservice);

    // Short timeouts for health checks
    rpcConf.setInt("ipc.client.connect.timeout", 5000); // 5 seconds
    rpcConf.setInt("ipc.client.connect.max.retries", 3);

    // Use NameNodeProxies to create proxy with correct RPC engine
    try {
      java.net.URI nameNodeUri = new java.net.URI("hdfs://" + rpcAddress.getHostString() + ":" + rpcAddress.getPort());
      return org.apache.hadoop.hdfs.NameNodeProxies.createProxy(
          rpcConf,
          nameNodeUri,
          ClientProtocol.class).getProxy();
    } catch (java.net.URISyntaxException e) {
      throw new IOException("Invalid NameNode URI", e);
    }
  }

  /**
   * Close the ClientProtocol RPC proxy.
   */
  private void closeClientProxy() {
    if (clientProxy != null) {
      try {
        RPC.stopProxy(clientProxy);
      } catch (Exception e) {
        LOG.trace("Error closing ClientProtocol proxy: {}", e.getMessage());
      } finally {
        clientProxy = null;
      }
    }
  }

  @Override
  protected boolean tryGracefulShutdown() {
    // TODO: Implement RPC-based shutdown
    // For now, rely on SIGTERM which NameNode handles gracefully
    LOG.info("Attempting graceful shutdown of NameNode {}", nodeIndex);

    // Close RPC proxy first
    closeClientProxy();

    // Send SIGTERM (handled by process.destroy() in base class)
    if (process != null) {
      process.destroy();
    }

    // Wait for process to exit
    return waitForExit(GRACEFUL_SHUTDOWN_TIMEOUT_MS);
  }

  @Override
  protected void cleanup() {
    closeClientProxy();
    super.cleanup();
  }

  /**
   * Add all JAR files from a directory to the classpath.
   *
   * @param classpath classpath list to add to
   * @param dir directory to scan
   */
  private void addJarsFromDir(List<String> classpath, File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      LOG.warn("Directory does not exist, skipping: {}", dir);
      return;
    }

    File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
    if (jars != null) {
      Arrays.sort(jars); // Consistent ordering
      for (File jar : jars) {
        classpath.add(jar.getAbsolutePath());
      }
    }
  }

  /**
   * Formats the NameNode filesystem.
   *
   * @throws IOException if format fails
   */
  public void format() throws IOException {
    LOG.info("Formatting NameNode {}", nodeIndex);

    // Build format command
    List<String> command = new ArrayList<>();
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());
    command.add("-Xmx1g");
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-format.log");
    command.add("-cp");
    command.add(String.join(File.pathSeparator, buildClasspath()));
    command.add("org.apache.hadoop.hdfs.server.namenode.NameNode");
    command.add("-format");
    command.add("-nonInteractive");

    LOG.debug("Format command: {}", String.join(" ", command));

    // Run format command
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true);

    // Set up environment
    java.util.Map<String, String> env = pb.environment();
    setupProcessEnvironment(env);

    // Redirect output to log file
    File formatLog = new File(workDir, "logs/format.log");
    formatLog.getParentFile().mkdirs();
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(formatLog));

    Process formatProcess = pb.start();
    try {
      int exitCode = formatProcess.waitFor();
      if (exitCode != 0) {
        throw new IOException("NameNode format failed with exit code " + exitCode);
      }
      LOG.info("NameNode {} formatted successfully", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while formatting NameNode", e);
    }
  }

  /**
   * Get the HTTP address for this NameNode.
   *
   * @return HTTP address
   */
  public InetSocketAddress getHttpAddress() {
    String httpAddressStr = nodeConfig.get(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY);
    if (httpAddressStr != null) {
      String[] parts = httpAddressStr.split(":");
      return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }
    return null;
  }
}

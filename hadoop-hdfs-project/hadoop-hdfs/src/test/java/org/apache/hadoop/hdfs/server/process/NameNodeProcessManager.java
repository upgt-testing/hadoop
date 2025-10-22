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
import org.apache.hadoop.ha.HAServiceProtocol;
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

  /** Cached namenode ID (captured at construction) */
  private final String initialNameNodeId;

  /** Cached nameservice ID (captured at construction) */
  private final String initialNameserviceId;

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

    this.initialNameNodeId = nodeConfig.getTrimmed(DFSConfigKeys.DFS_HA_NAMENODE_ID_KEY);
    this.initialNameserviceId = resolveNameserviceId(nodeConfig);

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

  /**
   * Builds a classpath for client tools (like HAAdmin) that excludes
   * the server configuration directory. This prevents server configuration
   * from polluting client configuration.
   *
   * @return list of classpath entries
   * @throws IOException if classpath construction fails
   */
  protected List<String> buildClientClasspath() throws IOException {
    List<String> classpath = new ArrayList<>();

    File hadoopHomeDir = new File(hadoopHome);

    // 1. Hadoop common JARs
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common/lib"));

    // 2. HDFS JARs
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs/lib"));

    // NOTE: We do NOT add the configuration directory to the classpath
    // Configuration will be loaded solely from hadoop.conf.dir system property

    LOG.debug("Built client classpath with {} entries (no config dir) for NameNode {}",
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
      // For HA-enabled NameNodes, just check if RPC port is open
      // HA NameNodes start in standby and may not respond to all RPCs immediately
      if (isHAEnabled()) {
        // Simple socket connectivity check
        try (java.net.Socket socket = new java.net.Socket()) {
          socket.connect(rpcAddress, 1000); // 1 second timeout
          LOG.info("NameNode {} RPC port is responsive", nodeIndex);
          return true;
        } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
          throw new IOException("RPC port not yet available: " + e.getMessage());
        }
      } else {
        // For non-HA NameNodes, use ClientProtocol
        if (clientProxy == null) {
          clientProxy = createClientProxy();
        }

        // Try a simple RPC call - getFileInfo on root
        clientProxy.getFileInfo("/");

        return true;
      }

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

  /**
   * Check if HA is enabled for this NameNode.
   *
   * @return true if HA is enabled, false otherwise
   */
  private boolean isHAEnabled() {
    String nameservices = nodeConfig.get(DFSConfigKeys.DFS_NAMESERVICES);
    return nameservices != null && !nameservices.trim().isEmpty();
  }

  /**
   * Create an HAServiceProtocol RPC proxy to the NameNode.
   * This is used for health checks on HA-enabled NameNodes.
   *
   * @return HAServiceProtocol proxy
   * @throws IOException if proxy creation fails
   */
  private HAServiceProtocol createHAServiceProxy() throws IOException {
    LOG.debug("Creating HAServiceProtocol proxy to NameNode {} at {}",
        nodeIndex, rpcAddress);

    // Create a minimal configuration for RPC client
    Configuration rpcConf = new Configuration(nodeConfig);

    // Short timeouts for health checks
    rpcConf.setInt("ipc.client.connect.timeout", 5000); // 5 seconds
    rpcConf.setInt("ipc.client.connect.max.retries", 3);

    // Use NameNodeProxies to create proxy with correct RPC engine (ProtobufRpcEngine2)
    try {
      java.net.URI nameNodeUri = new java.net.URI("hdfs://" + rpcAddress.getHostString() + ":" + rpcAddress.getPort());
      return org.apache.hadoop.hdfs.NameNodeProxies.createNonHAProxy(
          rpcConf,
          rpcAddress,
          HAServiceProtocol.class,
          org.apache.hadoop.security.UserGroupInformation.getCurrentUser(),
          false).getProxy();
    } catch (java.net.URISyntaxException e) {
      throw new IOException("Invalid NameNode URI", e);
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
    command.add("-force");  // Force format for HA configurations

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
        // Read the format log to include in error message
        String logContents = readLastLinesFromFile(formatLog, 50);
        throw new IOException("NameNode format failed with exit code " + exitCode +
            ". Format log:\n" + logContents);
      }
      LOG.info("NameNode {} formatted successfully", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while formatting NameNode", e);
    }
  }

  /**
   * Initializes shared edits directory for HA setup.
   * This should be called on the active NameNode after formatting.
   *
   * @throws IOException if initialization fails
   */
  public void initializeSharedEdits() throws IOException {
    LOG.info("Initializing shared edits for NameNode {}", nodeIndex);

    // Build initializeSharedEdits command
    List<String> command = new ArrayList<>();
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());
    command.add("-Xmx1g");
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-initshared.log");
    command.add("-cp");
    command.add(String.join(File.pathSeparator, buildClasspath()));
    command.add("org.apache.hadoop.hdfs.server.namenode.NameNode");
    command.add("-initializeSharedEdits");
    command.add("-force");

    LOG.debug("InitializeSharedEdits command: {}", String.join(" ", command));

    // Run command
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true);

    // Set up environment
    java.util.Map<String, String> env = pb.environment();
    setupProcessEnvironment(env);

    // Redirect output to log file
    File initLog = new File(workDir, "logs/initshared.log");
    initLog.getParentFile().mkdirs();
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(initLog));

    Process initProcess = pb.start();

    // Provide "Y" input to confirm re-format if needed
    try (java.io.OutputStream stdin = initProcess.getOutputStream()) {
      stdin.write("Y\n".getBytes());
      stdin.flush();
    } catch (IOException e) {
      LOG.warn("Failed to write to initializeSharedEdits stdin", e);
    }

    try {
      int exitCode = initProcess.waitFor();
      if (exitCode != 0) {
        // Read the log to include in error message
        String logContents = readLastLinesFromFile(initLog, 50);
        throw new IOException("InitializeSharedEdits failed with exit code " + exitCode +
            ". Log:\n" + logContents);
      }
      LOG.info("NameNode {} shared edits initialized successfully", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while initializing shared edits", e);
    }
  }

  /**
   * Bootstraps a standby NameNode from the active NameNode's state.
   * This should be called on standby NameNodes before starting them.
   *
   * @throws IOException if bootstrap fails
   */
  public void bootstrapStandby() throws IOException {
    LOG.info("Bootstrapping standby NameNode {}", nodeIndex);

    // Build bootstrapStandby command
    List<String> command = new ArrayList<>();
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());
    command.add("-Xmx1g");
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-bootstrap.log");
    command.add("-cp");
    command.add(String.join(File.pathSeparator, buildClasspath()));
    command.add("org.apache.hadoop.hdfs.server.namenode.NameNode");
    command.add("-bootstrapStandby");
    command.add("-nonInteractive");

    LOG.debug("BootstrapStandby command: {}", String.join(" ", command));

    // Run command
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true);

    // Set up environment
    java.util.Map<String, String> env = pb.environment();
    setupProcessEnvironment(env);

    // Redirect output to log file
    File bootstrapLog = new File(workDir, "logs/bootstrap.log");
    bootstrapLog.getParentFile().mkdirs();
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(bootstrapLog));

    Process bootstrapProcess = pb.start();
    try {
      int exitCode = bootstrapProcess.waitFor();
      if (exitCode != 0) {
        throw new IOException("BootstrapStandby failed with exit code " + exitCode);
      }
      LOG.info("NameNode {} bootstrapped successfully", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while bootstrapping standby", e);
    }
  }

  /**
   * Transitions this NameNode to active state in an HA setup.
   * This should be called on the NameNode that should become active.
   *
   * @throws IOException if transition fails
   */
  public void transitionToActive() throws IOException {
    String namenodeId = getEffectiveNameNodeId();
    String nameserviceId = getEffectiveNameserviceId();

    LOG.info("Transitioning NameNode {} (ID: {}, nameservice: {}) to active state",
             nodeIndex, namenodeId, nameserviceId);

    // Create a client-only configuration directory without dfs.ha.namenode.id
    // (that property makes HAAdmin think it IS a specific NameNode, causing it to target the wrong one)
    File clientConfDir = createClientConfiguration();

    LOG.info("=== DEBUGGING INFO ===");
    LOG.info("Cluster directory: {}", workDir.getParentFile());
    LOG.info("NameNode work directory: {}", workDir);
    LOG.info("Client configuration directory: {}", clientConfDir);

    // Build haadmin command
    List<String> command = new ArrayList<>();
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());
    command.add("-Xmx1g");
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.conf.dir=" + clientConfDir.getAbsolutePath());
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-transitiontoactive.log");
    command.add("-cp");
    // Build classpath WITHOUT the server configuration directory
    // Add the client-conf directory to the classpath so hdfs-site.xml is loadable
    List<String> classpath = buildClientClasspath();
    classpath.add(clientConfDir.getAbsolutePath());
    command.add(String.join(File.pathSeparator, classpath));
    command.add("org.apache.hadoop.hdfs.tools.DFSHAAdmin");
    command.add("-ns");
    command.add(nameserviceId);
    command.add("-transitionToActive");
    command.add(namenodeId);

    LOG.info("=== HAAdmin Command ===");
    LOG.info("{}", String.join(" ", command));

    // Run command
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true);

    // Set up environment
    java.util.Map<String, String> env = pb.environment();
    setupProcessEnvironment(env);

    // CRITICAL: Override HADOOP_CONF_DIR to point to client-conf instead of server conf
    // The setupProcessEnvironment() method sets it to the server conf dir, but we need
    // HAAdmin to use the client configuration
    env.put("HADOOP_CONF_DIR", clientConfDir.getAbsolutePath());
    LOG.info("Overriding HADOOP_CONF_DIR to client configuration: {}", clientConfDir.getAbsolutePath());

    // Redirect output to log file
    File transitionLog = new File(workDir, "logs/transitiontoactive.log");
    transitionLog.getParentFile().mkdirs();
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(transitionLog));

    Process transitionProcess = pb.start();
    try {
      int exitCode = transitionProcess.waitFor();
      if (exitCode != 0) {
        String logContents = readLastLinesFromFile(transitionLog, 50);
        throw new IOException("TransitionToActive failed with exit code " + exitCode +
            ". Log:\n" + logContents);
      }
      LOG.info("NameNode {} transitioned to active successfully", nodeIndex);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while transitioning to active", e);
    }
  }

  /**
   * Creates a client-only configuration directory for HAAdmin commands.
   * This configuration includes ONLY nameservice-qualified keys and excludes all
   * global daemon keys (dfs.namenode.rpc-address, dfs.namenode.servicerpc-address, etc.)
   * to prevent HAAdmin from using the wrong service RPC address.
   *
   * @return File pointing to the client configuration directory
   * @throws IOException if unable to create the configuration
   */
  private File createClientConfiguration() throws IOException {
    File clientConfDir = new File(workDir, "client-conf");
    if (!clientConfDir.exists() && !clientConfDir.mkdirs()) {
      throw new IOException("Failed to create client conf directory: " + clientConfDir);
    }

    // Create configuration WITH defaults (to load hdfs-default.xml, core-default.xml, etc.)
    // but then copy over client-side keys from nodeConfig
    Configuration clientConfig = new Configuration();

    // Extract nameservice and HA configuration
    String nameservice = nodeConfig.get(DFSConfigKeys.DFS_NAMESERVICES);
    if (nameservice != null) {
      clientConfig.set(DFSConfigKeys.DFS_NAMESERVICES, nameservice);

      // Add HA NameNode list
      String namenodes = nodeConfig.get(DFSConfigKeys.DFS_HA_NAMENODES_KEY_PREFIX + "." + nameservice);
      if (namenodes != null) {
        clientConfig.set(DFSConfigKeys.DFS_HA_NAMENODES_KEY_PREFIX + "." + nameservice, namenodes);

        // Add nameservice-qualified addresses for each NameNode
        for (String nnId : namenodes.split(",")) {
          // RPC address
          String rpcKey = DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY + "." + nameservice + "." + nnId;
          String rpcValue = nodeConfig.get(rpcKey);
          if (rpcValue != null) {
            clientConfig.set(rpcKey, rpcValue);
          }

          // Service RPC address
          String serviceRpcKey = DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY + "." + nameservice + "." + nnId;
          String serviceRpcValue = nodeConfig.get(serviceRpcKey);
          if (serviceRpcValue != null) {
            clientConfig.set(serviceRpcKey, serviceRpcValue);
          }

          // HTTP address (optional but nice to include)
          String httpKey = DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY + "." + nameservice + "." + nnId;
          String httpValue = nodeConfig.get(httpKey);
          if (httpValue != null) {
            clientConfig.set(httpKey, httpValue);
          }
        }
      }

      // Add failover proxy provider (optional but recommended)
      String failoverProviderKey = DFSConfigKeys.DFS_CLIENT_FAILOVER_PROXY_PROVIDER_KEY_PREFIX + "." + nameservice;
      String failoverProviderValue = nodeConfig.get(failoverProviderKey);
      if (failoverProviderValue != null) {
        clientConfig.set(failoverProviderKey, failoverProviderValue);
      }
    }

    // Explicitly remove all global daemon keys (these should NOT be in client config)
    clientConfig.unset(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY);
    clientConfig.unset(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY);
    clientConfig.unset(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY);
    clientConfig.unset(DFSConfigKeys.DFS_HA_NAMENODE_ID_KEY);
    clientConfig.unset(DFSConfigKeys.DFS_NAMESERVICE_ID);

    // Write the client configuration to hdfs-site.xml
    // We write it manually to ensure ONLY the client-side keys are present
    File clientHdfsSite = new File(clientConfDir, "hdfs-site.xml");
    try (java.io.FileWriter writer = new java.io.FileWriter(clientHdfsSite)) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.write("<configuration>\n");

      // Only write the properties we explicitly want in the client configuration
      if (nameservice != null) {
        writer.write("  <property>\n");
        writer.write("    <name>" + DFSConfigKeys.DFS_NAMESERVICES + "</name>\n");
        writer.write("    <value>" + nameservice + "</value>\n");
        writer.write("  </property>\n");

        String namenodes = nodeConfig.get(DFSConfigKeys.DFS_HA_NAMENODES_KEY_PREFIX + "." + nameservice);
        if (namenodes != null) {
          writer.write("  <property>\n");
          writer.write("    <name>" + DFSConfigKeys.DFS_HA_NAMENODES_KEY_PREFIX + "." + nameservice + "</name>\n");
          writer.write("    <value>" + namenodes + "</value>\n");
          writer.write("  </property>\n");

          for (String nnId : namenodes.split(",")) {
            // RPC address
            String rpcKey = DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY + "." + nameservice + "." + nnId;
            String rpcValue = nodeConfig.get(rpcKey);
            if (rpcValue != null) {
              writer.write("  <property>\n");
              writer.write("    <name>" + rpcKey + "</name>\n");
              writer.write("    <value>" + rpcValue + "</value>\n");
              writer.write("  </property>\n");
            }

            // Service RPC address
            String serviceRpcKey = DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY + "." + nameservice + "." + nnId;
            String serviceRpcValue = nodeConfig.get(serviceRpcKey);
            if (serviceRpcValue != null) {
              writer.write("  <property>\n");
              writer.write("    <name>" + serviceRpcKey + "</name>\n");
              writer.write("    <value>" + serviceRpcValue + "</value>\n");
              writer.write("  </property>\n");
            }

            // HTTP address
            String httpKey = DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY + "." + nameservice + "." + nnId;
            String httpValue = nodeConfig.get(httpKey);
            if (httpValue != null) {
              writer.write("  <property>\n");
              writer.write("    <name>" + httpKey + "</name>\n");
              writer.write("    <value>" + httpValue + "</value>\n");
              writer.write("  </property>\n");
            }
          }
        }

        // Failover proxy provider
        String failoverProviderKey = DFSConfigKeys.DFS_CLIENT_FAILOVER_PROXY_PROVIDER_KEY_PREFIX + "." + nameservice;
        String failoverProviderValue = nodeConfig.get(failoverProviderKey);
        if (failoverProviderValue != null) {
          writer.write("  <property>\n");
          writer.write("    <name>" + failoverProviderKey + "</name>\n");
          writer.write("    <value>" + failoverProviderValue + "</value>\n");
          writer.write("  </property>\n");
        }
      }

      writer.write("</configuration>\n");
    }

    LOG.info("Created pure client configuration at {} with only nameservice-qualified keys", clientConfDir);
    return clientConfDir;
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

  /**
   * Get the Hadoop configuration for this NameNode.
   *
   * @return Configuration object
   */
  public Configuration getConfiguration() {
    return nodeConfig;
  }

  /**
   * Get the working directory for this NameNode.
   *
   * @return working directory File
   */
  public File getWorkDirectory() {
    return workDir;
  }

  private String getEffectiveNameNodeId() throws IOException {
    if (initialNameNodeId != null && !initialNameNodeId.isEmpty()) {
      return initialNameNodeId;
    }

    String configuredId = nodeConfig.getTrimmed(DFSConfigKeys.DFS_HA_NAMENODE_ID_KEY);
    if (configuredId != null && !configuredId.isEmpty()) {
      return configuredId;
    }

    if (isHAEnabled()) {
      // ProcessBasedMiniDFSCluster assigns deterministic IDs (nn1, nn2, ...)
      return "nn" + (nodeIndex + 1);
    }

    throw new IOException("Cannot transition to active: dfs.ha.namenode.id is not set");
  }

  private String getEffectiveNameserviceId() throws IOException {
    if (initialNameserviceId != null && !initialNameserviceId.isEmpty()) {
      return initialNameserviceId;
    }

    String configuredId = resolveNameserviceId(nodeConfig);
    if (configuredId != null && !configuredId.isEmpty()) {
      return configuredId;
    }

    throw new IOException("Cannot transition to active: dfs.nameservice.id is not set");
  }

  private String resolveNameserviceId(Configuration config) {
    String nsId = config.getTrimmed(DFSConfigKeys.DFS_NAMESERVICE_ID);
    if (nsId != null && !nsId.isEmpty()) {
      return nsId;
    }

    String nameservices = config.getTrimmed(DFSConfigKeys.DFS_NAMESERVICES);
    if (nameservices != null && !nameservices.isEmpty()) {
      for (String candidate : nameservices.split(",")) {
        String trimmed = candidate.trim();
        if (!trimmed.isEmpty()) {
          return trimmed;
        }
      }
    }

    return null;
  }

  /**
   * Read last N lines from a file.
   *
   * @param file the file to read
   * @param lines number of lines to read
   * @return the last lines as a string
   */
  private String readLastLinesFromFile(File file, int lines) {
    if (!file.exists()) {
      return "(Log file not found: " + file.getAbsolutePath() + ")";
    }

    try (java.io.BufferedReader reader = new java.io.BufferedReader(
        new java.io.FileReader(file))) {
      java.util.List<String> lineList = new java.util.ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        lineList.add(line);
        if (lineList.size() > lines) {
          lineList.remove(0);
        }
      }
      return String.join("\n", lineList);
    } catch (java.io.IOException e) {
      return "(Error reading log file: " + e.getMessage() + ")";
    }
  }
}

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
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.ipc.RPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages a DataNode process running in a separate JVM.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Building DataNode-specific classpath</li>
 *   <li>Launching DataNode with proper configuration</li>
 *   <li>Health monitoring via IPC port checks and NameNode registration</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Configuration conf = new Configuration();
 * conf.set(DFS_DATANODE_ADDRESS_KEY, "localhost:9866");
 * conf.set(DFS_DATANODE_IPC_ADDRESS_KEY, "localhost:9867");
 * conf.set(DFS_NAMENODE_RPC_ADDRESS_KEY, "localhost:9000");
 *
 * DataNodeProcessManager manager = new DataNodeProcessManager(
 *     conf, "/opt/hadoop-3.3.5", new File("/tmp/dn0"), 0);
 *
 * manager.start();
 * // ... use datanode ...
 * manager.stop();
 * </pre>
 */
public class DataNodeProcessManager extends ProcessNodeManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(DataNodeProcessManager.class);

  /** Data transfer address of this DataNode */
  private InetSocketAddress dataAddress;

  /** IPC address of this DataNode */
  private InetSocketAddress ipcAddress;

  /** HTTP address of this DataNode */
  private InetSocketAddress httpAddress;

  /** NameNode RPC address (for registration checks) */
  private InetSocketAddress nameNodeRpcAddress;

  /** ClientProtocol proxy for checking DataNode registration */
  private ClientProtocol nameNodeProxy;

  /** DataNode UUID (for identification) */
  private String datanodeUuid;

  /**
   * Constructor for DataNodeProcessManager.
   *
   * @param nodeConfig Configuration for this DataNode
   * @param hadoopHome Path to Hadoop distribution directory
   * @param workDir Working directory for this DataNode
   * @param nodeIndex Index of this DataNode
   */
  public DataNodeProcessManager(Configuration nodeConfig, String hadoopHome,
                                 File workDir, int nodeIndex) {
    super(nodeConfig, hadoopHome, workDir, nodeIndex);

    // Extract addresses from configuration
    extractAddresses(nodeConfig);

    LOG.info("Created DataNodeProcessManager for node {} - data: {}, ipc: {}, http: {}",
        nodeIndex, dataAddress, ipcAddress, httpAddress);
  }

  /**
   * Extract DataNode addresses from configuration.
   *
   * @param conf configuration to extract from
   */
  private void extractAddresses(Configuration conf) {
    // Data transfer address
    String dataAddrStr = conf.get(DFSConfigKeys.DFS_DATANODE_ADDRESS_KEY);
    if (dataAddrStr != null) {
      this.dataAddress = parseAddress(dataAddrStr,
          DFSConfigKeys.DFS_DATANODE_ADDRESS_DEFAULT);
    } else {
      this.dataAddress = parseAddress(
          DFSConfigKeys.DFS_DATANODE_ADDRESS_DEFAULT,
          DFSConfigKeys.DFS_DATANODE_ADDRESS_DEFAULT);
    }

    // IPC address
    String ipcAddrStr = conf.get(DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_KEY);
    if (ipcAddrStr != null) {
      this.ipcAddress = parseAddress(ipcAddrStr,
          DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_DEFAULT);
    } else {
      this.ipcAddress = parseAddress(
          DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_DEFAULT,
          DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_DEFAULT);
    }

    // HTTP address
    String httpAddrStr = conf.get(DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_KEY);
    if (httpAddrStr != null) {
      this.httpAddress = parseAddress(httpAddrStr,
          DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_DEFAULT);
    } else {
      this.httpAddress = parseAddress(
          DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_DEFAULT,
          DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_DEFAULT);
    }

    // NameNode RPC address (for registration checks)
    String nnRpcAddrStr = conf.get(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY);
    if (nnRpcAddrStr != null) {
      this.nameNodeRpcAddress = parseAddress(nnRpcAddrStr, "localhost:9000");
    }
  }

  /**
   * Parse an address string into InetSocketAddress.
   *
   * @param addressStr address string (host:port)
   * @param defaultAddr default address if parsing fails
   * @return InetSocketAddress
   */
  private InetSocketAddress parseAddress(String addressStr, String defaultAddr) {
    String addr = addressStr != null ? addressStr : defaultAddr;
    String[] parts = addr.split(":");
    String host = parts[0];
    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    return new InetSocketAddress(host, port);
  }

  @Override
  protected String getNodeType() {
    return "DataNode";
  }

  @Override
  public InetSocketAddress getRpcAddress() {
    return ipcAddress;
  }

  /**
   * Get the data transfer address.
   *
   * @return data transfer address
   */
  public InetSocketAddress getDataAddress() {
    return dataAddress;
  }

  /**
   * Get the HTTP address.
   *
   * @return HTTP address
   */
  public InetSocketAddress getHttpAddress() {
    return httpAddress;
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

    LOG.debug("Built classpath with {} entries for DataNode {}",
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
    command.add("-Dhadoop.log.file=hadoop-datanode.log");
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.id.str=dn" + nodeIndex);
    command.add("-Dhadoop.root.logger=INFO,console");
    command.add("-Dhadoop.security.logger=INFO,console");

    // 4. Classpath
    command.add("-cp");
    command.add(String.join(File.pathSeparator, classpath));

    // 5. Main class - using DataNode directly
    command.add("org.apache.hadoop.hdfs.server.datanode.DataNode");

    // 6. Arguments - none for regular startup
    // (configuration is passed via config files, not command line)

    LOG.debug("Built command with {} elements for DataNode {}",
        command.size(), nodeIndex);

    return command;
  }

  @Override
  protected boolean checkRpcHealth() throws IOException {
    // For DataNode, we have multiple health check strategies:
    // 1. Check if IPC port is listening (basic connectivity)
    // 2. Check if registered with NameNode (requires NN access)

    // Strategy 1: Check if IPC port is listening
    if (!isPortListening(ipcAddress)) {
      throw new IOException("DataNode IPC port not listening: " + ipcAddress);
    }

    // Strategy 2: If we have NameNode address, check registration
    if (nameNodeRpcAddress != null) {
      return checkRegistrationWithNameNode();
    }

    // If we can't check registration, port listening is good enough
    return true;
  }

  /**
   * Check if a port is listening by attempting to connect.
   *
   * @param address address to check
   * @return true if port is listening
   */
  private boolean isPortListening(InetSocketAddress address) {
    try (Socket socket = new Socket()) {
      socket.connect(address, 1000); // 1 second timeout
      return true;
    } catch (IOException e) {
      LOG.trace("Port not listening: {} - {}", address, e.getMessage());
      return false;
    }
  }

  /**
   * Check if this DataNode is registered with the NameNode.
   *
   * @return true if registered
   * @throws IOException if check fails
   */
  private boolean checkRegistrationWithNameNode() throws IOException {
    try {
      // Create NameNode proxy if not already created
      if (nameNodeProxy == null && nameNodeRpcAddress != null) {
        nameNodeProxy = createNameNodeProxy();
      }

      if (nameNodeProxy == null) {
        // Can't check registration without NameNode proxy
        return true;
      }

      // Get list of DataNodes from NameNode
      DatanodeInfo[] datanodes = nameNodeProxy.getDatanodeReport(
          org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType.LIVE);

      // Check if our DataNode is in the list
      // We match by IPC address since UUID might not be available yet
      for (DatanodeInfo dn : datanodes) {
        if (matchesDataNode(dn)) {
          LOG.trace("DataNode {} found in NameNode's live list", nodeIndex);
          return true;
        }
      }

      LOG.trace("DataNode {} not yet registered with NameNode", nodeIndex);
      throw new IOException("DataNode not registered with NameNode");

    } catch (IOException e) {
      LOG.trace("Registration check failed for DataNode {}: {}",
          nodeIndex, e.getMessage());

      // Close and recreate proxy on next attempt
      closeNameNodeProxy();

      throw e;
    }
  }

  /**
   * Check if a DatanodeInfo matches this DataNode.
   *
   * @param dn DatanodeInfo to check
   * @return true if it matches
   */
  private boolean matchesDataNode(DatanodeInfo dn) {
    // Match by IPC address
    if (dn.getIpcPort() == ipcAddress.getPort() &&
        (dn.getIpAddr().equals(ipcAddress.getHostString()) ||
         dn.getHostName().equals(ipcAddress.getHostString()))) {
      return true;
    }

    // Match by data transfer port
    if (dn.getXferPort() == dataAddress.getPort() &&
        (dn.getIpAddr().equals(dataAddress.getHostString()) ||
         dn.getHostName().equals(dataAddress.getHostString()))) {
      return true;
    }

    return false;
  }

  /**
   * Create a ClientProtocol RPC proxy to the NameNode.
   *
   * @return ClientProtocol proxy
   * @throws IOException if proxy creation fails
   */
  private ClientProtocol createNameNodeProxy() throws IOException {
    if (nameNodeRpcAddress == null) {
      return null;
    }

    LOG.debug("Creating ClientProtocol proxy to NameNode at {} for DataNode {}",
        nameNodeRpcAddress, nodeIndex);

    // Create a minimal configuration for RPC client
    Configuration rpcConf = new Configuration();
    rpcConf.set(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY,
        nameNodeRpcAddress.getHostString() + ":" + nameNodeRpcAddress.getPort());

    // Short timeouts for health checks
    rpcConf.setInt("ipc.client.connect.timeout", 5000); // 5 seconds
    rpcConf.setInt("ipc.client.connect.max.retries", 3);

    return RPC.getProxy(
        ClientProtocol.class,
        ClientProtocol.versionID,
        nameNodeRpcAddress,
        rpcConf);
  }

  /**
   * Close the NameNode RPC proxy.
   */
  private void closeNameNodeProxy() {
    if (nameNodeProxy != null) {
      try {
        RPC.stopProxy(nameNodeProxy);
      } catch (Exception e) {
        LOG.trace("Error closing NameNode proxy: {}", e.getMessage());
      } finally {
        nameNodeProxy = null;
      }
    }
  }

  @Override
  protected boolean tryGracefulShutdown() {
    LOG.info("Attempting graceful shutdown of DataNode {}", nodeIndex);

    // Close NameNode proxy first
    closeNameNodeProxy();

    // Send SIGTERM (handled by process.destroy() in base class)
    if (process != null) {
      process.destroy();
    }

    // Wait for process to exit
    return waitForExit(GRACEFUL_SHUTDOWN_TIMEOUT_MS);
  }

  @Override
  protected void cleanup() {
    closeNameNodeProxy();
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
   * Set the DataNode UUID.
   *
   * @param uuid DataNode UUID
   */
  public void setDatanodeUuid(String uuid) {
    this.datanodeUuid = uuid;
  }

  /**
   * Get the DataNode UUID.
   *
   * @return DataNode UUID
   */
  public String getDatanodeUuid() {
    return datanodeUuid;
  }
}

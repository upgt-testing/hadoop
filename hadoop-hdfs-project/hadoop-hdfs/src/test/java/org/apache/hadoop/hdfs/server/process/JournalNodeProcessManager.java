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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a JournalNode process running in a separate JVM.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Building JournalNode-specific classpath</li>
 *   <li>Launching JournalNode with proper configuration</li>
 *   <li>Health monitoring via RPC port checks</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>JournalNodes are used in HDFS High Availability setups to provide
 * a quorum-based shared edit log storage. Multiple NameNodes write to
 * the JournalNode quorum, enabling edit log sharing between active and
 * standby NameNodes.
 *
 * <p>Example usage:
 * <pre>
 * Configuration conf = new Configuration();
 * conf.set(DFS_JOURNALNODE_RPC_ADDRESS_KEY, "localhost:8485");
 * conf.set(DFS_JOURNALNODE_HTTP_ADDRESS_KEY, "localhost:8480");
 * conf.set(DFS_JOURNALNODE_EDITS_DIR_KEY, "/tmp/jn0/edits");
 *
 * JournalNodeProcessManager manager = new JournalNodeProcessManager(
 *     conf, "/opt/hadoop-3.3.5", new File("/tmp/jn0"), 0);
 *
 * manager.start();
 * // ... use journal node ...
 * manager.stop();
 * </pre>
 */
public class JournalNodeProcessManager extends ProcessNodeManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(JournalNodeProcessManager.class);

  /** RPC address of this JournalNode */
  private InetSocketAddress rpcAddress;

  /** HTTP address of this JournalNode */
  private InetSocketAddress httpAddress;

  /** Edits directory for this JournalNode */
  private String editsDir;

  /**
   * Constructor for JournalNodeProcessManager.
   *
   * @param nodeConfig Configuration for this JournalNode
   * @param hadoopHome Path to Hadoop distribution directory
   * @param workDir Working directory for this JournalNode
   * @param nodeIndex Index of this JournalNode
   */
  public JournalNodeProcessManager(Configuration nodeConfig, String hadoopHome,
                                   File workDir, int nodeIndex) {
    super(nodeConfig, hadoopHome, workDir, nodeIndex);

    // Extract addresses from configuration
    extractAddresses(nodeConfig);

    LOG.info("Created JournalNodeProcessManager for node {} - rpc: {}, http: {}, edits: {}",
        nodeIndex, rpcAddress, httpAddress, editsDir);
  }

  /**
   * Extract JournalNode addresses from configuration.
   *
   * @param conf configuration to extract from
   */
  private void extractAddresses(Configuration conf) {
    // RPC address
    String rpcAddrStr = conf.get(DFSConfigKeys.DFS_JOURNALNODE_RPC_ADDRESS_KEY);
    if (rpcAddrStr != null) {
      this.rpcAddress = parseAddress(rpcAddrStr,
          DFSConfigKeys.DFS_JOURNALNODE_RPC_ADDRESS_DEFAULT);
    } else {
      this.rpcAddress = parseAddress(
          DFSConfigKeys.DFS_JOURNALNODE_RPC_ADDRESS_DEFAULT,
          DFSConfigKeys.DFS_JOURNALNODE_RPC_ADDRESS_DEFAULT);
    }

    // HTTP address
    String httpAddrStr = conf.get(DFSConfigKeys.DFS_JOURNALNODE_HTTP_ADDRESS_KEY);
    if (httpAddrStr != null) {
      this.httpAddress = parseAddress(httpAddrStr,
          DFSConfigKeys.DFS_JOURNALNODE_HTTP_ADDRESS_DEFAULT);
    } else {
      this.httpAddress = parseAddress(
          DFSConfigKeys.DFS_JOURNALNODE_HTTP_ADDRESS_DEFAULT,
          DFSConfigKeys.DFS_JOURNALNODE_HTTP_ADDRESS_DEFAULT);
    }

    // Edits directory
    this.editsDir = conf.get(DFSConfigKeys.DFS_JOURNALNODE_EDITS_DIR_KEY);
    if (this.editsDir == null) {
      // Default to workDir/edits if not specified
      this.editsDir = new File(workDir, "edits").getAbsolutePath();
      LOG.warn("JournalNode edits directory not configured, using: {}", this.editsDir);
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
    return "JournalNode";
  }

  @Override
  public InetSocketAddress getRpcAddress() {
    return rpcAddress;
  }

  /**
   * Get the HTTP address.
   *
   * @return HTTP address
   */
  public InetSocketAddress getHttpAddress() {
    return httpAddress;
  }

  /**
   * Get the edits directory.
   *
   * @return edits directory path
   */
  public String getEditsDir() {
    return editsDir;
  }

  @Override
  protected List<String> buildClasspath() throws IOException {
    List<String> classpath = new ArrayList<>();

    File hadoopHomeDir = new File(hadoopHome);

    // 1. Hadoop common JARs
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/common/lib"));

    // 2. HDFS JARs (includes QJournal code)
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs"));
    addJarsFromDir(classpath, new File(hadoopHomeDir, "share/hadoop/hdfs/lib"));

    // 3. Configuration directory
    File confDir = new File(workDir, "conf");
    classpath.add(confDir.getAbsolutePath());

    LOG.debug("Built classpath with {} entries for JournalNode {}",
        classpath.size(), nodeIndex);

    return classpath;
  }

  @Override
  protected List<String> buildCommand(List<String> classpath) {
    List<String> command = new ArrayList<>();

    // 1. Java executable
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());

    // 2. JVM options (JournalNode needs less memory than NameNode)
    command.add("-Xmx512m"); // Heap size
    command.add("-Xms256m");

    // 3. System properties
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-journalnode.log");
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.id.str=jn" + nodeIndex);
    command.add("-Dhadoop.root.logger=INFO,console");
    command.add("-Dhadoop.security.logger=INFO,console");

    // 4. Classpath
    command.add("-cp");
    command.add(String.join(File.pathSeparator, classpath));

    // 5. Main class - JournalNode
    command.add("org.apache.hadoop.hdfs.qjournal.server.JournalNode");

    // 6. Arguments - none for regular startup
    // (configuration is passed via config files, not command line)

    LOG.debug("Built command with {} elements for JournalNode {}",
        command.size(), nodeIndex);

    return command;
  }

  @Override
  protected boolean checkRpcHealth() throws IOException {
    // For JournalNode, we check if RPC port is listening
    // JournalNodes don't register with NameNode, so simpler health check
    if (!isPortListening(rpcAddress)) {
      throw new IOException("JournalNode RPC port not listening: " + rpcAddress);
    }

    // Optionally also check HTTP port
    if (!isPortListening(httpAddress)) {
      LOG.warn("JournalNode HTTP port not listening: {} (non-critical)", httpAddress);
      // Don't fail on HTTP port - RPC is the critical service
    }

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

  @Override
  public void stop() throws IOException {
    LOG.info("Stopping JournalNode {}", nodeIndex);
    super.stop();
  }

  /**
   * Get a URI string for this JournalNode in QJournal format.
   * Used for constructing the full quorum URI.
   *
   * @return URI string like "host:port"
   */
  public String getJournalNodeUri() {
    return rpcAddress.getHostName() + ":" + rpcAddress.getPort();
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
      java.util.Arrays.sort(jars); // Consistent ordering
      for (File jar : jars) {
        classpath.add(jar.getAbsolutePath());
      }
      LOG.trace("Added {} JARs from {}", jars.length, dir);
    }
  }

  @Override
  public String toString() {
    return "JournalNode-" + nodeIndex +
        " [rpc=" + rpcAddress +
        ", http=" + httpAddress +
        ", edits=" + editsDir +
        ", alive=" + isAlive() + "]";
  }
}

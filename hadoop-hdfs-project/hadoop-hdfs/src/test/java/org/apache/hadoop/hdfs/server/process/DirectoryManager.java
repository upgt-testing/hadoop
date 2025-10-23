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

import org.apache.hadoop.fs.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages directory structures for ProcessBasedMiniDFSCluster nodes.
 *
 * <p>This class creates and manages the temporary directory hierarchy required
 * for running HDFS nodes in separate processes. Each node gets its own isolated
 * directory structure containing:
 * <ul>
 *   <li>conf/ - Configuration files (hdfs-site.xml, core-site.xml, etc.)</li>
 *   <li>data/ - Node data directory (NameNode metadata or DataNode blocks)</li>
 *   <li>logs/ - Log files for the node process</li>
 *   <li>pid - Process ID file</li>
 * </ul>
 *
 * <p>Directory structure example:
 * <pre>
 * /tmp/process-minicluster-123456/
 * ├── nn0/
 * │   ├── conf/
 * │   ├── data/
 * │   ├── logs/
 * │   └── pid
 * ├── dn0/
 * │   ├── conf/
 * │   ├── data/
 * │   ├── logs/
 * │   └── pid
 * └── cluster.properties
 * </pre>
 *
 * <p>The manager supports cleanup operations to remove temporary directories
 * after cluster shutdown.
 */
public class DirectoryManager {
  private static final Logger LOG = LoggerFactory.getLogger(DirectoryManager.class);

  private static final String CLUSTER_DIR_PREFIX = "process-minicluster-";
  private static final String NAMENODE_DIR_PREFIX = "nn";
  private static final String DATANODE_DIR_PREFIX = "dn";

  private final File clusterBaseDir;
  private final List<File> nodeDirs;
  private final boolean cleanupOnExit;

  /**
   * Creates a DirectoryManager with a new temporary cluster directory.
   *
   * @param cleanupOnExit if true, registers shutdown hook to clean up directories
   * @throws IOException if unable to create the base directory
   */
  public DirectoryManager(boolean cleanupOnExit) throws IOException {
    this(null, cleanupOnExit);
  }

  /**
   * Creates a DirectoryManager with a specified base directory.
   *
   * @param baseDir the base directory for the cluster, or null to create a temp directory
   * @param cleanupOnExit if true, registers shutdown hook to clean up directories
   * @throws IOException if unable to create the base directory
   */
  public DirectoryManager(File baseDir, boolean cleanupOnExit) throws IOException {
    this.cleanupOnExit = cleanupOnExit;
    this.nodeDirs = new ArrayList<>();

    if (baseDir == null) {
      // Create a unique temporary directory
      String timestamp = String.valueOf(System.currentTimeMillis());
      String dirName = CLUSTER_DIR_PREFIX + timestamp;
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      this.clusterBaseDir = new File(tempDir, dirName);
    } else {
      this.clusterBaseDir = baseDir;
    }

    if (!clusterBaseDir.exists()) {
      if (!clusterBaseDir.mkdirs()) {
        throw new IOException("Failed to create cluster base directory: " + clusterBaseDir);
      }
      LOG.info("Created cluster base directory: {}", clusterBaseDir);
    }

    if (cleanupOnExit) {
      registerShutdownHook();
    }
  }

  /**
   * Creates a directory structure for a NameNode.
   *
   * @param nodeIndex the index of the NameNode (0 for single NN, 0-N for HA)
   * @return NodeDirectory object containing the created directories
   * @throws IOException if unable to create directories
   */
  public NodeDirectory createNameNodeDirectory(int nodeIndex) throws IOException {
    String nodeDirName = NAMENODE_DIR_PREFIX + nodeIndex;
    return createNodeDirectory(nodeDirName);
  }

  /**
   * Creates a directory structure for a DataNode (backward compatibility).
   *
   * @param nodeIndex the index of the DataNode (0 to numDataNodes-1)
   * @return NodeDirectory object containing the created directories
   * @throws IOException if unable to create directories
   */
  public NodeDirectory createDataNodeDirectory(int nodeIndex) throws IOException {
    return createDataNodeDirectory(nodeIndex, 1, null);
  }

  /**
   * Creates a directory structure for a DataNode with storage type configuration.
   *
   * @param nodeIndex the index of the DataNode (0 to numDataNodes-1)
   * @param storagesPerDatanode number of storage locations for this DataNode
   * @param storageTypes array of storage types for each storage location
   * @return NodeDirectory object containing the created directories
   * @throws IOException if unable to create directories
   */
  public NodeDirectory createDataNodeDirectory(int nodeIndex, int storagesPerDatanode,
                                                 StorageType[] storageTypes) throws IOException {
    String nodeDirName = DATANODE_DIR_PREFIX + nodeIndex;
    return createNodeDirectory(nodeDirName, storagesPerDatanode, storageTypes);
  }

  /**
   * Creates a complete directory structure for a node (backward compatibility).
   *
   * @param nodeDirName the name of the node directory (e.g., "nn0", "dn0")
   * @return NodeDirectory object containing the created directories
   * @throws IOException if unable to create directories
   */
  private NodeDirectory createNodeDirectory(String nodeDirName) throws IOException {
    return createNodeDirectory(nodeDirName, 1, null);
  }

  /**
   * Creates a complete directory structure for a node with storage type configuration.
   *
   * @param nodeDirName the name of the node directory (e.g., "nn0", "dn0")
   * @param storagesPerDatanode number of storage locations (only applies to DataNodes)
   * @param storageTypes array of storage types for each storage location
   * @return NodeDirectory object containing the created directories
   * @throws IOException if unable to create directories
   */
  private NodeDirectory createNodeDirectory(String nodeDirName, int storagesPerDatanode,
                                             StorageType[] storageTypes) throws IOException {
    File nodeDir = new File(clusterBaseDir, nodeDirName);

    // Create main node directory
    if (!nodeDir.exists() && !nodeDir.mkdirs()) {
      throw new IOException("Failed to create node directory: " + nodeDir);
    }

    // Create subdirectories
    File confDir = new File(nodeDir, "conf");
    File logsDir = new File(nodeDir, "logs");

    if (!confDir.exists() && !confDir.mkdirs()) {
      throw new IOException("Failed to create conf directory: " + confDir);
    }
    if (!logsDir.exists() && !logsDir.mkdirs()) {
      throw new IOException("Failed to create logs directory: " + logsDir);
    }

    // Create multiple data storage directories for DataNodes
    File dataDir = new File(nodeDir, "data");
    if (!dataDir.exists() && !dataDir.mkdirs()) {
      throw new IOException("Failed to create data directory: " + dataDir);
    }

    List<File> dataStorageDirs = new ArrayList<>();
    for (int i = 0; i < storagesPerDatanode; i++) {
      StorageType type = (storageTypes != null && i < storageTypes.length)
          ? storageTypes[i]
          : StorageType.DEFAULT;

      // Create directory named by storage type: data-DISK-0, data-SSD-1, etc.
      String dirName = "data-" + type.toString() + "-" + i;
      File storageDir = new File(dataDir, dirName);
      if (!storageDir.mkdirs()) {
        throw new IOException("Failed to create storage directory: " + storageDir);
      }
      dataStorageDirs.add(storageDir);
    }

    File pidFile = new File(nodeDir, "pid");

    nodeDirs.add(nodeDir);

    LOG.info("Created node directory structure at: {} with {} storage locations",
        nodeDir, storagesPerDatanode);

    return new NodeDirectory(nodeDir, confDir, dataDir, dataStorageDirs,
        storageTypes, logsDir, pidFile);
  }

  /**
   * Gets the cluster base directory.
   *
   * @return the base directory for the entire cluster
   */
  public File getClusterBaseDir() {
    return clusterBaseDir;
  }

  /**
   * Gets all created node directories.
   *
   * @return list of node directories
   */
  public List<File> getNodeDirs() {
    return new ArrayList<>(nodeDirs);
  }

  /**
   * Creates a cluster properties file in the base directory.
   *
   * @return the created properties file
   * @throws IOException if unable to create the file
   */
  public File createClusterPropertiesFile() throws IOException {
    File propsFile = new File(clusterBaseDir, "cluster.properties");
    if (!propsFile.exists() && !propsFile.createNewFile()) {
      throw new IOException("Failed to create cluster properties file: " + propsFile);
    }
    return propsFile;
  }

  /**
   * Cleans up all directories managed by this DirectoryManager.
   *
   * @throws IOException if cleanup fails
   */
  public void cleanup() throws IOException {
    LOG.info("Cleaning up cluster directory: {}", clusterBaseDir);

    if (clusterBaseDir.exists()) {
      deleteRecursively(clusterBaseDir.toPath());
    }

    nodeDirs.clear();
  }

  /**
   * Recursively deletes a directory and all its contents.
   *
   * @param path the path to delete
   * @throws IOException if deletion fails
   */
  private void deleteRecursively(Path path) throws IOException {
    File file = path.toFile();
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursively(child.toPath());
        }
      }
    }

    if (!file.delete() && file.exists()) {
      // Try again with Files API for better error messages
      try {
        Files.delete(path);
      } catch (IOException e) {
        LOG.warn("Failed to delete: {}", path, e);
        throw e;
      }
    }
  }

  /**
   * Registers a JVM shutdown hook to clean up directories on exit.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Check if cleanup is disabled for debugging
      boolean noCleanup = Boolean.getBoolean("mini.dfs.no.cleanup");
      if (noCleanup) {
        LOG.info("Skipping shutdown hook cleanup due to mini.dfs.no.cleanup=true");
        LOG.info("Cluster directory preserved at: {}", clusterBaseDir);
        return;
      }
      try {
        cleanup();
      } catch (IOException e) {
        LOG.error("Error cleaning up directories on shutdown", e);
      }
    }, "DirectoryManager-Cleanup"));
  }

  /**
   * Container class for node directory paths.
   */
  public static class NodeDirectory {
    private final File nodeBaseDir;
    private final File confDir;
    private final File dataDir;  // Base data directory
    private final List<File> dataStorageDirs;  // Multiple storage directories
    private final StorageType[] storageTypes;
    private final File logsDir;
    private final File pidFile;

    // Backward compatibility constructor
    public NodeDirectory(File nodeBaseDir, File confDir, File dataDir,
                         File logsDir, File pidFile) {
      this.nodeBaseDir = nodeBaseDir;
      this.confDir = confDir;
      this.dataDir = dataDir;
      this.dataStorageDirs = new ArrayList<>();
      this.dataStorageDirs.add(dataDir);  // Single storage location
      this.storageTypes = null;
      this.logsDir = logsDir;
      this.pidFile = pidFile;
    }

    // New constructor with storage types
    public NodeDirectory(File nodeBaseDir, File confDir, File dataDir,
                         List<File> dataStorageDirs, StorageType[] storageTypes,
                         File logsDir, File pidFile) {
      this.nodeBaseDir = nodeBaseDir;
      this.confDir = confDir;
      this.dataDir = dataDir;
      this.dataStorageDirs = new ArrayList<>(dataStorageDirs);
      this.storageTypes = storageTypes;
      this.logsDir = logsDir;
      this.pidFile = pidFile;
    }

    public File getNodeBaseDir() {
      return nodeBaseDir;
    }

    public File getConfDir() {
      return confDir;
    }

    public File getDataDir() {
      return dataDir;
    }

    public List<File> getDataStorageDirs() {
      return new ArrayList<>(dataStorageDirs);
    }

    public StorageType[] getStorageTypes() {
      return storageTypes;
    }

    public File getLogsDir() {
      return logsDir;
    }

    public File getPidFile() {
      return pidFile;
    }

    @Override
    public String toString() {
      return "NodeDirectory{" +
          "nodeBaseDir=" + nodeBaseDir +
          ", confDir=" + confDir +
          ", dataDir=" + dataDir +
          ", logsDir=" + logsDir +
          ", pidFile=" + pidFile +
          '}';
    }
  }
}

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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.hadoop.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the directory structure for ProcessBasedMiniYARNCluster. This class
 * creates and maintains the temporary directories needed for YARN node
 * processes to run in isolation.
 *
 * <p>Creates a hierarchical directory structure with separate directories
 * for each ResourceManager and NodeManager, including conf, data, logs
 * subdirectories for each node.</p>
 *
 * @see ProcessConfigurationGenerator
 */
public class DirectoryManager {

  private static final Logger LOG =
      LoggerFactory.getLogger(DirectoryManager.class);

  /** Prefix for cluster root directory */
  private static final String CLUSTER_DIR_PREFIX = "process-miniyarn-";

  /** Name of configuration subdirectory */
  private static final String CONF_DIR = "conf";

  /** Name of data subdirectory */
  private static final String DATA_DIR = "data";

  /** Name of logs subdirectory */
  private static final String LOGS_DIR = "logs";

  /** Name of local-dirs subdirectory for NodeManager */
  private static final String LOCAL_DIRS = "local-dirs";

  /** Name of log-dirs subdirectory for NodeManager */
  private static final String LOG_DIRS = "log-dirs";

  /** Name of pid file */
  private static final String PID_FILE = "pid";

  /** Root directory for the cluster */
  private File clusterRoot;

  /** List of all created directories (for cleanup) */
  private final List<File> createdDirectories;

  /** Whether to delete directories on cleanup */
  private boolean deleteOnCleanup = true;

  /**
   * Constructs a new DirectoryManager.
   */
  public DirectoryManager() {
    this.createdDirectories = new ArrayList<>();
  }

  /**
   * Creates the cluster root directory in the system temp directory.
   * The directory name includes a timestamp to avoid conflicts.
   *
   * @return The created cluster root directory
   * @throws IOException if directory creation fails
   */
  public File createClusterRoot() throws IOException {
    return createClusterRoot(null);
  }

  /**
   * Creates the cluster root directory at a specific location or in
   * the system temp directory if null.
   *
   * @param baseDir Base directory (null for system temp)
   * @return The created cluster root directory
   * @throws IOException if directory creation fails
   */
  public File createClusterRoot(File baseDir) throws IOException {
    if (clusterRoot != null) {
      LOG.warn("Cluster root already exists: {}", clusterRoot);
      return clusterRoot;
    }

    if (baseDir == null) {
      baseDir = new File(System.getProperty("java.io.tmpdir"));
    }

    // Create unique directory name with timestamp
    String dirName = CLUSTER_DIR_PREFIX + Time.monotonicNow();
    clusterRoot = new File(baseDir, dirName);

    if (!clusterRoot.mkdirs()) {
      throw new IOException(
          "Failed to create cluster root directory: " + clusterRoot);
    }

    createdDirectories.add(clusterRoot);
    LOG.info("Created cluster root directory: {}", clusterRoot);

    return clusterRoot;
  }

  /**
   * Creates a ResourceManager directory.
   *
   * @param rmIndex Index of the ResourceManager (0, 1, 2, ...)
   * @return The created RM directory
   * @throws IOException if directory creation fails
   */
  public File createResourceManagerDir(int rmIndex) throws IOException {
    ensureClusterRoot();

    File rmDir = new File(clusterRoot, "rm" + rmIndex);
    createNodeDirectory(rmDir);

    // Create RM-specific subdirectories
    createDirectory(new File(rmDir, CONF_DIR));
    createDirectory(new File(rmDir, DATA_DIR));
    createDirectory(new File(rmDir, LOGS_DIR));

    // Create RM state store directory
    File stateStoreDir = new File(rmDir, DATA_DIR + "/rm-state-store");
    createDirectory(stateStoreDir);

    LOG.info("Created ResourceManager directory: {}", rmDir);
    return rmDir;
  }

  /**
   * Creates a NodeManager directory.
   *
   * @param nmIndex Index of the NodeManager (0, 1, 2, ...)
   * @return The created NM directory
   * @throws IOException if directory creation fails
   */
  public File createNodeManagerDir(int nmIndex) throws IOException {
    ensureClusterRoot();

    File nmDir = new File(clusterRoot, "nm" + nmIndex);
    createNodeDirectory(nmDir);

    // Create NM-specific subdirectories
    createDirectory(new File(nmDir, CONF_DIR));
    createDirectory(new File(nmDir, LOGS_DIR));

    // Create NM local directories
    File localDirs = new File(nmDir, LOCAL_DIRS);
    createDirectory(localDirs);
    createDirectory(new File(localDirs, "nm-local-dir"));
    createDirectory(new File(localDirs, "usercache"));

    // Create NM log directories
    File logDirs = new File(nmDir, LOG_DIRS);
    createDirectory(logDirs);

    LOG.info("Created NodeManager directory: {}", nmDir);
    return nmDir;
  }

  /**
   * Creates a basic node directory structure.
   *
   * @param nodeDir Node directory to create
   * @throws IOException if creation fails
   */
  private void createNodeDirectory(File nodeDir) throws IOException {
    if (!nodeDir.exists()) {
      if (!nodeDir.mkdirs()) {
        throw new IOException("Failed to create node directory: " + nodeDir);
      }
      createdDirectories.add(nodeDir);
    }
  }

  /**
   * Creates a directory and tracks it for cleanup.
   *
   * @param dir Directory to create
   * @throws IOException if creation fails
   */
  private void createDirectory(File dir) throws IOException {
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new IOException("Failed to create directory: " + dir);
      }
      createdDirectories.add(dir);
    }
  }

  /**
   * Gets the configuration directory for a node.
   *
   * @param nodeDir Node directory (rm0, nm0, etc.)
   * @return Configuration directory
   */
  public File getConfDir(File nodeDir) {
    return new File(nodeDir, CONF_DIR);
  }

  /**
   * Gets the data directory for a ResourceManager.
   *
   * @param rmDir RM directory
   * @return Data directory
   */
  public File getDataDir(File rmDir) {
    return new File(rmDir, DATA_DIR);
  }

  /**
   * Gets the logs directory for a node.
   *
   * @param nodeDir Node directory
   * @return Logs directory
   */
  public File getLogsDir(File nodeDir) {
    return new File(nodeDir, LOGS_DIR);
  }

  /**
   * Gets the local-dirs directory for a NodeManager.
   *
   * @param nmDir NM directory
   * @return Local dirs directory
   */
  public File getNMLocalDirs(File nmDir) {
    return new File(nmDir, LOCAL_DIRS);
  }

  /**
   * Gets the log-dirs directory for a NodeManager.
   *
   * @param nmDir NM directory
   * @return Log dirs directory
   */
  public File getNMLogDirs(File nmDir) {
    return new File(nmDir, LOG_DIRS);
  }

  /**
   * Gets the PID file for a node.
   *
   * @param nodeDir Node directory
   * @return PID file
   */
  public File getPidFile(File nodeDir) {
    return new File(nodeDir, PID_FILE);
  }

  /**
   * Writes a PID to the PID file.
   *
   * @param nodeDir Node directory
   * @param pid Process ID
   * @throws IOException if write fails
   */
  public void writePid(File nodeDir, long pid) throws IOException {
    File pidFile = getPidFile(nodeDir);
    try (FileWriter writer = new FileWriter(pidFile)) {
      writer.write(String.valueOf(pid));
    }
    LOG.debug("Wrote PID {} to {}", pid, pidFile);
  }

  /**
   * Gets the cluster root directory.
   *
   * @return Cluster root directory
   */
  public File getClusterRoot() {
    return clusterRoot;
  }

  /**
   * Sets whether to delete directories on cleanup.
   *
   * @param deleteOnCleanup true to delete, false to keep for debugging
   */
  public void setDeleteOnCleanup(boolean deleteOnCleanup) {
    this.deleteOnCleanup = deleteOnCleanup;
  }

  /**
   * Ensures the cluster root has been created.
   *
   * @throws IOException if cluster root doesn't exist
   */
  private void ensureClusterRoot() throws IOException {
    if (clusterRoot == null) {
      throw new IOException(
          "Cluster root not created. Call createClusterRoot() first.");
    }
  }

  /**
   * Cleans up all created directories. If deleteOnCleanup is true,
   * deletes all directories; otherwise, just logs their locations.
   *
   * @throws IOException if cleanup fails
   */
  public void cleanup() throws IOException {
    if (clusterRoot == null) {
      LOG.debug("No cluster root to clean up");
      return;
    }

    if (deleteOnCleanup) {
      LOG.info("Cleaning up cluster directory: {}", clusterRoot);

      try {
        // Delete the entire cluster root directory tree
        Files.walk(clusterRoot.toPath())
            .sorted(Comparator.reverseOrder())
            .map(java.nio.file.Path::toFile)
            .forEach(File::delete);

        LOG.info("Successfully deleted cluster directory: {}", clusterRoot);
      } catch (IOException e) {
        LOG.error("Failed to delete cluster directory: {}", clusterRoot, e);
        throw new IOException(
            "Failed to cleanup cluster directory: " + clusterRoot, e);
      }
    } else {
      LOG.info("Keeping cluster directory for debugging: {}", clusterRoot);
    }

    clusterRoot = null;
    createdDirectories.clear();
  }

  /**
   * Cleans up only a specific node directory.
   *
   * @param nodeDir Node directory to clean up
   * @throws IOException if cleanup fails
   */
  public void cleanupNode(File nodeDir) throws IOException {
    if (nodeDir == null || !nodeDir.exists()) {
      return;
    }

    if (deleteOnCleanup) {
      LOG.info("Cleaning up node directory: {}", nodeDir);

      try {
        Files.walk(nodeDir.toPath())
            .sorted(Comparator.reverseOrder())
            .map(java.nio.file.Path::toFile)
            .forEach(File::delete);

        createdDirectories.remove(nodeDir);
        LOG.info("Successfully deleted node directory: {}", nodeDir);
      } catch (IOException e) {
        LOG.error("Failed to delete node directory: {}", nodeDir, e);
        throw new IOException(
            "Failed to cleanup node directory: " + nodeDir, e);
      }
    }
  }

  @Override
  public String toString() {
    return "DirectoryManager{" +
        "clusterRoot=" + clusterRoot +
        ", directories=" + createdDirectories.size() +
        ", deleteOnCleanup=" + deleteOnCleanup +
        '}';
  }
}

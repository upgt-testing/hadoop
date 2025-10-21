/*
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
package org.apache.hadoop.hdfs.server.process.upgrade;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.VersionConfigAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper utility for testing Hadoop version upgrades and mixed-version clusters.
 *
 * This class provides convenient methods for:
 * - Performing rolling upgrades of DataNodes
 * - Verifying data integrity across upgrades
 * - Testing version compatibility
 * - Writing and reading test data
 */
public class UpgradeTestHelper {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeTestHelper.class);

  private static final Random RANDOM = new Random();
  private static final int DEFAULT_FILE_SIZE = 1024 * 1024; // 1MB
  private static final String TEST_DIR = "/upgrade-test";

  /**
   * Performs a rolling upgrade of all DataNodes in the cluster.
   *
   * For each DataNode:
   * 1. Shutdown the DataNode
   * 2. Change its Hadoop version
   * 3. Restart the DataNode
   * 4. Wait for cluster to be healthy
   * 5. Verify data can still be read
   *
   * @param cluster the cluster to upgrade
   * @param targetVersion the target Hadoop version path
   * @param testFiles list of test files to verify after each node upgrade
   * @throws Exception if upgrade fails
   */
  public static void performRollingDataNodeUpgrade(
      ProcessBasedMiniDFSCluster cluster,
      String targetVersion,
      List<Path> testFiles) throws Exception {

    int numDataNodes = cluster.getNumDataNodes();
    LOG.info("Starting rolling upgrade of {} DataNodes to version {}",
        numDataNodes, targetVersion);

    for (int i = 0; i < numDataNodes; i++) {
      LOG.info("Upgrading DataNode {} of {} to version {}", i + 1, numDataNodes, targetVersion);

      // Shutdown DataNode
      LOG.debug("Shutting down DataNode {}", i);
      cluster.shutdownDataNode(i);

      // Change version
      LOG.debug("Changing DataNode {} to version {}", i, targetVersion);
      cluster.changeDataNodeVersion(i, targetVersion);

      // Restart DataNode
      LOG.debug("Starting DataNode {} with new version", i);
      cluster.startDataNode(i);

      // Wait for cluster to be healthy
      LOG.debug("Waiting for cluster to be healthy after DataNode {} upgrade", i);
      cluster.waitClusterUp();

      // Verify data integrity
      LOG.debug("Verifying data integrity after DataNode {} upgrade", i);
      verifyTestData(cluster.getFileSystem(), testFiles);

      LOG.info("Successfully upgraded DataNode {} to version {}", i, targetVersion);
    }

    LOG.info("Rolling upgrade of all DataNodes completed successfully");
  }

  /**
   * Performs a rolling upgrade of DataNodes one at a time with custom verification.
   *
   * @param cluster the cluster to upgrade
   * @param targetVersion the target Hadoop version path
   * @param verifier custom verification logic to run after each node upgrade
   * @throws Exception if upgrade fails
   */
  public static void performRollingDataNodeUpgrade(
      ProcessBasedMiniDFSCluster cluster,
      String targetVersion,
      UpgradeVerifier verifier) throws Exception {

    int numDataNodes = cluster.getNumDataNodes();
    LOG.info("Starting rolling upgrade of {} DataNodes to version {}",
        numDataNodes, targetVersion);

    for (int i = 0; i < numDataNodes; i++) {
      LOG.info("Upgrading DataNode {} of {}", i + 1, numDataNodes);

      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, targetVersion);
      cluster.startDataNode(i);
      cluster.waitClusterUp();

      // Run custom verification
      verifier.verify(cluster, i);

      LOG.info("Successfully upgraded DataNode {}", i);
    }

    LOG.info("Rolling upgrade completed successfully");
  }

  /**
   * Verifies version compatibility between two Hadoop versions.
   *
   * @param version1 first Hadoop version string
   * @param version2 second Hadoop version string
   * @return true if versions are compatible, false otherwise
   */
  public static boolean verifyVersionCompatibility(String version1, String version2) {
    LOG.info("Checking compatibility between versions {} and {}", version1, version2);

    try {
      VersionConfigAdapter adapter1 = new VersionConfigAdapter(version1);
      VersionConfigAdapter adapter2 = new VersionConfigAdapter(version2);

      boolean compatible = adapter1.isCompatibleWith(adapter2);

      if (compatible) {
        LOG.info("Versions {} and {} are compatible", version1, version2);
      } else {
        LOG.warn("Versions {} and {} are NOT compatible", version1, version2);
      }

      return compatible;
    } catch (Exception e) {
      LOG.error("Error checking version compatibility", e);
      return false;
    }
  }

  /**
   * Writes test data to HDFS for verification purposes.
   *
   * Creates multiple files with random data in a test directory.
   *
   * @param fs the FileSystem to write to
   * @param numFiles number of test files to create
   * @param fileSize size of each file in bytes
   * @return list of paths to created files
   * @throws IOException if write fails
   */
  public static List<Path> writeTestData(FileSystem fs, int numFiles, int fileSize)
      throws IOException {

    List<Path> testFiles = new ArrayList<>();

    // Ensure test directory exists
    Path testDirPath = new Path(TEST_DIR);
    if (!fs.exists(testDirPath)) {
      fs.mkdirs(testDirPath);
      LOG.info("Created test directory: {}", testDirPath);
    }

    // Create test files
    for (int i = 0; i < numFiles; i++) {
      Path filePath = new Path(testDirPath, "testfile-" + i + ".dat");

      // Write random data
      byte[] data = new byte[fileSize];
      RANDOM.nextBytes(data);

      FSDataOutputStream out = fs.create(filePath, true);
      out.write(data);
      out.close();

      testFiles.add(filePath);
      LOG.debug("Created test file: {} ({} bytes)", filePath, fileSize);
    }

    LOG.info("Created {} test files in {}", numFiles, testDirPath);
    return testFiles;
  }

  /**
   * Writes test data with default file size (1MB).
   *
   * @param fs the FileSystem to write to
   * @param numFiles number of test files to create
   * @return list of paths to created files
   * @throws IOException if write fails
   */
  public static List<Path> writeTestData(FileSystem fs, int numFiles) throws IOException {
    return writeTestData(fs, numFiles, DEFAULT_FILE_SIZE);
  }

  /**
   * Verifies that test data can be read and matches expected content.
   *
   * @param fs the FileSystem to read from
   * @param testFiles list of test files to verify
   * @throws IOException if verification fails
   */
  public static void verifyTestData(FileSystem fs, List<Path> testFiles) throws IOException {
    LOG.debug("Verifying {} test files", testFiles.size());

    for (Path filePath : testFiles) {
      if (!fs.exists(filePath)) {
        throw new IOException("Test file does not exist: " + filePath);
      }

      // Read file to verify it's accessible
      FSDataInputStream in = fs.open(filePath);
      long bytesRead = 0;
      byte[] buffer = new byte[4096];
      int len;
      while ((len = in.read(buffer)) > 0) {
        bytesRead += len;
      }
      in.close();

      LOG.debug("Verified test file: {} ({} bytes)", filePath, bytesRead);
    }

    LOG.debug("Successfully verified all {} test files", testFiles.size());
  }

  /**
   * Verifies that the cluster can perform basic file operations.
   *
   * @param fs the FileSystem to test
   * @throws IOException if operations fail
   */
  public static void assertCanReadWriteData(FileSystem fs) throws IOException {
    LOG.debug("Testing basic file operations");

    Path testFile = new Path("/tmp/cluster-health-check-" + System.currentTimeMillis() + ".dat");

    try {
      // Test write
      String testData = "Cluster health check data";
      FSDataOutputStream out = fs.create(testFile, true);
      out.writeUTF(testData);
      out.close();

      // Test read
      FSDataInputStream in = fs.open(testFile);
      String readData = in.readUTF();
      in.close();

      if (!testData.equals(readData)) {
        throw new IOException("Data mismatch: expected '" + testData + "', got '" + readData + "'");
      }

      // Test delete
      fs.delete(testFile, false);

      LOG.debug("Basic file operations verified successfully");
    } catch (IOException e) {
      LOG.error("Basic file operations failed", e);
      throw e;
    } finally {
      // Cleanup
      try {
        if (fs.exists(testFile)) {
          fs.delete(testFile, false);
        }
      } catch (IOException e) {
        LOG.warn("Failed to cleanup test file: {}", testFile, e);
      }
    }
  }

  /**
   * Cleans up test data created by writeTestData().
   *
   * @param fs the FileSystem to clean
   * @param testFiles list of test files to delete
   * @throws IOException if cleanup fails
   */
  public static void cleanupTestData(FileSystem fs, List<Path> testFiles) throws IOException {
    LOG.info("Cleaning up {} test files", testFiles.size());

    for (Path filePath : testFiles) {
      if (fs.exists(filePath)) {
        fs.delete(filePath, false);
        LOG.debug("Deleted test file: {}", filePath);
      }
    }

    // Remove test directory if empty
    Path testDirPath = new Path(TEST_DIR);
    if (fs.exists(testDirPath)) {
      try {
        fs.delete(testDirPath, false);
        LOG.info("Deleted test directory: {}", testDirPath);
      } catch (IOException e) {
        LOG.debug("Test directory not empty or failed to delete: {}", testDirPath);
      }
    }
  }

  /**
   * Waits for the cluster to stabilize after a configuration change.
   *
   * @param cluster the cluster to wait for
   * @param timeoutMs maximum time to wait in milliseconds
   * @throws Exception if cluster doesn't stabilize within timeout
   */
  public static void waitForClusterStable(ProcessBasedMiniDFSCluster cluster, long timeoutMs)
      throws Exception {
    LOG.debug("Waiting for cluster to stabilize (timeout: {}ms)", timeoutMs);

    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        cluster.waitClusterUp();
        assertCanReadWriteData(cluster.getFileSystem());
        LOG.debug("Cluster is stable");
        return;
      } catch (Exception e) {
        LOG.debug("Cluster not yet stable, retrying...", e);
        Thread.sleep(1000);
      }
    }

    throw new Exception("Cluster did not stabilize within " + timeoutMs + "ms");
  }

  /**
   * Gets information about a Hadoop version.
   *
   * @param versionPath path to Hadoop distribution
   * @return VersionInfo object with version details
   */
  public static VersionInfo getVersionInfo(String versionPath) {
    // Extract version from path (e.g., /opt/hadoop-3.3.5 -> 3.3.5)
    String version = versionPath;
    if (versionPath.contains("hadoop-")) {
      int idx = versionPath.lastIndexOf("hadoop-");
      version = versionPath.substring(idx + 7);
    }

    return new VersionInfo(version, versionPath);
  }

  /**
   * Information about a Hadoop version.
   */
  public static class VersionInfo {
    private final String version;
    private final String path;
    private final VersionConfigAdapter adapter;

    public VersionInfo(String version, String path) {
      this.version = version;
      this.path = path;
      this.adapter = new VersionConfigAdapter(version);
    }

    public String getVersion() {
      return version;
    }

    public String getPath() {
      return path;
    }

    public VersionConfigAdapter getAdapter() {
      return adapter;
    }

    public int getMajorVersion() {
      return adapter.getMajorVersion();
    }

    public int getMinorVersion() {
      return adapter.getMinorVersion();
    }

    public boolean isCompatibleWith(VersionInfo other) {
      return adapter.isCompatibleWith(other.adapter);
    }

    @Override
    public String toString() {
      return "VersionInfo{version='" + version + "', path='" + path + "'}";
    }
  }

  /**
   * Interface for custom upgrade verification logic.
   */
  public interface UpgradeVerifier {
    /**
     * Verify the cluster state after upgrading a node.
     *
     * @param cluster the cluster being upgraded
     * @param nodeIndex the index of the node that was just upgraded
     * @throws Exception if verification fails
     */
    void verify(ProcessBasedMiniDFSCluster cluster, int nodeIndex) throws Exception;
  }
}

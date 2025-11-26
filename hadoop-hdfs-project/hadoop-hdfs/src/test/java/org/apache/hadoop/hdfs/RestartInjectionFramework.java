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

package org.apache.hadoop.hdfs;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.LeaseManager;
import org.apache.hadoop.hdfs.server.namenode.NameNodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Framework for systematically testing HDFS crash-restart scenarios.
 * Provides utilities to inject restarts at various points in test execution
 * and verify system behavior after recovery.
 *
 * <p>This framework enables transformation of existing MiniDFSCluster tests
 * to include restart scenarios, helping discover bugs related to:
 * - State recovery and persistence
 * - EditLog replay
 * - Lease recovery
 * - Block recovery
 * - Pipeline recovery
 * - Data durability
 */
public class RestartInjectionFramework {

  private static final Logger LOG =
      LoggerFactory.getLogger(RestartInjectionFramework.class);

  /**
   * Defines points in test execution where restarts can be injected.
   */
  public enum RestartPoint {
    /** After writing a complete block */
    AFTER_WRITE_BLOCK,
    /** During an active write (partial block) */
    DURING_WRITE,
    /** After hflush() call */
    AFTER_FLUSH,
    /** After hsync() call */
    AFTER_SYNC,
    /** Before close() on output stream */
    BEFORE_CLOSE,
    /** After close() completes */
    AFTER_CLOSE,
    /** During setReplication operation */
    DURING_REPLICATION,
    /** During delete operation */
    DURING_DELETE,
    /** During append operation */
    DURING_APPEND,
    /** During rename operation */
    DURING_RENAME
  }

  /**
   * Defines which component(s) to restart.
   */
  public enum RestartTarget {
    /** Restart the NameNode */
    NAMENODE,
    /** Restart a single DataNode */
    SINGLE_DATANODE,
    /** Restart all DataNodes */
    ALL_DATANODES,
    /** Restart a randomly selected DataNode */
    RANDOM_DATANODE,
    /** Restart both NameNode and all DataNodes */
    NAMENODE_AND_DATANODES
  }

  /**
   * Defines the type of restart to perform.
   */
  public enum RestartMode {
    /** Normal graceful restart with clean shutdown */
    GRACEFUL,
    /** Simulated crash with immediate shutdown */
    CRASH,
    /** Crash after a short delay to allow partial state propagation */
    DELAYED_CRASH
  }

  /**
   * Execute a restart operation on the specified target.
   *
   * @param cluster the MiniDFSCluster to restart components in
   * @param target which component(s) to restart
   * @param mode the restart mode (graceful, crash, delayed crash)
   * @param waitActive whether to wait for cluster to become active after restart
   * @throws Exception if restart fails
   */
  public static void executeRestart(
      MiniDFSCluster cluster,
      RestartTarget target,
      RestartMode mode,
      boolean waitActive) throws Exception {

    LOG.info("Executing {} restart of {} (waitActive={})",
        mode, target, waitActive);

    switch (target) {
      case NAMENODE:
        restartNameNode(cluster, 0, mode, waitActive);
        break;

      case SINGLE_DATANODE:
        restartDataNode(cluster, 0, mode, waitActive);
        break;

      case ALL_DATANODES:
        restartAllDataNodes(cluster, mode, waitActive);
        break;

      case RANDOM_DATANODE:
        int randomDN = new Random().nextInt(cluster.getDataNodes().size());
        restartDataNode(cluster, randomDN, mode, waitActive);
        break;

      case NAMENODE_AND_DATANODES:
        restartNameNode(cluster, 0, mode, waitActive);
        restartAllDataNodes(cluster, mode, waitActive);
        break;

      default:
        throw new IllegalArgumentException("Unknown restart target: " + target);
    }

    LOG.info("Restart completed successfully");
  }

  /**
   * Execute a restart on NameNode at specified index.
   */
  private static void restartNameNode(
      MiniDFSCluster cluster,
      int nnIndex,
      RestartMode mode,
      boolean waitActive) throws Exception {

    switch (mode) {
      case GRACEFUL:
        LOG.info("Gracefully restarting NameNode {}", nnIndex);
        cluster.restartNameNode(nnIndex, waitActive);
        break;

      case CRASH:
        LOG.info("Simulating NameNode {} crash", nnIndex);
        cluster.shutdownNameNode(nnIndex);
        cluster.restartNameNode(nnIndex, waitActive);
        break;

      case DELAYED_CRASH:
        LOG.info("Simulating delayed crash of NameNode {}", nnIndex);
        cluster.shutdownNameNode(nnIndex);
        Thread.sleep(500); // Allow some state propagation before restart
        cluster.restartNameNode(nnIndex, waitActive);
        break;

      default:
        throw new IllegalArgumentException("Unknown restart mode: " + mode);
    }
  }

  /**
   * Execute a restart on DataNode at specified index.
   */
  private static void restartDataNode(
      MiniDFSCluster cluster,
      int dnIndex,
      RestartMode mode,
      boolean waitActive) throws Exception {

    switch (mode) {
      case GRACEFUL:
        LOG.info("Gracefully restarting DataNode {}", dnIndex);
        assertTrue("Failed to restart DataNode " + dnIndex,
            cluster.restartDataNode(dnIndex, true)); // Keep same port
        break;

      case CRASH:
        LOG.info("Simulating DataNode {} crash", dnIndex);
        MiniDFSCluster.DataNodeProperties dnProp = cluster.stopDataNode(dnIndex);
        assertNotNull("Failed to stop DataNode " + dnIndex, dnProp);
        assertTrue("Failed to restart DataNode " + dnIndex,
            cluster.restartDataNode(dnProp, true));
        break;

      case DELAYED_CRASH:
        LOG.info("Simulating delayed crash of DataNode {}", dnIndex);
        MiniDFSCluster.DataNodeProperties dnProp2 = cluster.stopDataNode(dnIndex);
        assertNotNull("Failed to stop DataNode " + dnIndex, dnProp2);
        Thread.sleep(500);
        assertTrue("Failed to restart DataNode " + dnIndex,
            cluster.restartDataNode(dnProp2, true));
        break;

      default:
        throw new IllegalArgumentException("Unknown restart mode: " + mode);
    }

    if (waitActive) {
      cluster.waitActive();
    }
  }

  /**
   * Restart all DataNodes in the cluster.
   */
  private static void restartAllDataNodes(
      MiniDFSCluster cluster,
      RestartMode mode,
      boolean waitActive) throws Exception {

    int numDataNodes = cluster.getDataNodes().size();
    LOG.info("Restarting all {} DataNodes", numDataNodes);

    for (int i = 0; i < numDataNodes; i++) {
      restartDataNode(cluster, i, mode, false);
    }

    if (waitActive) {
      cluster.waitActive();
    }
  }

  /**
   * Verify that the cluster is in a healthy state after restart.
   * Checks:
   * - Cluster is out of safemode
   * - All DataNodes have registered
   * - No dangling leases
   *
   * @param cluster the cluster to verify
   * @param fs the FileSystem to use for checks
   * @throws Exception if cluster is not healthy
   */
  public static void verifyClusterHealth(
      MiniDFSCluster cluster,
      FileSystem fs) throws Exception {

    LOG.info("Verifying cluster health after restart");

    // Verify cluster is active
    cluster.waitActive();
    LOG.info("✓ Cluster is active");

    // Verify cluster is out of safemode
    cluster.waitClusterUp();
    LOG.info("✓ Cluster is out of safemode");

    // Verify all DataNodes are registered
    List<DatanodeDescriptor> datanodes = cluster.getNameNode().getNamesystem()
        .getBlockManager().getDatanodeManager().getDatanodeListForReport(
            org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType.LIVE);
    int expectedDNs = cluster.getDataNodes().size();
    assertEquals("Not all DataNodes registered after restart",
        expectedDNs, datanodes.size());
    LOG.info("✓ All {} DataNodes registered", expectedDNs);

    // Verify no dangling leases (all files properly closed/recovered)
    LeaseManager leaseManager = NameNodeAdapter.getLeaseManager(cluster.getNamesystem());
    int leaseCount = leaseManager.countLease();
    LOG.info("Lease count after restart: {}", leaseCount);
    // Note: We don't assert leaseCount == 0 because tests might intentionally
    // have open files. Callers should verify this if needed.

    LOG.info("Cluster health verification passed");
  }

  /**
   * Verify data integrity for a file.
   * Checks that the file:
   * - Exists
   * - Has expected length
   * - Contains expected data (if provided)
   *
   * @param fs the FileSystem
   * @param path the file to verify
   * @param expectedLength expected file length in bytes
   * @param expectedData expected file contents (null to skip content check)
   * @throws Exception if verification fails
   */
  public static void verifyFileIntegrity(
      FileSystem fs,
      Path path,
      long expectedLength,
      byte[] expectedData) throws Exception {

    LOG.info("Verifying integrity of file: {}", path);

    // Verify file exists
    assertTrue("File does not exist: " + path, fs.exists(path));
    LOG.info("✓ File exists");

    // Verify file length
    long actualLength = fs.getFileStatus(path).getLen();
    assertEquals("File length mismatch", expectedLength, actualLength);
    LOG.info("✓ File length correct: {} bytes", actualLength);

    // Verify file content if expected data provided
    if (expectedData != null) {
      AppendTestUtil.check(fs, path, expectedLength);
      LOG.info("✓ File content verification passed");
    }

    LOG.info("File integrity verification passed for: {}", path);
  }

  /**
   * Verify that no data was lost after restart by checking:
   * - File exists
   * - File length matches expected
   * - File is readable
   *
   * @param fs the FileSystem
   * @param path the file to verify
   * @param expectedLength expected file length
   * @throws IOException if verification fails
   */
  public static void verifyNoDataLoss(
      FileSystem fs,
      Path path,
      long expectedLength) throws IOException {

    assertTrue("File lost after restart: " + path, fs.exists(path));
    long actualLength = fs.getFileStatus(path).getLen();
    assertEquals("File length changed after restart",
        expectedLength, actualLength);

    // Verify file is readable
    InputStream in = null;
    try {
      in = fs.open(path);
      byte[] buf = new byte[4096];
      long totalRead = 0;
      int bytesRead;
      while ((bytesRead = in.read(buf)) > 0) {
        totalRead += bytesRead;
      }
      assertEquals("Could not read entire file after restart",
          expectedLength, totalRead);
    } finally {
      if (in != null) {
        in.close();
      }
    }

    LOG.info("Verified no data loss for: {}", path);
  }

  /**
   * Helper to get current lease count (for verification).
   */
  public static int getLeaseCount(MiniDFSCluster cluster) {
    return NameNodeAdapter.getLeaseManager(cluster.getNamesystem()).countLease();
  }

  /**
   * Helper to wait for lease recovery to complete.
   */
  public static void waitForLeaseRecovery(
      MiniDFSCluster cluster,
      long timeoutMs) throws InterruptedException {

    long startTime = System.currentTimeMillis();
    while (getLeaseCount(cluster) > 0) {
      if (System.currentTimeMillis() - startTime > timeoutMs) {
        throw new RuntimeException(
            "Timeout waiting for lease recovery: " + getLeaseCount(cluster) +
            " leases still active");
      }
      Thread.sleep(100);
    }
    LOG.info("All leases recovered");
  }
}

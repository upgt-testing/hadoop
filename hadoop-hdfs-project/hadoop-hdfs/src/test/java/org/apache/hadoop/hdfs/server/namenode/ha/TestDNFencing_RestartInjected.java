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
package org.apache.hadoop.hdfs.server.namenode.ha;

import static org.junit.Assert.assertEquals;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerTestUtil;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;

/**
 * Restart-injection variant of TestDNFencing.
 * Tests DN fencing operations survive datanode restarts.
 *
 * Original tests with hflush:
 * - testBlockReportsWhileFileBeingWritten
 * - testQueueingWithAppend
 * - testRBWReportArrivesAfterEdits
 *
 * Note: This is an HA test, so we only inject DN restarts (not NN restarts)
 * since the failover behavior is part of the test logic.
 *
 * Generated variants per test:
 * - AfterHflush x 2 DN targets x 2 RestartModes = 4 variants per test
 * Total: 12 variants (3 tests x 4 variants)
 */
public class TestDNFencing_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDNFencing_RestartInjected.class);
  private static final String TEST_FILE = "/testStandbyIsHot";
  private static final Path TEST_FILE_PATH = new Path(TEST_FILE);
  private static final int SMALL_BLOCK = 1024;

  /**
   * Helper to verify cluster health after restart (DN only).
   */
  private void verifyClusterHealth(MiniDFSCluster cluster) throws Exception {
    // Wait for datanodes to become live
    Thread.sleep(3000);
    cluster.triggerHeartbeats();
    cluster.triggerBlockReports();
  }

  /**
   * Core test logic for testBlockReportsWhileFileBeingWritten with restart injection.
   */
  private void testBlockReportsWhileFileBeingWrittenWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, SMALL_BLOCK);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 600);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_MAX_STREAMS_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(3)
        .build();
    cluster.waitActive();
    cluster.transitionToActive(0);
    cluster.triggerBlockReports();

    NameNode nn1 = cluster.getNameNode(0);
    NameNode nn2 = cluster.getNameNode(1);
    FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);

    try {
      FSDataOutputStream out = fs.create(TEST_FILE_PATH);
      try {
        AppendTestUtil.write(out, 0, 10);
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster);
        LOG.info("=== RESTART COMPLETE ===");

        // Block report will include the RBW replica, but will be
        // queued on the StandbyNode.
        cluster.triggerBlockReports();
      } finally {
        IOUtils.closeStream(out);
      }

      cluster.transitionToStandby(0);
      cluster.transitionToActive(1);

      // Verify that no replicas are marked corrupt, and that the
      // file is readable from the failed-over standby.
      BlockManagerTestUtil.updateState(nn1.getNamesystem().getBlockManager());
      BlockManagerTestUtil.updateState(nn2.getNamesystem().getBlockManager());
      assertEquals(0, nn1.getNamesystem().getCorruptReplicaBlocks());
      assertEquals(0, nn2.getNamesystem().getCorruptReplicaBlocks());

      DFSTestUtil.readFile(fs, TEST_FILE_PATH);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testQueueingWithAppend with restart injection.
   */
  private void testQueueingWithAppendWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, SMALL_BLOCK);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 600);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_MAX_STREAMS_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(3)
        .build();
    cluster.waitActive();
    cluster.transitionToActive(0);
    cluster.triggerBlockReports();

    NameNode nn1 = cluster.getNameNode(0);
    NameNode nn2 = cluster.getNameNode(1);
    FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);
    int numDN = cluster.getDataNodes().size();

    try {
      int numQueued = 0;

      // case 1: create file and call hflush after write
      FSDataOutputStream out = fs.create(TEST_FILE_PATH);
      try {
        AppendTestUtil.write(out, 0, 10);
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster);
        LOG.info("=== RESTART COMPLETE ===");

        cluster.triggerBlockReports();
        numQueued += numDN; // RBW messages
        numQueued += numDN; // RBW messages
      } finally {
        IOUtils.closeStream(out);
        numQueued += numDN; // blockReceived messages
      }

      cluster.triggerBlockReports();
      numQueued += numDN;

      // case 2: append to file and call hflush after write
      try {
        out = fs.append(TEST_FILE_PATH);
        AppendTestUtil.write(out, 10, 10);
        out.hflush();
        cluster.triggerBlockReports();
        numQueued += numDN * 2;
      } finally {
        IOUtils.closeStream(out);
        cluster.triggerHeartbeats();
        numQueued += numDN;
      }

      // case 3: similar to case 2, except no hflush is called.
      try {
        out = fs.append(TEST_FILE_PATH);
        AppendTestUtil.write(out, 20, 10);
      } finally {
        IOUtils.closeStream(out);
        numQueued += numDN;
      }

      cluster.triggerBlockReports();
      numQueued += numDN;

      cluster.transitionToStandby(0);
      cluster.transitionToActive(1);

      // Verify that no replicas are marked corrupt
      BlockManagerTestUtil.updateState(nn1.getNamesystem().getBlockManager());
      BlockManagerTestUtil.updateState(nn2.getNamesystem().getBlockManager());
      assertEquals(0, nn1.getNamesystem().getCorruptReplicaBlocks());
      assertEquals(0, nn2.getNamesystem().getCorruptReplicaBlocks());

      AppendTestUtil.check(fs, TEST_FILE_PATH, 30);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testRBWReportArrivesAfterEdits with restart injection.
   */
  private void testRBWReportArrivesAfterEditsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, SMALL_BLOCK);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 600);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_MAX_STREAMS_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(3)
        .build();
    cluster.waitActive();
    cluster.transitionToActive(0);
    cluster.triggerBlockReports();

    NameNode nn1 = cluster.getNameNode(0);
    NameNode nn2 = cluster.getNameNode(1);
    FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);

    try {
      FSDataOutputStream out = fs.create(TEST_FILE_PATH);
      try {
        AppendTestUtil.write(out, 0, 10);
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster);
        LOG.info("=== RESTART COMPLETE ===");

        // Trigger block reports after restart
        cluster.triggerBlockReports();
      } finally {
        IOUtils.closeStream(out);
      }

      cluster.transitionToStandby(0);
      cluster.transitionToActive(1);

      // Verify that no replicas are marked corrupt
      BlockManagerTestUtil.updateState(nn1.getNamesystem().getBlockManager());
      BlockManagerTestUtil.updateState(nn2.getNamesystem().getBlockManager());
      assertEquals(0, nn1.getNamesystem().getCorruptReplicaBlocks());
      assertEquals(0, nn2.getNamesystem().getCorruptReplicaBlocks());

      DFSTestUtil.readFile(fs, TEST_FILE_PATH);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testBlockReportsWhileFileBeingWritten
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testBlockReportsWhileFileBeingWritten_AfterHflush_SingleDN_Graceful() throws Exception {
    testBlockReportsWhileFileBeingWrittenWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockReportsWhileFileBeingWritten_AfterHflush_SingleDN_Crash() throws Exception {
    testBlockReportsWhileFileBeingWrittenWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockReportsWhileFileBeingWritten_AfterHflush_AllDN_Graceful() throws Exception {
    testBlockReportsWhileFileBeingWrittenWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockReportsWhileFileBeingWritten_AfterHflush_AllDN_Crash() throws Exception {
    testBlockReportsWhileFileBeingWrittenWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testQueueingWithAppend
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testQueueingWithAppend_AfterHflush_SingleDN_Graceful() throws Exception {
    testQueueingWithAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testQueueingWithAppend_AfterHflush_SingleDN_Crash() throws Exception {
    testQueueingWithAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testQueueingWithAppend_AfterHflush_AllDN_Graceful() throws Exception {
    testQueueingWithAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testQueueingWithAppend_AfterHflush_AllDN_Crash() throws Exception {
    testQueueingWithAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testRBWReportArrivesAfterEdits
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testRBWReportArrivesAfterEdits_AfterHflush_SingleDN_Graceful() throws Exception {
    testRBWReportArrivesAfterEditsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRBWReportArrivesAfterEdits_AfterHflush_SingleDN_Crash() throws Exception {
    testRBWReportArrivesAfterEditsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRBWReportArrivesAfterEdits_AfterHflush_AllDN_Graceful() throws Exception {
    testRBWReportArrivesAfterEditsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRBWReportArrivesAfterEdits_AfterHflush_AllDN_Crash() throws Exception {
    testRBWReportArrivesAfterEditsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }
}

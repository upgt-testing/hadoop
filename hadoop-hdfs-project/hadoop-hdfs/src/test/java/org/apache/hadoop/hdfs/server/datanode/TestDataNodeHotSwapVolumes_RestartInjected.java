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
package org.apache.hadoop.hdfs.server.datanode;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDataNodeHotSwapVolumes.
 * Tests DataNode hot swap volume operations survive component restarts.
 *
 * Original test: testRemoveVolumeBeingWritten (has hflush)
 *
 * Note: The original test uses CyclicBarrier synchronization for testing
 * volume removal during active writes. This simplified version focuses on
 * verifying that data written with hflush survives restarts.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDataNodeHotSwapVolumes_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(
      TestDataNodeHotSwapVolumes_RestartInjected.class);
  private static final int BLOCK_SIZE = 512;

  /**
   * Core test logic with restart injection after hflush.
   */
  private void testVolumeWriteWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_DF_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_DATANODE_FAILED_VOLUMES_TOLERATED_KEY, 1);
    conf.setTimeDuration(DFSConfigKeys.DFS_DATANODE_DISK_CHECK_MIN_GAP_KEY,
        0, TimeUnit.MILLISECONDS);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSNNTopology nnTopology =
        MiniDFSNNTopology.simpleFederatedTopology(1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(nnTopology)
        .numDataNodes(4)
        .storagesPerDatanode(2)
        .build();
    cluster.waitActive();

    try {
      final short REPLICATION = 3;
      final DistributedFileSystem fs = cluster.getFileSystem(0);
      final Path testFile = new Path("/test");
      FSDataOutputStream out = fs.create(testFile, REPLICATION);

      Random rb = new Random(0);
      byte[] writeBuf = new byte[BLOCK_SIZE / 2];
      rb.nextBytes(writeBuf);
      out.write(writeBuf);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      rb.nextBytes(writeBuf);
      out.write(writeBuf);
      out.hflush();
      out.close();

      // Verify the file has sufficient replications
      DFSTestUtil.waitReplication(fs, testFile, REPLICATION);

      // Read the content back
      byte[] content = DFSTestUtil.readFileBuffer(fs, testFile);
      assertEquals(BLOCK_SIZE, content.length);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testVolumeWrite with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_NN_Graceful() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_NN_Crash() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_SingleDN_Graceful() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_SingleDN_Crash() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_AllDN_Graceful() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_AllDN_Crash() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_NNDN_Graceful() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeWrite_AfterHflush_NNDN_Crash() throws Exception {
    testVolumeWriteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

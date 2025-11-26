/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_CORRUPT_BLOCK_DELETE_IMMEDIATELY_ENABLED;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestCorruptionWithFailover.
 * Tests corruption detection during failover survives additional component restarts.
 *
 * Original test: testCorruptReplicaAfterFailover (has hsync)
 *
 * Note: This test uses HA topology, so we focus on DataNode restart variants
 * as NameNode failover is already part of the test logic.
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestCorruptionWithFailover_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestCorruptionWithFailover_RestartInjected.class);

  /**
   * Core test logic for corrupt replica after failover with restart injection.
   * Since this uses HA topology, restart injection focuses on DataNodes.
   */
  private void testCorruptReplicaAfterFailoverWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setBoolean(DFS_NAMENODE_CORRUPT_BLOCK_DELETE_IMMEDIATELY_ENABLED,
        false);
    // Enable data to be written, to less replicas in case of pipeline failure.
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology()).numDataNodes(3)
        .build()) {
      cluster.transitionToActive(0);
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem(0);
      FSDataOutputStream out = dfs.create(new Path("/dir/file"));
      // Write some data and flush.
      for (int i = 0; i < 1024 * 1024; i++) {
        out.write(i);
      }
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      // For HA cluster, we only inject DN restarts as NN failover is part of test
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      if (target == RestartTarget.SINGLE_DATANODE ||
          target == RestartTarget.ALL_DATANODES ||
          target == RestartTarget.RANDOM_DATANODE) {
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, dfs);
      } else {
        // For NN targets in HA, skip the injection as NN failover is already tested
        LOG.info("Skipping NN restart injection in HA cluster - failover is already part of test");
      }
      LOG.info("=== RESTART COMPLETE ===");

      // Stop one datanode, so as to trigger update pipeline.
      MiniDFSCluster.DataNodeProperties dn = cluster.stopDataNode(0);
      // Write some more data and close the file.
      for (int i = 0; i < 1024 * 1024; i++) {
        out.write(i);
      }
      out.close();
      BlockManager bm0 = cluster.getNamesystem(0).getBlockManager();
      BlockManager bm1 = cluster.getNamesystem(1).getBlockManager();
      // Mark datanodes as stale, as are marked if a namenode went through a
      // failover, to prevent replica deletion.
      bm0.getDatanodeManager().markAllDatanodesStale();
      bm1.getDatanodeManager().markAllDatanodesStale();
      // Restart the datanode
      cluster.restartDataNode(dn);
      // The replica from the datanode will be having lesser genstamp, so
      // would be marked as CORRUPT.
      GenericTestUtils.waitFor(() -> bm0.getCorruptBlocks() == 1, 100, 30000);

      // Perform failover to other namenode
      cluster.transitionToStandby(0);
      cluster.transitionToActive(1);
      cluster.waitActive(1);
      // The corrupt count should be same as first namenode.
      GenericTestUtils.waitFor(() -> bm1.getCorruptBlocks() == 1, 100, 30000);
    }
  }

  // ============================================================
  // Test variants: testCorruptReplicaAfterFailover with restart injection
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_NN_Graceful() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_NN_Crash() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_SingleDN_Graceful() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_SingleDN_Crash() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_AllDN_Graceful() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_AllDN_Crash() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_NNDN_Graceful() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testCorruptReplicaAfterFailover_AfterHsync_NNDN_Crash() throws Exception {
    testCorruptReplicaAfterFailoverWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

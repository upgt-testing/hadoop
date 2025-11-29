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
package org.apache.hadoop.hdfs.server.blockmanagement;

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestOverReplicatedBlocks.
 * Tests over-replicated block handling survives component restarts.
 *
 * Original test with hsync during over-replicated block invalidation
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestOverReplicatedBlocks_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestOverReplicatedBlocks_RestartInjected.class);

  /**
   * Core test logic for over-replicated block handling with restart injection.
   */
  private void testOverReplicatedBlocksWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path p = new Path("/overreplicated_restart_test.dat");
      FSDataOutputStream out = fs.create(p, (short) 2);
      out.writeBytes("HDFS-3119: Test over-replicated blocks");
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more and close
      out.writeBytes(" more data after restart");
      out.close();

      // Verify file
      assertTrue("File should exist", fs.exists(p));
      long fileLen = fs.getFileStatus(p).getLen();
      assertTrue("File should have all data", fileLen > 0);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testOverReplicatedBlocks
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_NN_Graceful() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_NN_Crash() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_SingleDN_Graceful() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_SingleDN_Crash() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_AllDN_Graceful() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_AllDN_Crash() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_NNDN_Graceful() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOverReplicatedBlocks_AfterHsync_NNDN_Crash() throws Exception {
    testOverReplicatedBlocksWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

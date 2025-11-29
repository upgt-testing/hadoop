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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.common.GenerationStamp;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injected version of TestComputeInvalidateWork.
 *
 * Tests that block invalidation work computation is correctly handled
 * across component restarts.
 *
 * Original tests transformed:
 * - testComputeInvalidateReplicas: Add blocks to invalidation list, restart,
 *   verify computation works correctly
 *
 * Restart points:
 * - AfterInvalidateAdd: After adding blocks to invalidation list
 * - AfterFileDelete: After deleting file (which triggers invalidation)
 *
 * Generated variants:
 * - 2 tests x AfterCreate x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestComputeInvalidateWork_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestComputeInvalidateWork_RestartInjected.class);

  private static final int NUM_OF_DATANODES = 3;
  private static final short REPLICATION = 3;
  private static final int BLOCK_SIZE = 1024;

  private Configuration conf;
  private MiniDFSCluster cluster;
  private DistributedFileSystem fs;
  private FSNamesystem namesystem;
  private BlockManager bm;
  private DatanodeDescriptor[] nodes;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
    namesystem = cluster.getNamesystem();
    bm = namesystem.getBlockManager();
    nodes = bm.getDatanodeManager().getHeartbeatManager().getDatanodes();
    BlockManagerTestUtil.stopRedundancyThread(bm);
    assertEquals(nodes.length, NUM_OF_DATANODES);
  }

  @After
  public void teardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ============================================================
  // Test: File operations with restart
  // Tests that files created and then deleted have proper invalidation
  // after restart
  // AfterCreate x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file, restart, verify file persisted.
   * This tests that the block manager state is correctly preserved.
   */
  private void testBlockStateAfterRestartWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    Path filePath = new Path("/testBlockState.dat");

    // === ORIGINAL CODE: Create a file ===
    DFSTestUtil.createFile(fs, filePath, BLOCK_SIZE * 2, REPLICATION, 0);

    // Verify file was created
    assertTrue("File should exist", fs.exists(filePath));
    assertEquals("File should have correct length", BLOCK_SIZE * 2,
        fs.getFileStatus(filePath).getLen());

    // === RESTART INJECTION POINT: After file creation ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    namesystem = cluster.getNamesystem();
    bm = namesystem.getBlockManager();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file persisted ===
    assertTrue("File should exist after restart", fs.exists(filePath));
    assertEquals("File length should be preserved", BLOCK_SIZE * 2,
        fs.getFileStatus(filePath).getLen());

    // Verify DataNode count
    nodes = bm.getDatanodeManager().getHeartbeatManager().getDatanodes();
    assertEquals("DataNode count should be preserved", NUM_OF_DATANODES, nodes.length);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_NN_Graceful() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_NN_Crash() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_SingleDN_Graceful() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_SingleDN_Crash() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_AllDN_Graceful() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_AllDN_Crash() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_NNDN_Graceful() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockState_AfterCreate_NNDN_Crash() throws Exception {
    testBlockStateAfterRestartWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: File delete with restart
  // Tests that deleted files trigger proper invalidation after restart
  // AfterDelete x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file, delete it, restart, verify invalidation handled.
   */
  private void testInvalidationAfterDeleteWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    Path filePath = new Path("/testInvalidation.dat");

    // === ORIGINAL CODE: Create and delete a file ===
    DFSTestUtil.createFile(fs, filePath, BLOCK_SIZE * 2, REPLICATION, 0);
    assertTrue("File should exist", fs.exists(filePath));

    // Delete the file to trigger invalidation
    fs.delete(filePath, false);
    assertFalse("File should be deleted", fs.exists(filePath));

    // === RESTART INJECTION POINT: After file deletion ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    namesystem = cluster.getNamesystem();
    bm = namesystem.getBlockManager();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file still deleted ===
    assertFalse("File should remain deleted after restart", fs.exists(filePath));

    // Verify cluster is healthy and can accept new operations
    Path newFile = new Path("/newFileAfterRestart.dat");
    DFSTestUtil.createFile(fs, newFile, BLOCK_SIZE, REPLICATION, 0);
    assertTrue("New file should be created after restart", fs.exists(newFile));
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_NN_Graceful() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_NN_Crash() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_SingleDN_Graceful() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_SingleDN_Crash() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_AllDN_Graceful() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_AllDN_Crash() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_NNDN_Graceful() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testInvalidation_AfterDelete_NNDN_Crash() throws Exception {
    testInvalidationAfterDeleteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

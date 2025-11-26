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

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.EnumSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.io.EnumSetWritable;
import org.apache.hadoop.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestBlockManager.
 *
 * Original test: TestBlockManager
 * This file contains restart injection variants for tests with hflush operations.
 */
public class TestBlockManager_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockManager_RestartInjected.class);

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Allow client to survive NN restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
  }

  @After
  public void tearDown() {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ==========================================================================
  // testNeededReconstructionWhileAppending - AFTER_FLUSH restart variants
  // ==========================================================================

  /**
   * Original: TestBlockManager#testNeededReconstructionWhileAppending
   * Restart: After hflush(), NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testNeededReconstructionWhileAppending_AfterFlush_NN_Graceful() throws Exception {
    LOG.info("=== Starting testNeededReconstructionWhileAppending_AfterFlush_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = "/test-file";
    Path file = new Path(src);
    cluster = new MiniDFSCluster.Builder(conf).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    NamenodeProtocols namenode = cluster.getNameNodeRpc();
    DFSOutputStream out = null;
    try {
      out = (DFSOutputStream) (fs.create(file).getWrappedStream());
      out.write(1);
      out.hflush();
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // Need to refresh references after NN restart
      bm = cluster.getNamesystem().getBlockManager();

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      out.close();
      FSDataInputStream in = null;
      ExtendedBlock oldBlock = null;
      try {
        in = fs.open(file);
        oldBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
      } finally {
        IOUtils.closeStream(in);
      }

      String clientName =
          ((DistributedFileSystem) fs).getClient().getClientName();
      namenode.append(src, clientName, new EnumSetWritable<>(
          EnumSet.of(CreateFlag.APPEND)));
      LocatedBlock newLocatedBlock =
          namenode.updateBlockForPipeline(oldBlock, clientName);
      ExtendedBlock newBlock =
          new ExtendedBlock(oldBlock.getBlockPoolId(), oldBlock.getBlockId(),
              oldBlock.getNumBytes(),
              newLocatedBlock.getBlock().getGenerationStamp());
      namenode.updatePipeline(clientName, oldBlock, newBlock,
          newLocatedBlock.getLocations(), newLocatedBlock.getStorageIDs());
      BlockInfo bi = bm.getStoredBlock(newBlock.getLocalBlock());
      assertFalse(bm.isNeededReconstruction(bi, bm.countNodes(bi,
          cluster.getNamesystem().isInStartupSafeMode())));
    } finally {
      IOUtils.closeStream(out);
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testNeededReconstructionWhileAppending
   * Restart: After hflush(), NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testNeededReconstructionWhileAppending_AfterFlush_NN_Crash() throws Exception {
    LOG.info("=== Starting testNeededReconstructionWhileAppending_AfterFlush_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = "/test-file";
    Path file = new Path(src);
    cluster = new MiniDFSCluster.Builder(conf).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    NamenodeProtocols namenode = cluster.getNameNodeRpc();
    DFSOutputStream out = null;
    try {
      out = (DFSOutputStream) (fs.create(file).getWrappedStream());
      out.write(1);
      out.hflush();
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // Need to refresh references after NN restart
      bm = cluster.getNamesystem().getBlockManager();

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      out.close();
      FSDataInputStream in = null;
      ExtendedBlock oldBlock = null;
      try {
        in = fs.open(file);
        oldBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
      } finally {
        IOUtils.closeStream(in);
      }

      String clientName =
          ((DistributedFileSystem) fs).getClient().getClientName();
      namenode.append(src, clientName, new EnumSetWritable<>(
          EnumSet.of(CreateFlag.APPEND)));
      LocatedBlock newLocatedBlock =
          namenode.updateBlockForPipeline(oldBlock, clientName);
      ExtendedBlock newBlock =
          new ExtendedBlock(oldBlock.getBlockPoolId(), oldBlock.getBlockId(),
              oldBlock.getNumBytes(),
              newLocatedBlock.getBlock().getGenerationStamp());
      namenode.updatePipeline(clientName, oldBlock, newBlock,
          newLocatedBlock.getLocations(), newLocatedBlock.getStorageIDs());
      BlockInfo bi = bm.getStoredBlock(newBlock.getLocalBlock());
      assertFalse(bm.isNeededReconstruction(bi, bm.countNodes(bi,
          cluster.getNamesystem().isInStartupSafeMode())));
    } finally {
      IOUtils.closeStream(out);
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testNeededReconstructionWhileAppending
   * Restart: After hflush(), SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testNeededReconstructionWhileAppending_AfterFlush_DN_Graceful() throws Exception {
    LOG.info("=== Starting testNeededReconstructionWhileAppending_AfterFlush_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = "/test-file";
    Path file = new Path(src);
    cluster = new MiniDFSCluster.Builder(conf).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    NamenodeProtocols namenode = cluster.getNameNodeRpc();
    DFSOutputStream out = null;
    try {
      out = (DFSOutputStream) (fs.create(file).getWrappedStream());
      out.write(1);
      out.hflush();
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      out.close();
      FSDataInputStream in = null;
      ExtendedBlock oldBlock = null;
      try {
        in = fs.open(file);
        oldBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
      } finally {
        IOUtils.closeStream(in);
      }

      String clientName =
          ((DistributedFileSystem) fs).getClient().getClientName();
      namenode.append(src, clientName, new EnumSetWritable<>(
          EnumSet.of(CreateFlag.APPEND)));
      LocatedBlock newLocatedBlock =
          namenode.updateBlockForPipeline(oldBlock, clientName);
      ExtendedBlock newBlock =
          new ExtendedBlock(oldBlock.getBlockPoolId(), oldBlock.getBlockId(),
              oldBlock.getNumBytes(),
              newLocatedBlock.getBlock().getGenerationStamp());
      namenode.updatePipeline(clientName, oldBlock, newBlock,
          newLocatedBlock.getLocations(), newLocatedBlock.getStorageIDs());
      BlockInfo bi = bm.getStoredBlock(newBlock.getLocalBlock());
      assertFalse(bm.isNeededReconstruction(bi, bm.countNodes(bi,
          cluster.getNamesystem().isInStartupSafeMode())));
    } finally {
      IOUtils.closeStream(out);
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testNeededReconstructionWhileAppending
   * Restart: After hflush(), SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testNeededReconstructionWhileAppending_AfterFlush_DN_Crash() throws Exception {
    LOG.info("=== Starting testNeededReconstructionWhileAppending_AfterFlush_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = "/test-file";
    Path file = new Path(src);
    cluster = new MiniDFSCluster.Builder(conf).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    NamenodeProtocols namenode = cluster.getNameNodeRpc();
    DFSOutputStream out = null;
    try {
      out = (DFSOutputStream) (fs.create(file).getWrappedStream());
      out.write(1);
      out.hflush();
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      out.close();
      FSDataInputStream in = null;
      ExtendedBlock oldBlock = null;
      try {
        in = fs.open(file);
        oldBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
      } finally {
        IOUtils.closeStream(in);
      }

      String clientName =
          ((DistributedFileSystem) fs).getClient().getClientName();
      namenode.append(src, clientName, new EnumSetWritable<>(
          EnumSet.of(CreateFlag.APPEND)));
      LocatedBlock newLocatedBlock =
          namenode.updateBlockForPipeline(oldBlock, clientName);
      ExtendedBlock newBlock =
          new ExtendedBlock(oldBlock.getBlockPoolId(), oldBlock.getBlockId(),
              oldBlock.getNumBytes(),
              newLocatedBlock.getBlock().getGenerationStamp());
      namenode.updatePipeline(clientName, oldBlock, newBlock,
          newLocatedBlock.getLocations(), newLocatedBlock.getStorageIDs());
      BlockInfo bi = bm.getStoredBlock(newBlock.getLocalBlock());
      assertFalse(bm.isNeededReconstruction(bi, bm.countNodes(bi,
          cluster.getNamesystem().isInStartupSafeMode())));
    } finally {
      IOUtils.closeStream(out);
    }
    // === ORIGINAL CODE END ===
  }

  // ==========================================================================
  // testDeleteCorruptReplicaWithStaleStorages - AFTER_FLUSH restart variants
  // ==========================================================================

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NN_Graceful() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh references after NN restart
    blockManager = cluster.getNamesystem().getBlockManager();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NN_Crash() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh references after NN restart
    blockManager = cluster.getNamesystem().getBlockManager();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_DN_Graceful() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_DN_Crash() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh references after NN restart
    blockManager = cluster.getNamesystem().getBlockManager();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockManager#testDeleteCorruptReplicaWithStatleStorages
   * Restart: After hflush(), NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testDeleteCorruptReplicaWithStaleStorages_AfterFlush_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setInt(HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.
        MIN_REPLICATION, 2);
    Path file = new Path("/test-file");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    BlockManager blockManager = cluster.getNamesystem().getBlockManager();
    blockManager.getDatanodeManager().markAllDatanodesStale();
    FSDataOutputStream out = fs.create(file);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh references after NN restart
    blockManager = cluster.getNamesystem().getBlockManager();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    MiniDFSCluster.DataNodeProperties datanode = cluster.stopDataNode(0);
    for (int i = 0; i < 1024 * 1024 * 1; i++) {
      out.write(i);
    }
    out.close();
    cluster.restartDataNode(datanode);
    cluster.triggerBlockReports();
    DataNodeTestUtils.triggerBlockReport(datanode.getDatanode());
    assertEquals(0, blockManager.getCorruptBlocks());
    // === ORIGINAL CODE END ===
  }
}

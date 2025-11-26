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
package org.apache.hadoop.hdfs.server.datanode;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SIZE_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_REPLICATION_MIN_KEY;
import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.util.AutoCloseableLock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestBlockRecovery2.
 *
 * Original test: TestBlockRecovery2
 * This file contains restart injection variants for tests with hsync operations.
 */
public class TestBlockRecovery2_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockRecovery2_RestartInjected.class);

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
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ==========================================================================
  // testRaceBetweenReplicaRecoveryAndFinalizeBlock - AFTER_SYNC restart variants
  // ==========================================================================

  /**
   * Original: TestBlockRecovery2#testRaceBetweenReplicaRecoveryAndFinalizeBlock
   * Restart: After hsync(), NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_NN_Graceful() throws Exception {
    LOG.info("=== Starting testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setLong(DFSConfigKeys.DFS_DATANODE_XCEIVER_STOP_TIMEOUT_MILLIS_KEY, 5000L);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();

    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = cluster.getFileSystem();
    Path path = new Path("/test");
    FSDataOutputStream out = dfs.create(path);
    out.writeBytes("data");
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    List<LocatedBlock> blocks = DFSTestUtil.getAllBlocks(dfs.open(path));
    final LocatedBlock block = blocks.get(0);
    final DataNode dataNode = cluster.getDataNodes().get(0);

    final AtomicBoolean recoveryInitResult = new AtomicBoolean(true);
    Thread recoveryThread = new Thread(() -> {
      try {
        DatanodeInfo[] locations = block.getLocations();
        final BlockRecoveryCommand.RecoveringBlock recoveringBlock =
            new BlockRecoveryCommand.RecoveringBlock(block.getBlock(),
                locations, block.getBlock().getGenerationStamp() + 1);
        try (AutoCloseableLock lock = dataNode.data.acquireDatasetLock()) {
          Thread.sleep(2000);
          dataNode.initReplicaRecovery(recoveringBlock);
        }
      } catch (Exception e) {
        LOG.error("Something went wrong.", e);
        recoveryInitResult.set(false);
      }
    });
    recoveryThread.start();
    try {
      out.close();
    } catch (Exception e) {
      Assert.assertTrue("Writing should fail",
          e.getMessage().contains("are bad. Aborting..."));
    } finally {
      recoveryThread.join();
    }
    Assert.assertTrue("Recovery should be initiated successfully",
        recoveryInitResult.get());

    dataNode.updateReplicaUnderRecovery(block.getBlock(), block.getBlock()
            .getGenerationStamp() + 1, block.getBlock().getBlockId(),
        block.getBlockSize());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRaceBetweenReplicaRecoveryAndFinalizeBlock
   * Restart: After hsync(), NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_NN_Crash() throws Exception {
    LOG.info("=== Starting testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setLong(DFSConfigKeys.DFS_DATANODE_XCEIVER_STOP_TIMEOUT_MILLIS_KEY, 5000L);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();

    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = cluster.getFileSystem();
    Path path = new Path("/test");
    FSDataOutputStream out = dfs.create(path);
    out.writeBytes("data");
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    List<LocatedBlock> blocks = DFSTestUtil.getAllBlocks(dfs.open(path));
    final LocatedBlock block = blocks.get(0);
    final DataNode dataNode = cluster.getDataNodes().get(0);

    final AtomicBoolean recoveryInitResult = new AtomicBoolean(true);
    Thread recoveryThread = new Thread(() -> {
      try {
        DatanodeInfo[] locations = block.getLocations();
        final BlockRecoveryCommand.RecoveringBlock recoveringBlock =
            new BlockRecoveryCommand.RecoveringBlock(block.getBlock(),
                locations, block.getBlock().getGenerationStamp() + 1);
        try (AutoCloseableLock lock = dataNode.data.acquireDatasetLock()) {
          Thread.sleep(2000);
          dataNode.initReplicaRecovery(recoveringBlock);
        }
      } catch (Exception e) {
        LOG.error("Something went wrong.", e);
        recoveryInitResult.set(false);
      }
    });
    recoveryThread.start();
    try {
      out.close();
    } catch (Exception e) {
      Assert.assertTrue("Writing should fail",
          e.getMessage().contains("are bad. Aborting..."));
    } finally {
      recoveryThread.join();
    }
    Assert.assertTrue("Recovery should be initiated successfully",
        recoveryInitResult.get());

    dataNode.updateReplicaUnderRecovery(block.getBlock(), block.getBlock()
            .getGenerationStamp() + 1, block.getBlock().getBlockId(),
        block.getBlockSize());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRaceBetweenReplicaRecoveryAndFinalizeBlock
   * Restart: After hsync(), SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_DN_Graceful() throws Exception {
    LOG.info("=== Starting testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setLong(DFSConfigKeys.DFS_DATANODE_XCEIVER_STOP_TIMEOUT_MILLIS_KEY, 5000L);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();

    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = cluster.getFileSystem();
    Path path = new Path("/test");
    FSDataOutputStream out = dfs.create(path);
    out.writeBytes("data");
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    List<LocatedBlock> blocks = DFSTestUtil.getAllBlocks(dfs.open(path));
    final LocatedBlock block = blocks.get(0);
    final DataNode dataNode = cluster.getDataNodes().get(0);

    final AtomicBoolean recoveryInitResult = new AtomicBoolean(true);
    Thread recoveryThread = new Thread(() -> {
      try {
        DatanodeInfo[] locations = block.getLocations();
        final BlockRecoveryCommand.RecoveringBlock recoveringBlock =
            new BlockRecoveryCommand.RecoveringBlock(block.getBlock(),
                locations, block.getBlock().getGenerationStamp() + 1);
        try (AutoCloseableLock lock = dataNode.data.acquireDatasetLock()) {
          Thread.sleep(2000);
          dataNode.initReplicaRecovery(recoveringBlock);
        }
      } catch (Exception e) {
        LOG.error("Something went wrong.", e);
        recoveryInitResult.set(false);
      }
    });
    recoveryThread.start();
    try {
      out.close();
    } catch (Exception e) {
      Assert.assertTrue("Writing should fail",
          e.getMessage().contains("are bad. Aborting..."));
    } finally {
      recoveryThread.join();
    }
    Assert.assertTrue("Recovery should be initiated successfully",
        recoveryInitResult.get());

    dataNode.updateReplicaUnderRecovery(block.getBlock(), block.getBlock()
            .getGenerationStamp() + 1, block.getBlock().getBlockId(),
        block.getBlockSize());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRaceBetweenReplicaRecoveryAndFinalizeBlock
   * Restart: After hsync(), SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_DN_Crash() throws Exception {
    LOG.info("=== Starting testRaceBetweenReplicaRecoveryAndFinalizeBlock_AfterSync_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    conf.setLong(DFSConfigKeys.DFS_DATANODE_XCEIVER_STOP_TIMEOUT_MILLIS_KEY, 5000L);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();

    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = cluster.getFileSystem();
    Path path = new Path("/test");
    FSDataOutputStream out = dfs.create(path);
    out.writeBytes("data");
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    List<LocatedBlock> blocks = DFSTestUtil.getAllBlocks(dfs.open(path));
    final LocatedBlock block = blocks.get(0);
    final DataNode dataNode = cluster.getDataNodes().get(0);

    final AtomicBoolean recoveryInitResult = new AtomicBoolean(true);
    Thread recoveryThread = new Thread(() -> {
      try {
        DatanodeInfo[] locations = block.getLocations();
        final BlockRecoveryCommand.RecoveringBlock recoveringBlock =
            new BlockRecoveryCommand.RecoveringBlock(block.getBlock(),
                locations, block.getBlock().getGenerationStamp() + 1);
        try (AutoCloseableLock lock = dataNode.data.acquireDatasetLock()) {
          Thread.sleep(2000);
          dataNode.initReplicaRecovery(recoveringBlock);
        }
      } catch (Exception e) {
        LOG.error("Something went wrong.", e);
        recoveryInitResult.set(false);
      }
    });
    recoveryThread.start();
    try {
      out.close();
    } catch (Exception e) {
      Assert.assertTrue("Writing should fail",
          e.getMessage().contains("are bad. Aborting..."));
    } finally {
      recoveryThread.join();
    }
    Assert.assertTrue("Recovery should be initiated successfully",
        recoveryInitResult.get());

    dataNode.updateReplicaUnderRecovery(block.getBlock(), block.getBlock()
            .getGenerationStamp() + 1, block.getBlock().getBlockId(),
        block.getBlockSize());
    // === ORIGINAL CODE END ===
  }

  // ==========================================================================
  // testRecoveryWillIgnoreMinReplication - AFTER_SYNC restart variants
  // ==========================================================================

  /**
   * Original: TestBlockRecovery2#testRecoveryWillIgnoreMinReplication
   * Restart: After hsync(), NameNode, Graceful
   */
  @Test(timeout = 360000)
  public void testRecoveryWillIgnoreMinReplication_AfterSync_NN_Graceful() throws Exception {
    LOG.info("=== Starting testRecoveryWillIgnoreMinReplication_AfterSync_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    final int blockSize = 4096;
    final int numReplicas = 3;
    final String filename = "/testIgnoreMinReplication";
    final Path filePath = new Path(filename);
    conf.setInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 2000);
    conf.setInt(DFS_NAMENODE_REPLICATION_MIN_KEY, 2);
    conf.setLong(DFS_BLOCK_SIZE_KEY, blockSize);

    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(5).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
    final DistributedFileSystem dfs = cluster.getFileSystem();
    final FSNamesystem fsn = cluster.getNamesystem();

    // Create a file and never close the output stream to trigger recovery
    FSDataOutputStream out = dfs.create(filePath, (short) numReplicas);
    out.write(AppendTestUtil.randomBytes(0, blockSize));
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    DFSClient dfsClient = new DFSClient(new InetSocketAddress("localhost",
        cluster.getNameNodePort()), conf);
    LocatedBlock blk = dfsClient.getNamenode().
        getBlockLocations(filename, 0, blockSize).
        getLastLocatedBlock();

    // Kill 2 out of 3 datanodes so that only 1 alive, thus < minReplication
    List<DatanodeInfo> dataNodes = Arrays.asList(blk.getLocations());
    assertEquals(dataNodes.size(), numReplicas);
    for (DatanodeInfo dataNode : dataNodes.subList(0, numReplicas - 1)) {
      cluster.stopDataNode(dataNode.getName());
    }

    GenericTestUtils.waitFor(() -> cluster.getNamesystem().getNumDeadDataNodes() == 2,
        300, 300000);

    // Make sure hard lease expires to trigger replica recovery
    cluster.setLeasePeriod(100L, 100L);

    // Wait for recovery to succeed
    GenericTestUtils.waitFor(() -> {
      try {
        return dfs.isFileClosed(filePath);
      } catch (Exception e) {
        LOG.info("Something went wrong.", e);
      }
      return false;
    }, 300, 300000);

    // Wait for the block to be replicated
    DFSTestUtil.waitForReplication(cluster, DFSTestUtil.getFirstBlock(
        dfs, filePath), 1, numReplicas, 0);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRecoveryWillIgnoreMinReplication
   * Restart: After hsync(), NameNode, Crash
   */
  @Test(timeout = 360000)
  public void testRecoveryWillIgnoreMinReplication_AfterSync_NN_Crash() throws Exception {
    LOG.info("=== Starting testRecoveryWillIgnoreMinReplication_AfterSync_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    final int blockSize = 4096;
    final int numReplicas = 3;
    final String filename = "/testIgnoreMinReplication";
    final Path filePath = new Path(filename);
    conf.setInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 2000);
    conf.setInt(DFS_NAMENODE_REPLICATION_MIN_KEY, 2);
    conf.setLong(DFS_BLOCK_SIZE_KEY, blockSize);

    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(5).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
    final DistributedFileSystem dfs = cluster.getFileSystem();
    final FSNamesystem fsn = cluster.getNamesystem();

    // Create a file and never close the output stream to trigger recovery
    FSDataOutputStream out = dfs.create(filePath, (short) numReplicas);
    out.write(AppendTestUtil.randomBytes(0, blockSize));
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    DFSClient dfsClient = new DFSClient(new InetSocketAddress("localhost",
        cluster.getNameNodePort()), conf);
    LocatedBlock blk = dfsClient.getNamenode().
        getBlockLocations(filename, 0, blockSize).
        getLastLocatedBlock();

    // Kill 2 out of 3 datanodes so that only 1 alive, thus < minReplication
    List<DatanodeInfo> dataNodes = Arrays.asList(blk.getLocations());
    assertEquals(dataNodes.size(), numReplicas);
    for (DatanodeInfo dataNode : dataNodes.subList(0, numReplicas - 1)) {
      cluster.stopDataNode(dataNode.getName());
    }

    GenericTestUtils.waitFor(() -> cluster.getNamesystem().getNumDeadDataNodes() == 2,
        300, 300000);

    // Make sure hard lease expires to trigger replica recovery
    cluster.setLeasePeriod(100L, 100L);

    // Wait for recovery to succeed
    GenericTestUtils.waitFor(() -> {
      try {
        return dfs.isFileClosed(filePath);
      } catch (Exception e) {
        LOG.info("Something went wrong.", e);
      }
      return false;
    }, 300, 300000);

    // Wait for the block to be replicated
    DFSTestUtil.waitForReplication(cluster, DFSTestUtil.getFirstBlock(
        dfs, filePath), 1, numReplicas, 0);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRecoveryWillIgnoreMinReplication
   * Restart: After hsync(), SingleDataNode, Graceful
   */
  @Test(timeout = 360000)
  public void testRecoveryWillIgnoreMinReplication_AfterSync_DN_Graceful() throws Exception {
    LOG.info("=== Starting testRecoveryWillIgnoreMinReplication_AfterSync_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    final int blockSize = 4096;
    final int numReplicas = 3;
    final String filename = "/testIgnoreMinReplication";
    final Path filePath = new Path(filename);
    conf.setInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 2000);
    conf.setInt(DFS_NAMENODE_REPLICATION_MIN_KEY, 2);
    conf.setLong(DFS_BLOCK_SIZE_KEY, blockSize);

    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(5).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
    final DistributedFileSystem dfs = cluster.getFileSystem();
    final FSNamesystem fsn = cluster.getNamesystem();

    // Create a file and never close the output stream to trigger recovery
    FSDataOutputStream out = dfs.create(filePath, (short) numReplicas);
    out.write(AppendTestUtil.randomBytes(0, blockSize));
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    DFSClient dfsClient = new DFSClient(new InetSocketAddress("localhost",
        cluster.getNameNodePort()), conf);
    LocatedBlock blk = dfsClient.getNamenode().
        getBlockLocations(filename, 0, blockSize).
        getLastLocatedBlock();

    // Kill 2 out of 3 datanodes so that only 1 alive, thus < minReplication
    List<DatanodeInfo> dataNodes = Arrays.asList(blk.getLocations());
    assertEquals(dataNodes.size(), numReplicas);
    for (DatanodeInfo dataNode : dataNodes.subList(0, numReplicas - 1)) {
      cluster.stopDataNode(dataNode.getName());
    }

    GenericTestUtils.waitFor(() -> cluster.getNamesystem().getNumDeadDataNodes() == 2,
        300, 300000);

    // Make sure hard lease expires to trigger replica recovery
    cluster.setLeasePeriod(100L, 100L);

    // Wait for recovery to succeed
    GenericTestUtils.waitFor(() -> {
      try {
        return dfs.isFileClosed(filePath);
      } catch (Exception e) {
        LOG.info("Something went wrong.", e);
      }
      return false;
    }, 300, 300000);

    // Wait for the block to be replicated
    DFSTestUtil.waitForReplication(cluster, DFSTestUtil.getFirstBlock(
        dfs, filePath), 1, numReplicas, 0);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBlockRecovery2#testRecoveryWillIgnoreMinReplication
   * Restart: After hsync(), SingleDataNode, Crash
   */
  @Test(timeout = 360000)
  public void testRecoveryWillIgnoreMinReplication_AfterSync_DN_Crash() throws Exception {
    LOG.info("=== Starting testRecoveryWillIgnoreMinReplication_AfterSync_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    final int blockSize = 4096;
    final int numReplicas = 3;
    final String filename = "/testIgnoreMinReplication";
    final Path filePath = new Path(filename);
    conf.setInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 2000);
    conf.setInt(DFS_NAMENODE_REPLICATION_MIN_KEY, 2);
    conf.setLong(DFS_BLOCK_SIZE_KEY, blockSize);

    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(5).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
    final DistributedFileSystem dfs = cluster.getFileSystem();
    final FSNamesystem fsn = cluster.getNamesystem();

    // Create a file and never close the output stream to trigger recovery
    FSDataOutputStream out = dfs.create(filePath, (short) numReplicas);
    out.write(AppendTestUtil.randomBytes(0, blockSize));
    out.hsync();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    DFSClient dfsClient = new DFSClient(new InetSocketAddress("localhost",
        cluster.getNameNodePort()), conf);
    LocatedBlock blk = dfsClient.getNamenode().
        getBlockLocations(filename, 0, blockSize).
        getLastLocatedBlock();

    // Kill 2 out of 3 datanodes so that only 1 alive, thus < minReplication
    List<DatanodeInfo> dataNodes = Arrays.asList(blk.getLocations());
    assertEquals(dataNodes.size(), numReplicas);
    for (DatanodeInfo dataNode : dataNodes.subList(0, numReplicas - 1)) {
      cluster.stopDataNode(dataNode.getName());
    }

    GenericTestUtils.waitFor(() -> cluster.getNamesystem().getNumDeadDataNodes() == 2,
        300, 300000);

    // Make sure hard lease expires to trigger replica recovery
    cluster.setLeasePeriod(100L, 100L);

    // Wait for recovery to succeed
    GenericTestUtils.waitFor(() -> {
      try {
        return dfs.isFileClosed(filePath);
      } catch (Exception e) {
        LOG.info("Something went wrong.", e);
      }
      return false;
    }, 300, 300000);

    // Wait for the block to be replicated
    DFSTestUtil.waitForReplication(cluster, DFSTestUtil.getFirstBlock(
        dfs, filePath), 1, numReplicas, 0);
    // === ORIGINAL CODE END ===
  }
}

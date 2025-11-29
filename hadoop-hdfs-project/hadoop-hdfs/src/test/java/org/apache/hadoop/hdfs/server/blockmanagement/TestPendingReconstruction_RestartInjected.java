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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_RECONSTRUCTION_PENDING_TIMEOUT_SEC_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY;
import static org.apache.hadoop.test.MetricsAsserts.assertCounter;
import static org.apache.hadoop.test.MetricsAsserts.getLongCounter;
import static org.apache.hadoop.test.MetricsAsserts.getMetrics;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.NameNodeAdapter;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;
import org.apache.hadoop.hdfs.server.protocol.ReceivedDeletedBlockInfo;
import org.apache.hadoop.hdfs.server.protocol.ReceivedDeletedBlockInfo.BlockStatus;
import org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.GenericTestUtils.LogCapturer;
import org.apache.log4j.Level;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestPendingReconstruction.
 *
 * This class tests the internals of PendingReconstructionBlocks.java with restart injection.
 */
public class TestPendingReconstruction_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestPendingReconstruction_RestartInjected.class);

  final static int TIMEOUT = 3;     // 3 seconds
  private static final int DFS_REPLICATION_INTERVAL = 1;
  private static final int DATANODE_COUNT = 5;

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_NN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_NN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_DN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_DN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_AllDN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_AllDN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_NNAndDN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testBlockReceived
   * Restart: After file creation, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testBlockReceived_AfterCreate_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testBlockReceived_AfterCreate_NNAndDN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(
          DATANODE_COUNT).build();
      cluster.waitActive();

      DistributedFileSystem hdfs = cluster.getFileSystem();
      FSNamesystem fsn = cluster.getNamesystem();
      BlockManager blkManager = fsn.getBlockManager();

      final String file = "/tmp.txt";
      final Path filePath = new Path(file);
      short replFactor = 1;
      DFSTestUtil.createFile(hdfs, filePath, 1024L, replFactor, 0);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);

      ArrayList<DataNode> datanodes = cluster.getDataNodes();
      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(datanodes.get(i), true);
      }

      hdfs.setReplication(filePath, (short) DATANODE_COUNT);
      BlockManagerTestUtil.computeAllPendingWork(blkManager);

      assertEquals(1, blkManager.pendingReconstruction.size());
      INodeFile fileNode = fsn.getFSDirectory().getINode4Write(file).asFile();
      BlockInfo[] blocks = fileNode.getBlocks();
      assertEquals(DATANODE_COUNT - 1,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      LocatedBlock locatedBlock = hdfs.getClient().getLocatedBlocks(file, 0)
          .get(0);
      DatanodeInfo existingDn = (locatedBlock.getLocations())[0];
      int reportDnNum = 0;
      String poolId = cluster.getNamesystem().getBlockPoolId();
      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report = {
              new StorageReceivedDeletedBlocks(
                  new DatanodeStorage("Fake-storage-ID-Ignored"),
              new ReceivedDeletedBlockInfo[] { new ReceivedDeletedBlockInfo(
                  blocks[0], BlockStatus.RECEIVED_BLOCK, "") }) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }
      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT && reportDnNum < 2; i++) {
        if (!datanodes.get(i).getDatanodeId().equals(existingDn)) {
          DatanodeRegistration dnR = datanodes.get(i).getDNRegistrationForBP(
              poolId);
          StorageReceivedDeletedBlocks[] report =
            { new StorageReceivedDeletedBlocks(
                new DatanodeStorage("Fake-storage-ID-Ignored"),
                new ReceivedDeletedBlockInfo[] {
                  new ReceivedDeletedBlockInfo(
                      blocks[0], BlockStatus.RECEIVED_BLOCK, "")}) };
          cluster.getNameNodeRpc().blockReceivedAndDeleted(dnR, poolId, report);
          reportDnNum++;
        }
      }

      cluster.getNamesystem().getBlockManager().flushBlockOps();
      assertEquals(DATANODE_COUNT - 3,
          blkManager.pendingReconstruction.getNumReplicas(blocks[0]));

      for (int i = 0; i < DATANODE_COUNT; i++) {
        DataNodeTestUtils
            .setHeartbeatsDisabledForTests(datanodes.get(i), false);
        DataNodeTestUtils.triggerHeartbeat(datanodes.get(i));
      }

      Thread.sleep(5000);
      assertEquals(0, blkManager.pendingReconstruction.size());
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_NN_Graceful ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_NN_Crash ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_DN_Graceful ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_DN_Crash ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_AllDN_Graceful ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_AllDN_Crash ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_NNAndDN_Graceful ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingAndInvalidate
   * Restart: After file creation, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testPendingAndInvalidate_AfterCreate_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testPendingAndInvalidate_AfterCreate_NNAndDN_Crash ===");
    final Configuration CONF = new HdfsConfiguration();
    CONF.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    CONF.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFS_REPLICATION_INTERVAL);
    CONF.setInt(DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFS_REPLICATION_INTERVAL);
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(CONF).numDataNodes(
        DATANODE_COUNT).build();
    cluster.waitActive();

    FSNamesystem namesystem = cluster.getNamesystem();
    BlockManager bm = namesystem.getBlockManager();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0L);
      DFSTestUtil.waitForReplication(cluster.getFileSystem(), filePath,
          (short) 3, 10000);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      LocatedBlock block = NameNodeAdapter.getBlockLocations(
          cluster.getNameNode(), filePath.toString(), 0, 1).get(0);
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[0],
            "STORAGE_ID", "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), block.getLocations()[1],
            "STORAGE_ID", "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(bm.getPendingReconstructionBlocksCount(), 1L);
        BlockInfo storedBlock = bm.getStoredBlock(block.getBlock().getLocalBlock());
        assertEquals(bm.pendingReconstruction.getNumReplicas(storedBlock), 2);
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      fs.delete(filePath, true);
      int retries = 10;
      long pendingNum = bm.getPendingReconstructionBlocksCount();
      while (pendingNum != 0 && retries-- > 0) {
        Thread.sleep(1000);
        BlockManagerTestUtil.updateState(bm);
        pendingNum = bm.getPendingReconstructionBlocksCount();
      }
      assertEquals(pendingNum, 0L);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_NN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_NN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_DN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_DN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_AllDN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_AllDN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_NNAndDN_Graceful ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Original: TestPendingReconstruction#testPendingReConstructionBlocksForSameDN
   * Restart: After file creation, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testPendingReConstructionBlocksForSameDN_AfterCreate_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testPendingReConstructionBlocksForSameDN_AfterCreate_NNAndDN_Crash ===");
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    DFSTestUtil.setNameNodeLogLevel(Level.DEBUG);
    LogCapturer logs = GenericTestUtils.LogCapturer
        .captureLogs(LoggerFactory.getLogger("BlockStateChange"));
    BlockManager bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 1, 0L);

      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      for (DataNode dn : cluster.getDataNodes()) {
        DataNodeTestUtils.pauseIBR(dn);
      }
      DatanodeManager datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList =
          new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);

      dfs.setReplication(filePath, (short) 3);

      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.computeAllPendingWork(bm);
      BlockManagerTestUtil.updateState(bm);

      String blockName =
          "to replicate " + block.getBlock().getLocalBlock().toString();
      assertEquals(1, StringUtils.countMatches(logs.getOutput(), blockName));
    } finally {
      cluster.shutdown();
    }
  }
}

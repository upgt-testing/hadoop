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
package org.apache.hadoop.hdfs.server.namenode;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;

import java.util.EnumSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestAddBlock.
 *
 * Original test: TestAddBlock
 * This file contains all restart injection variants for the original test.
 *
 * Test AddBlockOp is written and read correctly with component restarts.
 */
public class TestAddBlock_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestAddBlock_RestartInjected.class);

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  private MiniDFSCluster cluster;
  private Configuration conf;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    // Allow client to survive NN restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
  }

  @After
  public void tearDown() {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ==========================================================================
  // testAddBlock - AFTER_FILE_CREATE restart variants
  // Tests that AddBlockOp is persisted correctly after file creation
  // ==========================================================================

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlock
   * Restart: After file creation, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlock_AfterCreate_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlock_AfterCreate_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();

    final Path file1 = new Path("/file1");
    final Path file2 = new Path("/file2");
    final Path file3 = new Path("/file3");
    final Path file4 = new Path("/file4");

    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file2, BLOCKSIZE, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file3, BLOCKSIZE * 2 - 1, REPLICATION, 0L);
    DFSTestUtil.createFile(dfs, file4, BLOCKSIZE * 2, REPLICATION, 0L);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // restart NameNode
    cluster.restartNameNode(true);
    FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

    // check file1
    INodeFile file1Node = fsdir.getINode4Write(file1.toString()).asFile();
    BlockInfo[] file1Blocks = file1Node.getBlocks();
    assertEquals(1, file1Blocks.length);
    assertEquals(BLOCKSIZE - 1, file1Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file1Blocks[0].getBlockUCState());

    // check file2
    INodeFile file2Node = fsdir.getINode4Write(file2.toString()).asFile();
    BlockInfo[] file2Blocks = file2Node.getBlocks();
    assertEquals(1, file2Blocks.length);
    assertEquals(BLOCKSIZE, file2Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file2Blocks[0].getBlockUCState());

    // check file3
    INodeFile file3Node = fsdir.getINode4Write(file3.toString()).asFile();
    BlockInfo[] file3Blocks = file3Node.getBlocks();
    assertEquals(2, file3Blocks.length);
    assertEquals(BLOCKSIZE, file3Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE - 1, file3Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file3Blocks[1].getBlockUCState());

    // check file4
    INodeFile file4Node = fsdir.getINode4Write(file4.toString()).asFile();
    BlockInfo[] file4Blocks = file4Node.getBlocks();
    assertEquals(2, file4Blocks.length);
    assertEquals(BLOCKSIZE, file4Blocks[0].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[0].getBlockUCState());
    assertEquals(BLOCKSIZE, file4Blocks[1].getNumBytes());
    assertEquals(BlockUCState.COMPLETE, file4Blocks[1].getBlockUCState());
    // === ORIGINAL CODE END ===
  }

  // ==========================================================================
  // testAddBlockUC - AFTER_SYNC restart variants
  // Tests block under construction persistence after hsync
  // ==========================================================================

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_NN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_DN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAddBlock#testAddBlockUC
   * Restart: After hsync with UPDATE_LENGTH, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAddBlockUC_AfterSync_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testAddBlockUC_AfterSync_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    DistributedFileSystem dfs = cluster.getFileSystem();
    final Path file1 = new Path("/file1");
    DFSTestUtil.createFile(dfs, file1, BLOCKSIZE - 1, REPLICATION, 0L);

    FSDataOutputStream out = null;
    try {
      // append files without closing the streams
      out = dfs.append(file1);
      String appendContent = "appending-content";
      out.writeBytes(appendContent);
      ((DFSOutputStream) out.getWrappedStream()).hsync(
          EnumSet.of(SyncFlag.UPDATE_LENGTH));
      // === ORIGINAL CODE PAUSE ===

      // === RESTART INJECTION ===
      executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      // === ORIGINAL CODE RESUME (UNCHANGED) ===
      // restart NN
      cluster.restartNameNode(true);
      FSDirectory fsdir = cluster.getNamesystem().getFSDirectory();

      INodeFile fileNode = fsdir.getINode4Write(file1.toString()).asFile();
      BlockInfo[] fileBlocks = fileNode.getBlocks();
      assertEquals(2, fileBlocks.length);
      assertEquals(BLOCKSIZE, fileBlocks[0].getNumBytes());
      assertEquals(BlockUCState.COMPLETE, fileBlocks[0].getBlockUCState());
      assertEquals(appendContent.length() - 1, fileBlocks[1].getNumBytes());
      assertEquals(BlockUCState.UNDER_CONSTRUCTION,
          fileBlocks[1].getBlockUCState());
    } finally {
      if (out != null) {
        out.close();
      }
    }
    // === ORIGINAL CODE END ===
  }
}

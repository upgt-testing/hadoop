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

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Restart-injected version of TestAbandonBlock.
 *
 * Original test: TestAbandonBlock
 * This file contains all restart injection variants for the original test.
 *
 * Tests abandoning blocks with component restarts at various points.
 */
public class TestAbandonBlock_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestAbandonBlock_RestartInjected.class);

  private static final String FILE_NAME_PREFIX
      = "/" + TestAbandonBlock_RestartInjected.class.getSimpleName() + "_";

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileSystem fs;

  @Before
  public void setUp() throws Exception {
    conf = new HdfsConfiguration();
    // Allow client to survive NN restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
  }

  @After
  public void tearDown() throws Exception {
    if (fs != null) {
      fs.close();
      fs = null;
    }
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ==========================================================================
  // testAbandonBlock - AFTER_FLUSH restart variants
  // ==========================================================================

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_NN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_NN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_NN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_DN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_DN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_DN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_AllDN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_AllDN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), RandomDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_RandomDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_RandomDN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), RandomDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_RandomDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_RandomDN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_NNAndDN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After hflush(), NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterFlush_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterFlush_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterFlush_NNAndDN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  // ==========================================================================
  // testAbandonBlock - AFTER_ABANDON restart variants
  // Tests that abandonBlock operation is persisted correctly
  // ==========================================================================

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_NN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_NN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_NN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), SingleDataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_DN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), SingleDataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_DN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_DN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), AllDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_AllDN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), AllDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_AllDN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_NNAndDN_Graceful";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestAbandonBlock#testAbandonBlock
   * Restart: After abandonBlock(), NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 120000)
  public void testAbandonBlock_AfterAbandon_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testAbandonBlock_AfterAbandon_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    String src = FILE_NAME_PREFIX + "foo_AfterAbandon_NNAndDN_Crash";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    fout.hflush();
    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem)fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode();
    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
    // === ORIGINAL CODE END ===
  }
}

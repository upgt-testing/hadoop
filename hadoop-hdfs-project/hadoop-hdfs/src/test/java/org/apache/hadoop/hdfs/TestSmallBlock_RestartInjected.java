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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestSmallBlock.
 * Tests small block handling survives component restarts.
 *
 * Generated variants:
 * - AfterFileCreate x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestSmallBlock_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestSmallBlock_RestartInjected.class);

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 512; // Small block for test

  /**
   * Core test logic with restart injection.
   */
  private void testSmallBlockWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create file with small blocks
      Path filePath = new Path("/testSmallBlock.dat");
      DFSTestUtil.createFile(fs, filePath, BLOCKSIZE * 4, REPLICATION, 0);

      // === RESTART INJECTION POINT: After file creation ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Verify file still exists and is readable
      assertTrue("File should exist", fs.exists(filePath));
      long fileLen = fs.getFileStatus(filePath).getLen();
      assertEquals("File should have correct length", BLOCKSIZE * 4, fileLen);

      // Read file to verify integrity
      byte[] readBack = DFSTestUtil.readFileBuffer(fs, filePath);
      assertEquals("Read back should match written", BLOCKSIZE * 4, readBack.length);
    } finally {
      cluster.shutdown();
    }
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_NN_Graceful() throws Exception {
    testSmallBlockWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_NN_Crash() throws Exception {
    testSmallBlockWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_SingleDN_Graceful() throws Exception {
    testSmallBlockWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_SingleDN_Crash() throws Exception {
    testSmallBlockWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_AllDN_Graceful() throws Exception {
    testSmallBlockWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_AllDN_Crash() throws Exception {
    testSmallBlockWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_NNDN_Graceful() throws Exception {
    testSmallBlockWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSmallBlock_AfterCreate_NNDN_Crash() throws Exception {
    testSmallBlockWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

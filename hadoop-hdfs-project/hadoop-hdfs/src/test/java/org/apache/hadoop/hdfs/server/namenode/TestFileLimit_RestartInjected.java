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

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileLimit.
 * Tests file limit operations survive component restarts.
 *
 * Original test with hflush during max blocks per file test
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileLimit_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileLimit_RestartInjected.class);

  /**
   * Core test logic for max blocks per file with restart injection.
   */
  private void testMaxBlocksPerFileLimitWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    // Make a small block size and a low limit
    final long blockSize = 4096;
    final long numBlocks = 2;
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_MAX_BLOCKS_PER_FILE_KEY, numBlocks);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path file = new Path("/testmaxfilelimit_restart.dat");
      HdfsDataOutputStream fout = (HdfsDataOutputStream) fs.create(file);

      // Write maximum number of blocks
      fout.write(new byte[(int)blockSize * (int)numBlocks]);
      fout.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close the file (can't write more due to block limit)
      fout.close();

      // Verify file exists and has the expected size
      assertTrue("File should exist", fs.exists(file));
      long fileLen = fs.getFileStatus(file).getLen();
      assertTrue("File should have data", fileLen == blockSize * numBlocks);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testMaxBlocksPerFileLimit
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_NN_Graceful() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_NN_Crash() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_SingleDN_Graceful() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_SingleDN_Crash() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_AllDN_Graceful() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_AllDN_Crash() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_NNDN_Graceful() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMaxBlocksPerFileLimit_AfterHflush_NNDN_Crash() throws Exception {
    testMaxBlocksPerFileLimitWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

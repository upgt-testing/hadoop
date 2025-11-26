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

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestRenameWhileOpen.
 * Tests rename operations on open files survive component restarts.
 *
 * Original tests with hflush at multiple points in file rename scenarios
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestRenameWhileOpen_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestRenameWhileOpen_RestartInjected.class);
  private static final long BLOCK_SIZE = 1024;
  private static final int BUF_SIZE = 512;

  /**
   * Core test logic for rename while open with restart injection.
   */
  private void testRenameWhileOpenWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create and open first file
      Path file1 = new Path("/testRenameWhileOpen1_restart.dat");
      FSDataOutputStream stm1 = TestFileCreation.createFile(fs, file1, 1);

      // Write some bytes and hflush
      byte[] buffer = AppendTestUtil.initBuffer(BUF_SIZE);
      stm1.write(buffer);
      stm1.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Rename file while open
      Path file1renamed = new Path("/testRenameWhileOpen1_renamed_restart.dat");
      fs.rename(file1, file1renamed);
      assertTrue("File should exist after rename", fs.exists(file1renamed));

      // Write more and close
      stm1.write(buffer);
      stm1.close();

      // Verify renamed file
      long fileLen = fs.getFileStatus(file1renamed).getLen();
      assertTrue("Renamed file should have all data", fileLen == BUF_SIZE * 2);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testRenameWhileOpen
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_NN_Graceful() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_NN_Crash() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_SingleDN_Graceful() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_SingleDN_Crash() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_AllDN_Graceful() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_AllDN_Crash() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_NNDN_Graceful() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWhileOpen_AfterHflush_NNDN_Crash() throws Exception {
    testRenameWhileOpenWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

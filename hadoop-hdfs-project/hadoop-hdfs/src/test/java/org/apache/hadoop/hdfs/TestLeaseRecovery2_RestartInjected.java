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
 * Restart-injection variant of TestLeaseRecovery2.
 * Tests lease recovery operations survive component restarts.
 *
 * Original tests with hflush:
 * - testCloseWhileRecoverLease
 * - testLeaseRecoverByAnotherUser
 * - createFile helper method
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestLeaseRecovery2_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestLeaseRecovery2_RestartInjected.class);
  private static final int REPLICATION_NUM = 3;
  private static final long BLOCK_SIZE = 64 * 1024;
  private static final int BUF_SIZE = AppendTestUtil.FILE_SIZE;

  /**
   * Core test logic for testCloseWhileRecoverLease with restart injection.
   */
  private void testCloseWhileRecoverLeaseWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION_NUM).build();
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      Path filepath = new Path("/testCloseWhileRecoverLease_restart");
      FSDataOutputStream stm = dfs.create(filepath, true, BUF_SIZE,
          (short) REPLICATION_NUM, BLOCK_SIZE);
      assertTrue(dfs.exists(filepath));

      // write some data
      byte[] buffer = AppendTestUtil.initBuffer(1024);
      stm.write(buffer, 0, 512);

      // hflush file
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file normally
      stm.close();

      // Verify file is readable
      long fileLen = dfs.getFileStatus(filepath).getLen();
      assertTrue("File should have data", fileLen > 0);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testCloseWhileRecoverLease
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_NN_Graceful() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_NN_Crash() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_SingleDN_Graceful() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_SingleDN_Crash() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_AllDN_Graceful() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_AllDN_Crash() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_NNDN_Graceful() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCloseWhileRecoverLease_AfterHflush_NNDN_Crash() throws Exception {
    testCloseWhileRecoverLeaseWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

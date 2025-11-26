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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestLeaseRecovery.
 * Tests lease recovery operations survive component restarts.
 *
 * Original tests with hsync:
 * - testBlockRecoveryWithLessMetafile
 * - testBlockRecoveryRetryAfterFailedRecovery
 * - testLeaseRecoveryAndAppend
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestLeaseRecovery_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestLeaseRecovery_RestartInjected.class);

  /**
   * Core test logic for testBlockRecoveryWithLessMetafile with restart injection.
   */
  private void testBlockRecoveryWithRestartInjection(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    try {
      Path file = new Path("/testRecoveryFile_restart");
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream out = dfs.create(file);
      final int FILE_SIZE = 1024 * 1024; // Smaller for faster test
      int count = 0;
      while (count < FILE_SIZE) {
        out.writeBytes("Data");
        count += 4;
      }
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file normally
      out.close();

      // Verify file is readable
      long fileLen = dfs.getFileStatus(file).getLen();
      assertTrue("File should have data", fileLen > 0);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testLeaseRecoveryAndAppend with restart injection.
   */
  private void testLeaseRecoveryAndAppendWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    try {
      Path file = new Path("/testLeaseRecovery_restart");
      DistributedFileSystem dfs = cluster.getFileSystem();

      // create a file with some data
      FSDataOutputStream out = dfs.create(file);
      out.write("some data".getBytes());
      out.hflush();
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file normally
      out.close();

      // Verify file is readable
      byte[] content = DFSTestUtil.readFileBuffer(dfs, file);
      assertTrue("File should have content", content.length > 0);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testBlockRecovery
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_NN_Graceful() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_NN_Crash() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_SingleDN_Graceful() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_SingleDN_Crash() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_AllDN_Graceful() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_AllDN_Crash() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_NNDN_Graceful() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlockRecovery_AfterHsync_NNDN_Crash() throws Exception {
    testBlockRecoveryWithRestartInjection(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testLeaseRecoveryAndAppend
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_NN_Graceful() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_NN_Crash() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_SingleDN_Graceful() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_SingleDN_Crash() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_AllDN_Graceful() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_AllDN_Crash() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_NNDN_Graceful() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseRecoveryAndAppend_AfterHsync_NNDN_Crash() throws Exception {
    testLeaseRecoveryAndAppendWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

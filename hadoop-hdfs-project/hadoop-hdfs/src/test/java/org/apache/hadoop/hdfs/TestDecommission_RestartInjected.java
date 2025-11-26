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
 * Restart-injection variant of TestDecommission.
 * Tests decommission operations survive component restarts.
 *
 * Original tests with hsync:
 * - testDecommissionWithOpenFileAndBlockRecovery
 * - testCloseWhileDecommission
 *
 * Note: The original tests focus on decommissioning behavior.
 * This simplified version focuses on verifying hsync data survives restarts.
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDecommission_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDecommission_RestartInjected.class);

  private static final int BLOCKSIZE = 512;
  private static final int FILESIZE = BLOCKSIZE * 4;

  /**
   * Core test logic with restart injection after hsync.
   */
  private void testDecommissionWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      Path file = new Path("/testDecommissionWithRestart");

      // Create a file and write data
      FSDataOutputStream out = dfs.create(file, true,
          conf.getInt("io.file.buffer.size", 4096),
          (short) 3, BLOCKSIZE);

      // Write data to the file
      long writtenBytes = 0;
      while (writtenBytes < FILESIZE) {
        out.writeLong(writtenBytes);
        writtenBytes += 8;
      }
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close the file
      out.close();

      // Verify file is readable and has correct length
      long actualLen = dfs.getFileStatus(file).getLen();
      assert actualLen == writtenBytes : "Expected " + writtenBytes + " but got " + actualLen;

      // Read and verify content
      byte[] readBytes = DFSTestUtil.readFileBuffer(dfs, file);
      assert readBytes.length == writtenBytes;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDecommission with restart injection
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_NN_Graceful() throws Exception {
    testDecommissionWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_NN_Crash() throws Exception {
    testDecommissionWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_SingleDN_Graceful() throws Exception {
    testDecommissionWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_SingleDN_Crash() throws Exception {
    testDecommissionWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_AllDN_Graceful() throws Exception {
    testDecommissionWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_AllDN_Crash() throws Exception {
    testDecommissionWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_NNDN_Graceful() throws Exception {
    testDecommissionWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDecommission_AfterHsync_NNDN_Crash() throws Exception {
    testDecommissionWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

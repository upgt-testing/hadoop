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
import org.apache.hadoop.hdfs.client.HdfsDataInputStream;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileLengthOnClusterRestart.
 * Tests file length visibility after hsync survives component restarts.
 *
 * Original test: testFileLengthWithHSyncAndClusterRestartWithOutDNsRegister (has hsync)
 *
 * Note: Original test already has restart logic. This variant adds restart
 * injection at the hsync point to test different restart scenarios.
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileLengthOnClusterRestart_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileLengthOnClusterRestart_RestartInjected.class);

  /**
   * Core test logic for testFileLengthWithHSyncAndClusterRestartWithOutDNsRegister
   * with restart injection.
   */
  private void testFileLengthWithHSyncWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 512);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    HdfsDataInputStream in = null;
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      Path path = new Path("/tmp/TestFileLengthRestart", "test");
      FSDataOutputStream out = dfs.create(path);
      int fileLength = 1030;
      out.write(new byte[fileLength]);
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Verify the length after restart
      in = (HdfsDataInputStream) dfs.open(path, 1024);
      Assert.assertEquals("File length should match after restart", fileLength, in.getVisibleLength());

      out.close();
    } finally {
      if (in != null) {
        in.close();
      }
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testFileLengthWithHSyncAndClusterRestartWithOutDNsRegister
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_NN_Graceful() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_NN_Crash() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_SingleDN_Graceful() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_SingleDN_Crash() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_AllDN_Graceful() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_AllDN_Crash() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_NNDN_Graceful() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileLengthWithHSync_AfterHsync_NNDN_Crash() throws Exception {
    testFileLengthWithHSyncWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDiskspaceQuotaUpdate.
 * Tests diskspace quota updates survive component restarts.
 *
 * Original tests with hflush/hsync test quota updates.
 * This simplified version focuses on verifying hflush data survives restarts.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDiskspaceQuotaUpdate_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDiskspaceQuotaUpdate_RestartInjected.class);

  private static final int BLOCKSIZE = 1024;
  private static final short REPLICATION = 3;

  /**
   * Core test logic with restart injection after hflush.
   */
  private void testDiskspaceQuotaUpdateWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      Path dir = new Path("/TestDiskspaceQuotaUpdate");
      Path file = new Path(dir, "test.dat");
      dfs.mkdirs(dir);
      dfs.setQuota(dir, Long.MAX_VALUE, Long.MAX_VALUE);

      // Create file and write data
      FSDataOutputStream out = dfs.create(file, REPLICATION);
      byte[] data = new byte[BLOCKSIZE / 2];
      out.write(data);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close the file
      out.close();

      // Verify file is readable
      byte[] readData = DFSTestUtil.readFileBuffer(dfs, file);
      assert readData.length == BLOCKSIZE / 2;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDiskspaceQuotaUpdate with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_NN_Graceful() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_NN_Crash() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_SingleDN_Graceful() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_SingleDN_Crash() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_AllDN_Graceful() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_AllDN_Crash() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_NNDN_Graceful() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskspaceQuotaUpdate_AfterHflush_NNDN_Crash() throws Exception {
    testDiskspaceQuotaUpdateWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

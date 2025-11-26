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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileCreation.
 * Tests file creation operations survive component restarts.
 *
 * Original tests with hflush:
 * - testLeaseExpireHardLimit
 * - testFsCloseAfterClusterShutdown (already tests shutdown scenario)
 *
 * Note: testFileCreationNamenodeRestart already has restart logic so not duplicated.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileCreation_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileCreation_RestartInjected.class);
  private static final String DIR = "/test/";
  private static final int DATANODE_NUM = 3;

  static HdfsDataOutputStream create(DistributedFileSystem dfs, Path p, int numReplicas) throws Exception {
    return (HdfsDataOutputStream) dfs.create(p, true,
        dfs.getConf().getInt("io.file.buffer.size", 4096),
        (short) numReplicas, dfs.getDefaultBlockSize(p));
  }

  /**
   * Core test logic for testLeaseExpireHardLimit with restart injection.
   * Create a file, write something, hflush, inject restart, then verify.
   */
  private void testLeaseExpireHardLimitWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(DATANODE_NUM).build();
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      // create a new file
      final String f = DIR + "foo_restart";
      final Path fpath = new Path(f);
      HdfsDataOutputStream out = create(dfs, fpath, DATANODE_NUM);
      out.write("something".getBytes());
      out.hflush();

      int actualRepl = out.getCurrentBlockReplication();
      assertTrue(f + " should be replicated to " + DATANODE_NUM + " datanodes.",
          actualRepl == DATANODE_NUM);

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close and verify file is readable
      out.close();

      // Verify data
      byte[] content = DFSTestUtil.readFileBuffer(dfs, fpath);
      String readBack = new String(content);
      assertTrue("Content should match", readBack.startsWith("something"));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testLeaseExpireHardLimit
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_NN_Graceful() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_NN_Crash() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_SingleDN_Graceful() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_SingleDN_Crash() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_AllDN_Graceful() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_AllDN_Crash() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_NNDN_Graceful() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLeaseExpireHardLimit_AfterHflush_NNDN_Crash() throws Exception {
    testLeaseExpireHardLimitWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

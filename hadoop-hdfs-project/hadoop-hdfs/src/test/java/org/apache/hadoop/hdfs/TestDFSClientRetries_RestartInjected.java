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

import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDFSClientRetries.
 * Tests DFSClient retry mechanisms survive component restarts.
 *
 * Original test: testNamenodeRestart (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDFSClientRetries_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSClientRetries_RestartInjected.class);

  /**
   * Core test logic for namenode restart with additional restart injection.
   */
  private void testNamenodeRestartWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setBoolean(HdfsClientConfigKeys.Retry.POLICY_ENABLED_KEY, true);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_SAFEMODE_MIN_DATANODES_KEY, 1);
    conf.setInt(MiniDFSCluster.DFS_NAMENODE_SAFEMODE_EXTENSION_TESTING_KEY, 5000);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    final Path dir = new Path("/testNamenodeRestart");
    final short numDatanodes = 3;
    MiniDFSCluster cluster = null;

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .numDataNodes(numDatanodes)
          .build();
      cluster.waitActive();
      final DistributedFileSystem dfs = cluster.getFileSystem();

      // create a file
      final long length = 1L << 20;
      final Path file1 = new Path(dir, "foo");
      DFSTestUtil.createFile(dfs, file1, length, numDatanodes, 20120406L);

      // get file status
      final FileStatus s1 = dfs.getFileStatus(file1);
      assertEquals(length, s1.getLen());

      // create file4, write some data but not close
      final Path file4 = new Path(dir, "file4");
      final FSDataOutputStream out4 = dfs.create(file4, false, 4096,
          dfs.getDefaultReplication(file4), 1024L, null);
      final byte[] bytes = new byte[1000];
      new Random().nextBytes(bytes);
      out4.write(bytes);
      out4.write(bytes);
      out4.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // write more data and close
      out4.write(bytes);
      out4.write(bytes);
      out4.close();

      // Verify the file was written correctly
      FileStatus s4 = dfs.getFileStatus(file4);
      assertTrue("File4 should have content", s4.getLen() > 0);

    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testNamenodeRestart with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_NN_Graceful() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_NN_Crash() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_SingleDN_Graceful() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_SingleDN_Crash() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_AllDN_Graceful() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_AllDN_Crash() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_NNDN_Graceful() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testNamenodeRestart_AfterHflush_NNDN_Crash() throws Exception {
    testNamenodeRestartWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

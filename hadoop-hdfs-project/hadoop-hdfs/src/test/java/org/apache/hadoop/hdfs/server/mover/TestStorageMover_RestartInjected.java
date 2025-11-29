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
package org.apache.hadoop.hdfs.server.mover;

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
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
 * Restart-injection variant of TestStorageMover.
 * Tests storage mover operations survive component restarts.
 *
 * Original test with hsync during storage mover operations
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestStorageMover_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestStorageMover_RestartInjected.class);

  private static final int BLOCK_SIZE = 1024;

  /**
   * Core test logic for storage mover with restart injection.
   */
  private void testStorageMoverWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem dfs = cluster.getFileSystem();

      Path fooDir = new Path("/foo");
      dfs.mkdirs(fooDir);

      // Create a file and append with hsync
      Path barFile = new Path(fooDir, "bar");
      DFSTestUtil.createFile(dfs, barFile, BLOCK_SIZE, (short) 1, 0L);
      FSDataOutputStream out = dfs.append(barFile);
      out.writeBytes("hello, ");
      ((DFSOutputStream) out.getWrappedStream()).hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Continue writing and close
      out.writeBytes("world!");
      out.close();

      // Verify file
      assertTrue("File should exist", dfs.exists(barFile));
      long fileLen = dfs.getFileStatus(barFile).getLen();
      assertTrue("File should have all data", fileLen > BLOCK_SIZE);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testStorageMover
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_NN_Graceful() throws Exception {
    testStorageMoverWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_NN_Crash() throws Exception {
    testStorageMoverWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_SingleDN_Graceful() throws Exception {
    testStorageMoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_SingleDN_Crash() throws Exception {
    testStorageMoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_AllDN_Graceful() throws Exception {
    testStorageMoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_AllDN_Crash() throws Exception {
    testStorageMoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_NNDN_Graceful() throws Exception {
    testStorageMoverWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageMover_AfterHsync_NNDN_Crash() throws Exception {
    testStorageMoverWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

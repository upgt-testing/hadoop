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
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestRenameWithSnapshots.
 * Tests rename with snapshots operations survive component restarts.
 *
 * Original test with hsync during rename with snapshots
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestRenameWithSnapshots_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestRenameWithSnapshots_RestartInjected.class);

  private static final short REPL = 3;
  private static final int BLOCKSIZE = 1024;
  private static final long SEED = 0;

  /**
   * Core test logic for rename with snapshots with restart injection.
   */
  private void testRenameWithSnapshotsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPL).build();
    try {
      cluster.waitActive();
      DistributedFileSystem hdfs = cluster.getFileSystem();

      // Setup
      Path test = new Path("/test");
      Path foo = new Path(test, "foo");
      Path bar = new Path(foo, "bar");
      DFSTestUtil.createFile(hdfs, bar, BLOCKSIZE, REPL, SEED);

      // Allow snapshots
      hdfs.allowSnapshot(test);
      SnapshotTestHelper.createSnapshot(hdfs, test, "s0");

      // Rename bar -> bar2
      Path bar2 = new Path(foo, "bar2");
      hdfs.rename(bar, bar2);

      // Append with hsync
      FSDataOutputStream out = hdfs.append(bar2);
      out.writeByte(0);
      ((DFSOutputStream) out.getWrappedStream()).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, hdfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file
      out.close();

      // Verify
      assertTrue("File bar2 should exist", hdfs.exists(bar2));
      assertTrue("Snapshot should exist", hdfs.exists(new Path(test, ".snapshot/s0")));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testRenameWithSnapshots
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_NN_Graceful() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_NN_Crash() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_SingleDN_Graceful() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_SingleDN_Crash() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_AllDN_Graceful() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_AllDN_Crash() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_NNDN_Graceful() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRenameWithSnapshots_AfterHsync_NNDN_Crash() throws Exception {
    testRenameWithSnapshotsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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
 * Restart-injection variant of TestOpenFilesWithSnapshot.
 * Tests open files with snapshots survive component restarts.
 *
 * Original test with hsync during open file snapshot operations
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestOpenFilesWithSnapshot_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestOpenFilesWithSnapshot_RestartInjected.class);

  private static final short REPLICATION = 3;

  /**
   * Core test logic for open files with snapshot and restart injection.
   */
  private void testOpenFilesWithSnapshotWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_SNAPSHOT_CAPTURE_OPENFILES, true);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path path = new Path("/test");
      fs.mkdirs(path);
      fs.allowSnapshot(path);

      // Create initial files
      DFSTestUtil.createFile(fs, new Path("/test/test1"), 100, (short) 2, 100024L);
      DFSTestUtil.createFile(fs, new Path("/test/test2"), 100, (short) 2, 100024L);

      // Create and write to file without closing
      Path file = new Path("/test/test/test2");
      FSDataOutputStream out = fs.create(file);
      for (int i = 0; i < 2; i++) {
        long count = 0;
        while (count < 1048576) {
          out.writeBytes("hell");
          count += 4;
        }
      }
      ((DFSOutputStream) out.getWrappedStream()).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Create snapshot
      fs.createSnapshot(path, "s1");

      // Close file
      out.close();

      // Verify snapshot exists
      assertTrue("Snapshot should exist", fs.exists(new Path("/test/.snapshot/s1")));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testOpenFilesWithSnapshot
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_NN_Graceful() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_NN_Crash() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_SingleDN_Graceful() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_SingleDN_Crash() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_AllDN_Graceful() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_AllDN_Crash() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_NNDN_Graceful() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testOpenFilesWithSnapshot_AfterHsync_NNDN_Crash() throws Exception {
    testOpenFilesWithSnapshotWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

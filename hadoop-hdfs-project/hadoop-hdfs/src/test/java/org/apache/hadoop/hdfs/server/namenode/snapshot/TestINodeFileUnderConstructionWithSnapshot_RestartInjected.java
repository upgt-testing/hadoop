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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestINodeFileUnderConstructionWithSnapshot.
 * Tests snapshot with file under construction survives component restarts.
 *
 * Original test with hsync during file appending with snapshots
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestINodeFileUnderConstructionWithSnapshot_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestINodeFileUnderConstructionWithSnapshot_RestartInjected.class);

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final int BLOCKSIZE = 1024;

  /**
   * Core test logic for snapshot while appending with restart injection.
   */
  private void testSnapshotWhileAppendingWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem hdfs = cluster.getFileSystem();

      final Path dir = new Path("/TestSnapshot");
      hdfs.mkdirs(dir);

      Path file = new Path(dir, "file");
      DFSTestUtil.createFile(hdfs, file, BLOCKSIZE, REPLICATION, seed);

      // append without closing stream
      HdfsDataOutputStream out = (HdfsDataOutputStream) hdfs.append(file);
      byte[] toAppend = new byte[BLOCKSIZE];
      out.write(toAppend);
      out.hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, hdfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Create snapshot and close
      SnapshotTestHelper.createSnapshot(hdfs, dir, "s0");
      out.close();

      // Verify file size
      long fileLen = hdfs.getFileStatus(file).getLen();
      assertTrue("File should have all data", fileLen == BLOCKSIZE * 2);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testSnapshotWhileAppending
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_NN_Graceful() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_NN_Crash() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_SingleDN_Graceful() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_SingleDN_Crash() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_AllDN_Graceful() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_AllDN_Crash() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_NNDN_Graceful() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotWhileAppending_AfterHsync_NNDN_Crash() throws Exception {
    testSnapshotWhileAppendingWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

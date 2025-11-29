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
 * Restart-injection variant of TestSnapshotDiffReport.
 * Tests snapshot diff report operations survive component restarts.
 *
 * Original test with hsync during snapshot diff report with open files
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestSnapshotDiffReport_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotDiffReport_RestartInjected.class);

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  /**
   * Core test logic for snapshot diff report with restart injection.
   */
  private void testSnapshotDiffReportWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem hdfs = cluster.getFileSystem();

      // Setup directory structure
      Path level0A = new Path("/level_0_A");
      hdfs.mkdirs(level0A);
      hdfs.allowSnapshot(level0A);

      // Create file
      Path flumeFile = new Path(level0A, "flume.log");
      DFSTestUtil.createFile(hdfs, flumeFile, BLOCKSIZE, BLOCKSIZE * 4, BLOCKSIZE, REPLICATION, 0L);

      // Append and hsync
      FSDataOutputStream flumeOutputStream = hdfs.append(flumeFile);
      byte[] buf = new byte[BLOCKSIZE];
      flumeOutputStream.write(buf);
      ((HdfsDataOutputStream) flumeOutputStream).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // Create first snapshot
      hdfs.createSnapshot(level0A, "flume_snap_1");

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, hdfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close stream
      flumeOutputStream.close();

      // Verify file exists
      assertTrue("File should exist", hdfs.exists(flumeFile));
      assertTrue("Snapshot should exist", hdfs.exists(new Path(level0A, ".snapshot/flume_snap_1")));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testSnapshotDiffReport
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_NN_Graceful() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_NN_Crash() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_SingleDN_Graceful() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_SingleDN_Crash() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_AllDN_Graceful() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_AllDN_Crash() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_NNDN_Graceful() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSnapshotDiffReport_AfterHsync_NNDN_Crash() throws Exception {
    testSnapshotDiffReportWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
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
 * Restart-injection variant of TestUpdatePipelineWithSnapshots.
 * Tests update pipeline with snapshots operations survive component restarts.
 *
 * Original test with hflush during update pipeline with snapshots
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestUpdatePipelineWithSnapshots_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestUpdatePipelineWithSnapshots_RestartInjected.class);

  /**
   * Core test logic for update pipeline with snapshots with restart injection.
   */
  private void testUpdatePipelineWithSnapshotsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();
      DistributedFileSystem dfs = cluster.getFileSystem();

      // Create file and hflush
      Path file = new Path("/test-file");
      DFSOutputStream out = (DFSOutputStream) (fs.create(file).getWrappedStream());
      out.write(1);
      out.hflush();

      // Create snapshot
      dfs.allowSnapshot(new Path("/"));
      SnapshotTestHelper.createSnapshot(dfs, new Path("/"), "s1");

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more and close
      out.write(2);
      out.close();

      // Verify
      assertTrue("File should exist", fs.exists(file));
      assertTrue("Snapshot should exist", fs.exists(new Path("/.snapshot/s1")));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testUpdatePipelineWithSnapshots
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_NN_Graceful() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_NN_Crash() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_SingleDN_Graceful() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_SingleDN_Crash() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_AllDN_Graceful() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_AllDN_Crash() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_NNDN_Graceful() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUpdatePipelineWithSnapshots_AfterHflush_NNDN_Crash() throws Exception {
    testUpdatePipelineWithSnapshotsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

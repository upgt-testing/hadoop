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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Random;

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
 * Restart-injection variant of TestFSImageWithSnapshot.
 * Tests FSImage save/load with snapshot survives component restarts.
 *
 * Original test: testSaveLoadImageWithAppending - hsync during append
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFSImageWithSnapshot_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFSImageWithSnapshot_RestartInjected.class);

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;
  private static final long SEED = 0;

  /**
   * Core test logic for FSImage with snapshot and restart injection.
   */
  private void testFSImageWithSnapshotWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Setup directory with snapshot
      Path dir = new Path("/testFSImageSnapshot");
      fs.mkdirs(dir);
      fs.allowSnapshot(dir);

      // Create initial file
      Path file = new Path(dir, "file");
      DFSTestUtil.createFile(fs, file, BLOCKSIZE, REPLICATION, SEED);

      // Create snapshot
      fs.createSnapshot(dir, "s0");

      // Append to file with hsync
      HdfsDataOutputStream out = (HdfsDataOutputStream) fs.append(file);
      byte[] appendContent = new byte[BLOCKSIZE];
      new Random().nextBytes(appendContent);
      out.write(appendContent);
      out.hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close the stream
      out.close();

      // Create another snapshot
      fs.createSnapshot(dir, "s1");

      // Verify
      assertTrue("File should exist", fs.exists(file));
      assertTrue("Snapshot s0 should exist", fs.exists(new Path(dir, ".snapshot/s0")));
      assertTrue("Snapshot s1 should exist", fs.exists(new Path(dir, ".snapshot/s1")));
      long fileLen = fs.getFileStatus(file).getLen();
      assertEquals("File should have all data", BLOCKSIZE * 2, fileLen);

      // Verify snapshot has original size
      Path snapshotFile = new Path(dir, ".snapshot/s0/file");
      long snapshotLen = fs.getFileStatus(snapshotFile).getLen();
      assertEquals("Snapshot file should have original size", BLOCKSIZE, snapshotLen);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testFSImageWithSnapshot
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_NN_Graceful() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_NN_Crash() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_SingleDN_Graceful() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_SingleDN_Crash() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_AllDN_Graceful() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_AllDN_Crash() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_NNDN_Graceful() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFSImageWithSnapshot_AfterHsync_NNDN_Crash() throws Exception {
    testFSImageWithSnapshotWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

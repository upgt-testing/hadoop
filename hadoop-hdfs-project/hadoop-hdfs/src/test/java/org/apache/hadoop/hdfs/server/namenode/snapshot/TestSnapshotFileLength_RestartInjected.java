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
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestSnapshotFileLength.
 * Tests snapshot file length operations survive component restarts.
 *
 * Original test with hflush during snapshot file length operations
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestSnapshotFileLength_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotFileLength_RestartInjected.class);

  private static final long SEED = 0;
  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  /**
   * Core test logic for snapshot file length with restart injection.
   */
  private void testSnapshotFileLengthWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_MIN_BLOCK_SIZE_KEY, BLOCKSIZE);
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem hdfs = cluster.getFileSystem();

      Path sub = new Path("/TestSnapshotFileLength/sub1");
      hdfs.mkdirs(sub);

      // Create a file
      Path file1 = new Path(sub, "file1");
      int origLen = BLOCKSIZE + 1;
      DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, 0, BLOCKSIZE, REPLICATION, SEED);
      DFSTestUtil.appendFile(hdfs, file1, origLen);

      // Create snapshot
      hdfs.allowSnapshot(sub);
      hdfs.createSnapshot(sub, "snapshot1");

      // Append to file with hflush
      FSDataOutputStream out = hdfs.append(file1);
      int toAppend = BLOCKSIZE;
      AppendTestUtil.write(out, 0, toAppend);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, hdfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file
      out.close();

      // Verify file length
      long fileLen = hdfs.getFileStatus(file1).getLen();
      assertTrue("File should have all data", fileLen == origLen + toAppend);

      // Verify snapshot length is preserved
      Path file1snap1 = SnapshotTestHelper.getSnapshotPath(sub, "snapshot1", "file1");
      long snapLen = hdfs.getFileStatus(file1snap1).getLen();
      assertTrue("Snapshot should have original length", snapLen == origLen);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testSnapshotFileLength
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_NN_Graceful() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_NN_Crash() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_SingleDN_Graceful() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_SingleDN_Crash() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_AllDN_Graceful() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_AllDN_Crash() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_NNDN_Graceful() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 300000)
  public void testSnapshotFileLength_AfterHflush_NNDN_Crash() throws Exception {
    testSnapshotFileLengthWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

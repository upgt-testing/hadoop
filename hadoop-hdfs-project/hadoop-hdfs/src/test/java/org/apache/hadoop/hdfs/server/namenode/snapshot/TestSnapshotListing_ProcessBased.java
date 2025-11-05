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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSnapshotListing}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestSnapshotListing Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSnapshotListing_ProcessBased extends ProcessBasedUpgradeTestBase {

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final long BLOCKSIZE = 1024;

  private final Path dir = new Path("/test.snapshot/dir");

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_MKDIR",
      "AFTER_ALLOW_SNAPSHOT",
      "AFTER_FIRST_SNAPSHOT",
      "AFTER_SECOND_SNAPSHOT",
      "AFTER_THIRD_SNAPSHOT",
      "AFTER_ALL_SNAPSHOTS_CREATED",
      "AFTER_FIRST_DELETE",
      "AFTER_SECOND_DELETE",
      "BEFORE_FINAL_DELETE"
    );
  }

  /**
   * Test listing snapshots under a snapshottable directory
   */
  @Test (timeout=60000)
  public void testListSnapshots() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs.mkdirs(dir);
    checkpoint("AFTER_MKDIR");

    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    // special case: snapshots of root
    stats = fs.listStatus(new Path("/.snapshot"));
    // should be 0 since root's snapshot quota is 0
    assertEquals(0, stats.length);

    // list before set dir as snapshottable
    try {
      stats = fs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    // list before creating snapshots
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.allowSnapshot(dir);
    checkpoint("AFTER_ALLOW_SNAPSHOT");

    stats = fs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    // list while creating snapshots
    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      dfs.createSnapshot(dir, "s_" + sNum);

      // Insert checkpoints after specific snapshots
      if (sNum == 0) {
        checkpoint("AFTER_FIRST_SNAPSHOT");
      } else if (sNum == 1) {
        checkpoint("AFTER_SECOND_SNAPSHOT");
      } else if (sNum == 2) {
        checkpoint("AFTER_THIRD_SNAPSHOT");
      }

      stats = fs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    checkpoint("AFTER_ALL_SNAPSHOTS_CREATED");

    // list while deleting snapshots
    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      dfs.deleteSnapshot(dir, "s_" + sNum);

      // Insert checkpoints after specific deletions
      if (sNum == snapshotNum - 1) {
        checkpoint("AFTER_FIRST_DELETE");
      } else if (sNum == snapshotNum - 2) {
        checkpoint("AFTER_SECOND_DELETE");
      }

      stats = fs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    checkpoint("BEFORE_FINAL_DELETE");

    // remove the last snapshot
    dfs.deleteSnapshot(dir, "s_0");
    stats = fs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }
}

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

import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
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
 * ProcessBasedMiniDFSCluster version of {@link TestSnapRootDescendantDiff}.
 *
 * Tests snapshot diff report for the snapshot root descendant directory
 * with parameterized upgrade checkpoints.
 *
 * @see TestSnapRootDescendantDiff Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSnapRootDescendantDiff_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final long SEED = 0;
  private static final short REPLICATION = 3;
  private static final short REPLICATION_1 = 2;
  private static final long BLOCKSIZE = 1024;

  private final Path dir = new Path("/TestSnapshot");
  private final Path sub1 = new Path(dir, "sub1");
  private final HashMap<Path, Integer> snapshotNumberMap = new HashMap<Path, Integer>();

  private DistributedFileSystem hdfs;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_SUBSUB1_CREATE",
      "AFTER_SUBSUBSUB1_CREATE",
      "AFTER_FIRST_SNAPSHOT",
      "AFTER_SECOND_MODIFY",
      "AFTER_SECOND_SNAPSHOT",
      "BEFORE_DIFF_REPORT",
      UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  @Test
  public void testNonSnapRootDiffReport() throws Exception {
    // Configure snapshot settings
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_SNAPSHOT_CAPTURE_OPENFILES, true);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_ACCESSTIME_PRECISION_KEY, 1);
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_SNAPSHOT_SKIP_CAPTURE_ACCESSTIME_ONLY_CHANGE,
        true);
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_SNAPSHOT_DIFF_ALLOW_SNAP_ROOT_DESCENDANT,
        false);

    // Build cluster with 3 DataNodes
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();
    cluster.waitClusterUp();
    hdfs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create directory structure: /TestSnapshot/sub1/subsub1/subsubsub1
    Path subsub1 = new Path(getSnapRootDir(), "subsub1");
    hdfs.mkdirs(subsub1);
    checkpoint("AFTER_SUBSUB1_CREATE");

    Path subsubsub1 = new Path(subsub1, "subsubsub1");
    hdfs.mkdirs(subsubsub1);
    checkpoint("AFTER_SUBSUBSUB1_CREATE");

    // First modify and snapshot
    modifyAndCreateSnapshot(getSnapRootDir(), new Path[]{getSnapRootDir()});
    checkpoint("AFTER_FIRST_SNAPSHOT");

    // Second modify and snapshot
    modifyAndCreateSnapshot(subsubsub1, new Path[]{getSnapRootDir()});
    checkpoint("AFTER_SECOND_MODIFY");
    checkpoint("AFTER_SECOND_SNAPSHOT");

    checkpoint("BEFORE_DIFF_REPORT");

    // Try to get snapshot diff for non-snapshottable directory
    try {
      hdfs.getSnapshotDiffReport(subsub1, "s1", "s2");
      fail("Expect exception when getting snapshot diff report: " + subsub1
          + " is not a snapshottable directory.");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + subsub1, e);
    }

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  /**
   * Get the snapshot root directory for testing.
   */
  protected Path getSnapRootDir() {
    return sub1;
  }

  /**
   * Generate a snapshot name with auto-increment number.
   */
  private String genSnapshotName(Path snapshotDir) {
    int sNum = -1;
    if (snapshotNumberMap.containsKey(snapshotDir)) {
      sNum = snapshotNumberMap.get(snapshotDir);
    }
    snapshotNumberMap.put(snapshotDir, ++sNum);
    return "s" + sNum;
  }

  /**
   * Create/modify/delete files under a given directory, also create snapshots
   * of directories.
   */
  protected void modifyAndCreateSnapshot(Path modifyDir, Path[] snapshotDirs)
      throws Exception {
    Path file10 = new Path(modifyDir, "file10");
    Path file11 = new Path(modifyDir, "file11");
    Path file12 = new Path(modifyDir, "file12");
    Path file13 = new Path(modifyDir, "file13");
    Path link13 = new Path(modifyDir, "link13");
    Path file14 = new Path(modifyDir, "file14");
    Path file15 = new Path(modifyDir, "file15");

    DFSTestUtil.createFile(hdfs, file10, BLOCKSIZE, REPLICATION_1, SEED);
    DFSTestUtil.createFile(hdfs, file11, BLOCKSIZE, REPLICATION_1, SEED);
    DFSTestUtil.createFile(hdfs, file12, BLOCKSIZE, REPLICATION_1, SEED);
    DFSTestUtil.createFile(hdfs, file13, BLOCKSIZE, REPLICATION_1, SEED);

    // create link13
    hdfs.createSymlink(file13, link13, false);

    // create snapshot
    for (Path snapshotDir : snapshotDirs) {
      hdfs.allowSnapshot(snapshotDir);
      hdfs.createSnapshot(snapshotDir, genSnapshotName(snapshotDir));
    }

    // delete file11
    hdfs.delete(file11, true);
    // modify file12
    hdfs.setReplication(file12, REPLICATION);
    // modify file13
    hdfs.setReplication(file13, REPLICATION);
    // delete file14
    hdfs.delete(file14, true);
    // create file15
    DFSTestUtil.createFile(hdfs, file15, BLOCKSIZE, REPLICATION, SEED);

    // create snapshot
    for (Path snapshotDir : snapshotDirs) {
      hdfs.createSnapshot(snapshotDir, genSnapshotName(snapshotDir));
    }
  }
}

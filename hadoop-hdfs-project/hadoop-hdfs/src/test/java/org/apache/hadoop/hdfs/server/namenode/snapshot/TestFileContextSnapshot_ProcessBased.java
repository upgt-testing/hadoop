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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.protocol.SnapshotException;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileContextSnapshot}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestFileContextSnapshot Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFileContextSnapshot_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;
  private static final long SEED = 0;

  private final String snapshotRoot = "/snapshot";
  private final Path filePath = new Path(snapshotRoot, "file1");
  private Path snapRootPath;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_SNAPSHOT_DIR_CREATED",
      "AFTER_FILE_CREATED",
      "AFTER_SNAPSHOT_ALLOWED",
      "AFTER_SNAPSHOT_CREATED",
      "AFTER_SNAPSHOT_DELETED",
      "AFTER_SNAPSHOT_RENAMED"
    );
  }

  @Test(timeout = 60000)
  public void testCreateAndDeleteSnapshot() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    snapRootPath = new Path(snapshotRoot);
    dfs.mkdirs(snapRootPath);
    checkpoint("AFTER_SNAPSHOT_DIR_CREATED");

    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    checkpoint("AFTER_FILE_CREATED");

    // disallow snapshot on dir
    dfs.disallowSnapshot(snapRootPath);
    checkpoint("AFTER_SNAPSHOT_DISALLOWED");

    FileContext fileContext = FileContext.getFileContext(testConf);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }

    // allow snapshot on dir
    dfs.allowSnapshot(snapRootPath);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    checkpoint("AFTER_SNAPSHOT_CREATED");

    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
    checkpoint("AFTER_SNAPSHOT_DELETED");
  }

  /**
   * Test FileStatus of snapshot file before/after rename
   */
  @Test(timeout = 60000)
  public void testRenameSnapshot() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    snapRootPath = new Path(snapshotRoot);
    dfs.mkdirs(snapRootPath);
    checkpoint("AFTER_SNAPSHOT_DIR_CREATED");

    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    checkpoint("AFTER_FILE_CREATED");

    dfs.allowSnapshot(snapRootPath);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    FileContext fileContext = FileContext.getFileContext(testConf);

    // Create snapshot for sub1
    Path snapPath1 = fileContext.createSnapshot(snapRootPath, "s1");
    Path ssPath = new Path(snapPath1, filePath.getName());
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    FileStatus statusBeforeRename = dfs.getFileStatus(ssPath);
    checkpoint("AFTER_SNAPSHOT_CREATED");

    // Rename the snapshot
    fileContext.renameSnapshot(snapRootPath, "s1", "s2");
    checkpoint("AFTER_SNAPSHOT_RENAMED");

    // <sub1>/.snapshot/s1/file1 should no longer exist
    assertFalse("Old snapshot still exists after rename!", dfs.exists(ssPath));
    Path snapshotRoot = SnapshotTestHelper.getSnapshotRoot(snapRootPath, "s2");
    ssPath = new Path(snapshotRoot, filePath.getName());

    // Instead, <sub1>/.snapshot/s2/file1 should exist
    assertTrue("Snapshot doesn't exists!", dfs.exists(ssPath));
    FileStatus statusAfterRename = dfs.getFileStatus(ssPath);

    // FileStatus of the snapshot should not change except the path
    assertFalse("Filestatus of the snapshot matches",
        statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals("FileStatus of the snapshot mismatches!",
        statusBeforeRename.toString(), statusAfterRename.toString());
  }
}

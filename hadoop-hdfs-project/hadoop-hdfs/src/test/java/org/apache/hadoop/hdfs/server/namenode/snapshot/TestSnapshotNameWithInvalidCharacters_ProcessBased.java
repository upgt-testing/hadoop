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

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.ipc.RemoteException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSnapshotNameWithInvalidCharacters}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestSnapshotNameWithInvalidCharacters Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSnapshotNameWithInvalidCharacters_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final long SEED = 0;
  private static final short REPLICATION = 1;
  private static final int BLOCKSIZE = 1024;

  private final Path dir1 = new Path("/");
  private final String file1Name = "file1";
  private final String snapshot1 = "a:b:c";
  private final String snapshot2 = "a/b/c";

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_CREATE",
      "AFTER_ALLOW_SNAPSHOT",
      "BEFORE_INVALID_SNAPSHOT_CREATION"
    );
  }

  @Test (timeout = 60000)
  public void TestSnapshotWithInvalidName() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile((DistributedFileSystem) fs, file1, BLOCKSIZE, REPLICATION, SEED);
    checkpoint("AFTER_FILE_CREATE");

    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.allowSnapshot(dir1);
    checkpoint("AFTER_ALLOW_SNAPSHOT");

    checkpoint("BEFORE_INVALID_SNAPSHOT_CREATION");

    try {
      dfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
      // Expected - snapshot name contains invalid character ':'
    }
  }

  @Test(timeout = 60000)
  public void TestSnapshotWithInvalidName1() throws Exception{
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile((DistributedFileSystem) fs, file1, BLOCKSIZE, REPLICATION, SEED);
    checkpoint("AFTER_FILE_CREATE");

    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.allowSnapshot(dir1);
    checkpoint("AFTER_ALLOW_SNAPSHOT");

    checkpoint("BEFORE_INVALID_SNAPSHOT_CREATION");

    try {
      dfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
      // Expected - snapshot name contains invalid character '/'
    }
  }
}

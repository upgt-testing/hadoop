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
import static org.junit.Assert.fail;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.protocol.SnapshotAccessControlException;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDisallowModifyROSnapshot}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests that snapshot paths are read-only and modifications are disallowed.
 *
 * @see TestDisallowModifyROSnapshot Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDisallowModifyROSnapshot_ProcessBased extends ProcessBasedUpgradeTestBase {

  private final Path dir = new Path("/TestSnapshot");
  private final Path sub1 = new Path(dir, "sub1");
  private final Path sub2 = new Path(dir, "sub2");
  private Path objInSnapshot = null;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_DIRS_CREATED",
      "AFTER_SNAPSHOT_CREATED"
    );
  }

  private void setupSnapshotEnv() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path path1 = new Path(sub1, "dir1");
    assertTrue(dfs.mkdirs(path1));
    Path path2 = new Path(sub2, "dir2");
    assertTrue(dfs.mkdirs(path2));
    checkpoint("AFTER_DIRS_CREATED");

    SnapshotTestHelper.createSnapshot(dfs, sub1, "testSnapshot");
    objInSnapshot = SnapshotTestHelper.getSnapshotPath(sub1, "testSnapshot",
        "dir1");
    checkpoint("AFTER_SNAPSHOT_CREATED");
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testSetReplication() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setReplication(objInSnapshot, (short) 1);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testSetPermission() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setPermission(objInSnapshot, new FsPermission("777"));
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testSetOwner() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setOwner(objInSnapshot, "username", "groupname");
  }

  @Test (timeout=60000)
  public void testRename() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;

    try {
      dfs.rename(objInSnapshot, new Path("/invalid/path"));
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      dfs.rename(sub2, objInSnapshot);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      dfs.rename(sub2, objInSnapshot, (Options.Rename) null);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testDelete() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.delete(objInSnapshot, true);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testQuota() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setQuota(objInSnapshot, 100, 100);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testSetTime() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setTimes(objInSnapshot, 100, 100);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testCreate() throws Exception {
    setupSnapshotEnv();
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    dfsclient.create(objInSnapshot.toString(), true);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testAppend() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.append(objInSnapshot, 65535, null);
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testMkdir() throws Exception {
    setupSnapshotEnv();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.mkdirs(objInSnapshot, new FsPermission("777"));
  }

  @Test(timeout=60000, expected = SnapshotAccessControlException.class)
  public void testCreateSymlink() throws Exception {
    setupSnapshotEnv();
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    dfsclient.createSymlink(sub2.toString(), "/TestSnapshot/sub1/.snapshot",
        false);
  }
}

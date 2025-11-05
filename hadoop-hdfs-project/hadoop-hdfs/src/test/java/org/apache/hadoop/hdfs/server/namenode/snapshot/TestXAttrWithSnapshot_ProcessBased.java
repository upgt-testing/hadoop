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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.XAttrSetFlag;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.SnapshotAccessControlException;
import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestXAttrWithSnapshot}.
 *
 * Tests interaction of XAttrs with snapshots using process-based cluster
 * to enable multi-version upgrade scenarios.
 *
 * @see TestXAttrWithSnapshot Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestXAttrWithSnapshot_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static int pathCount = 0;
  private Path path, snapshotPath, snapshotPath2, snapshotPath3;
  private String snapshotName, snapshotName2, snapshotName3;
  private final int SUCCESS = 0;

  // XAttrs
  private static final String name1 = "user.a1";
  private static final byte[] value1 = { 0x31, 0x32, 0x33 };
  private static final byte[] newValue1 = { 0x31, 0x31, 0x31 };
  private static final String name2 = "user.a2";
  private static final byte[] value2 = { 0x37, 0x38, 0x39 };

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_XATTR_SET",
      "AFTER_SNAPSHOT_CREATE",
      "AFTER_XATTR_MODIFY",
      "AFTER_XATTR_REMOVE",
      "BEFORE_VERIFICATION"
    );
  }

  /**
   * Tests modifying xattrs on a directory that has been snapshotted
   */
  @Test (timeout = 120000)
  public void testModifyReadsCurrentState() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Init
    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    hdfs.setXAttr(path, name1, value1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_SET");

    // Verify that current path reflects xattrs, snapshot doesn't
    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 2);
    assertArrayEquals(value1, xattrs.get(name1));
    assertArrayEquals(value2, xattrs.get(name2));

    xattrs = hdfs.getXAttrs(snapshotPath);
    assertEquals(xattrs.size(), 0);

    // Modify each xattr and make sure it's reflected
    hdfs.setXAttr(path, name1, value2, EnumSet.of(XAttrSetFlag.REPLACE));
    checkpoint("AFTER_XATTR_MODIFY");

    xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 2);
    assertArrayEquals(value2, xattrs.get(name1));
    assertArrayEquals(value2, xattrs.get(name2));

    hdfs.setXAttr(path, name2, value1, EnumSet.of(XAttrSetFlag.REPLACE));
    xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 2);
    assertArrayEquals(value2, xattrs.get(name1));
    assertArrayEquals(value1, xattrs.get(name2));

    // Paranoia checks
    xattrs = hdfs.getXAttrs(snapshotPath);
    assertEquals(xattrs.size(), 0);

    hdfs.removeXAttr(path, name1);
    hdfs.removeXAttr(path, name2);
    checkpoint("AFTER_XATTR_REMOVE");

    xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 0);
  }

  @Test
  public void testXattrWithSnapshotAndNNRestart() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Init
    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    checkpoint("AFTER_XATTR_SET");

    hdfs.allowSnapshot(path);
    hdfs.createSnapshot(path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    SnapshotDiffReport report = hdfs.getSnapshotDiffReport(path, snapshotName, "");
    System.out.println(report);
    Assert.assertEquals(0, report.getDiffList().size());

    report = hdfs.getSnapshotDiffReport(path, snapshotName, "");
    System.out.println(report);
    Assert.assertEquals(0, report.getDiffList().size());

    // Save namespace and restart NameNode
    hdfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_ENTER);
    hdfs.saveNamespace();
    hdfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_LEAVE);
    checkpoint("BEFORE_NAMENODE_RESTART");

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    hdfs = (DistributedFileSystem) fs;
    checkpoint("AFTER_NAMENODE_RESTART");

    report = hdfs.getSnapshotDiffReport(path, snapshotName, "");
    System.out.println(report);
    Assert.assertEquals(0, report.getDiffList().size());
  }

  /**
   * Tests removing xattrs on a directory that has been snapshotted
   */
  @Test (timeout = 120000)
  public void testRemoveReadsCurrentState() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Init
    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    hdfs.setXAttr(path, name1, value1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_SET");

    // Verify that current path reflects xattrs, snapshot doesn't
    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 2);
    assertArrayEquals(value1, xattrs.get(name1));
    assertArrayEquals(value2, xattrs.get(name2));

    xattrs = hdfs.getXAttrs(snapshotPath);
    assertEquals(xattrs.size(), 0);

    // Remove xattrs and verify one-by-one
    hdfs.removeXAttr(path, name2);
    checkpoint("AFTER_XATTR_REMOVE");

    xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 1);
    assertArrayEquals(value1, xattrs.get(name1));

    hdfs.removeXAttr(path, name1);
    xattrs = hdfs.getXAttrs(path);
    assertEquals(xattrs.size(), 0);
  }

  /**
   * 1) Save xattrs, then create snapshot. Assert that inode of original and
   * snapshot have same xattrs. 2) Change the original xattrs, assert snapshot
   * still has old xattrs.
   */
  @Test
  public void testXAttrForSnapshotRootAfterChange() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_SET");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    // Both original and snapshot have same XAttrs.
    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    // Original XAttrs have changed, but snapshot still has old XAttrs.
    hdfs.setXAttr(path, name1, newValue1);
    checkpoint("AFTER_XATTR_MODIFY");

    doSnapshotRootChangeAssertions(hdfs, path, snapshotPath);
    restartNameNode(hdfs, false);
    doSnapshotRootChangeAssertions(hdfs, path, snapshotPath);
    restartNameNode(hdfs, true);
    doSnapshotRootChangeAssertions(hdfs, path, snapshotPath);
  }

  private void doSnapshotRootChangeAssertions(DistributedFileSystem hdfs,
      Path path, Path snapshotPath) throws Exception {
    checkpoint("BEFORE_VERIFICATION");

    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(newValue1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));
  }

  /**
   * 1) Save xattrs, then create snapshot. Assert that inode of original and
   * snapshot have same xattrs. 2) Remove some original xattrs, assert snapshot
   * still has old xattrs.
   */
  @Test
  public void testXAttrForSnapshotRootAfterRemove() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_SET");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    // Both original and snapshot have same XAttrs.
    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(xattrs.size(), 2);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    // Original XAttrs have been removed, but snapshot still has old XAttrs.
    hdfs.removeXAttr(path, name1);
    hdfs.removeXAttr(path, name2);
    checkpoint("AFTER_XATTR_REMOVE");

    doSnapshotRootRemovalAssertions(hdfs, path, snapshotPath);
    restartNameNode(hdfs, false);
    doSnapshotRootRemovalAssertions(hdfs, path, snapshotPath);
    restartNameNode(hdfs, true);
    doSnapshotRootRemovalAssertions(hdfs, path, snapshotPath);
  }

  private void doSnapshotRootRemovalAssertions(DistributedFileSystem hdfs,
      Path path, Path snapshotPath) throws Exception {
    checkpoint("BEFORE_VERIFICATION");

    Map<String, byte[]> xattrs = hdfs.getXAttrs(path);
    Assert.assertEquals(0, xattrs.size());

    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(2, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));
  }

  /**
   * Test successive snapshots in between modifications of XAttrs.
   * Also verify that snapshot XAttrs are not altered when a
   * snapshot is deleted.
   */
  @Test
  public void testSuccessiveSnapshotXAttrChanges() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // First snapshot
    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    checkpoint("AFTER_XATTR_SET");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    Map<String, byte[]> xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(1, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));

    // Second snapshot
    hdfs.setXAttr(path, name1, newValue1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_MODIFY");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName2);
    checkpoint("AFTER_SNAPSHOT2_CREATE");

    xattrs = hdfs.getXAttrs(snapshotPath2);
    Assert.assertEquals(2, xattrs.size());
    Assert.assertArrayEquals(newValue1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    // Third snapshot
    hdfs.setXAttr(path, name1, value1);
    hdfs.removeXAttr(path, name2);
    checkpoint("AFTER_XATTR_REMOVE");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName3);
    checkpoint("AFTER_SNAPSHOT3_CREATE");

    xattrs = hdfs.getXAttrs(snapshotPath3);
    Assert.assertEquals(1, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));

    // Check that the first and second snapshots'
    // XAttrs have stayed constant
    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(1, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    xattrs = hdfs.getXAttrs(snapshotPath2);
    Assert.assertEquals(2, xattrs.size());
    Assert.assertArrayEquals(newValue1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));

    // Remove the second snapshot and verify the first and
    // third snapshots' XAttrs have stayed constant
    hdfs.deleteSnapshot(path, snapshotName2);
    checkpoint("AFTER_SNAPSHOT_DELETE");

    xattrs = hdfs.getXAttrs(snapshotPath);
    Assert.assertEquals(1, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    xattrs = hdfs.getXAttrs(snapshotPath3);
    Assert.assertEquals(1, xattrs.size());
    Assert.assertArrayEquals(value1, xattrs.get(name1));

    hdfs.deleteSnapshot(path, snapshotName);
    hdfs.deleteSnapshot(path, snapshotName3);
  }

  /**
   * Assert exception of setting xattr on read-only snapshot.
   */
  @Test
  public void testSetXAttrSnapshotPath() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    exception.expect(SnapshotAccessControlException.class);
    hdfs.setXAttr(snapshotPath, name1, value1);
  }

  /**
   * Assert exception of removing xattr on read-only snapshot.
   */
  @Test
  public void testRemoveXAttrSnapshotPath() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    checkpoint("AFTER_XATTR_SET");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    exception.expect(SnapshotAccessControlException.class);
    hdfs.removeXAttr(snapshotPath, name1);
  }

  /**
   * Test that users can copy a snapshot while preserving its xattrs.
   */
  @Test (timeout = 120000)
  public void testCopySnapshotShouldPreserveXAttrs() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem hdfs = (DistributedFileSystem) fs;

    setupPaths();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem.mkdirs(hdfs, path, FsPermission.createImmutable((short) 0700));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    hdfs.setXAttr(path, name1, value1);
    hdfs.setXAttr(path, name2, value2);
    checkpoint("AFTER_XATTR_SET");

    SnapshotTestHelper.createSnapshot(hdfs, path, snapshotName);
    checkpoint("AFTER_SNAPSHOT_CREATE");

    Path snapshotCopy = new Path(path.toString() + "-copy");
    String[] argv = new String[] { "-cp", "-px", snapshotPath.toUri().toString(),
        snapshotCopy.toUri().toString() };
    int ret = ToolRunner.run(new FsShell(conf), argv);
    assertEquals("cp -px is not working on a snapshot", SUCCESS, ret);

    Map<String, byte[]> xattrs = hdfs.getXAttrs(snapshotCopy);
    assertArrayEquals(value1, xattrs.get(name1));
    assertArrayEquals(value2, xattrs.get(name2));
  }

  /**
   * Helper method to setup path variables for each test
   */
  private void setupPaths() {
    ++pathCount;
    path = new Path("/p" + pathCount);
    snapshotName = "snapshot" + pathCount;
    snapshotName2 = snapshotName + "-2";
    snapshotName3 = snapshotName + "-3";
    snapshotPath = new Path(path, new Path(".snapshot", snapshotName));
    snapshotPath2 = new Path(path, new Path(".snapshot", snapshotName2));
    snapshotPath3 = new Path(path, new Path(".snapshot", snapshotName3));
  }

  /**
   * Restart the NameNode, optionally saving a new checkpoint.
   *
   * @param hdfs the file system
   * @param checkpoint boolean true to save a new checkpoint
   * @throws Exception if restart fails
   */
  private void restartNameNode(DistributedFileSystem hdfs, boolean checkpoint)
      throws Exception {
    if (checkpoint) {
      hdfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_ENTER);
      hdfs.saveNamespace();
      hdfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_LEAVE);
    }
    checkpoint("BEFORE_NAMENODE_RESTART");

    cluster.restartNameNode(0);
    cluster.waitClusterUp();

    // Update fs reference after restart
    fs = cluster.getFileSystem();
    checkpoint("AFTER_NAMENODE_RESTART");
  }
}

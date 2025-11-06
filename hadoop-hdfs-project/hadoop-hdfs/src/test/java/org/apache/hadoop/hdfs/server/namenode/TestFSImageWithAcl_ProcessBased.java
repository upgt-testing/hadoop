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

import static org.apache.hadoop.hdfs.server.namenode.AclTestHelpers.*;
import static org.apache.hadoop.fs.permission.AclEntryScope.*;
import static org.apache.hadoop.fs.permission.AclEntryType.*;
import static org.apache.hadoop.fs.permission.FsAction.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclStatus;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.apache.hadoop.thirdparty.com.google.common.collect.Lists;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFSImageWithAcl}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios. Tests ACL
 * persistence across NameNode restarts and FSImage saves.
 *
 * @see TestFSImageWithAcl Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFSImageWithAcl_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_ACL_SETUP",
      "AFTER_FIRST_RESTART",
      "AFTER_ACL_REMOVE",
      "AFTER_SECOND_RESTART",
      "AFTER_ACL_REAPPLY"
    );
  }

  private void testAcl(boolean persistNamespace) throws Exception {
    Path p = new Path("/p");

    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_ACLS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem fs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs.create(p).close();
    fs.mkdirs(new Path("/23"));

    AclEntry e = new AclEntry.Builder().setName("foo")
        .setPermission(READ_EXECUTE).setScope(ACCESS).setType(USER).build();
    fs.modifyAclEntries(p, Lists.newArrayList(e));

    checkpoint("AFTER_ACL_SETUP");

    restart(fs, persistNamespace);
    checkpoint("AFTER_FIRST_RESTART");

    // Use client-side API instead of cluster.getNamesystem().getAclStatus()
    AclStatus s = fs.getAclStatus(p);
    AclEntry[] returned = Lists.newArrayList(s.getEntries()).toArray(
        new AclEntry[0]);
    Assert.assertArrayEquals(new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ) }, returned);

    fs.removeAcl(p);
    checkpoint("AFTER_ACL_REMOVE");

    if (persistNamespace) {
      fs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
      fs.saveNamespace();
      fs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
    }

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
    checkpoint("AFTER_SECOND_RESTART");

    // Use client-side API
    s = fs.getAclStatus(p);
    returned = Lists.newArrayList(s.getEntries()).toArray(new AclEntry[0]);
    Assert.assertArrayEquals(new AclEntry[] { }, returned);

    fs.modifyAclEntries(p, Lists.newArrayList(e));
    checkpoint("AFTER_ACL_REAPPLY");

    // Use client-side API
    s = fs.getAclStatus(p);
    returned = Lists.newArrayList(s.getEntries()).toArray(new AclEntry[0]);
    Assert.assertArrayEquals(new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ) }, returned);
  }

  @Test
  public void testPersistAcl() throws Exception {
    testAcl(true);
  }

  @Test
  public void testAclEditLog() throws Exception {
    testAcl(false);
  }

  private void doTestDefaultAclNewChildren(boolean persistNamespace)
      throws Exception {
    Path dirPath = new Path("/dir");
    Path filePath = new Path(dirPath, "file1");
    Path subdirPath = new Path(dirPath, "subdir1");

    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_ACLS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem fs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs.mkdirs(dirPath);
    List<AclEntry> aclSpec = Lists.newArrayList(
      aclEntry(DEFAULT, USER, "foo", ALL));
    fs.setAcl(dirPath, aclSpec);

    fs.create(filePath).close();
    fs.mkdirs(subdirPath);
    checkpoint("AFTER_FILE_CREATE");

    AclEntry[] fileExpected = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", ALL),
      aclEntry(ACCESS, GROUP, READ_EXECUTE) };
    AclEntry[] subdirExpected = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", ALL),
      aclEntry(ACCESS, GROUP, READ_EXECUTE),
      aclEntry(DEFAULT, USER, ALL),
      aclEntry(DEFAULT, USER, "foo", ALL),
      aclEntry(DEFAULT, GROUP, READ_EXECUTE),
      aclEntry(DEFAULT, MASK, ALL),
      aclEntry(DEFAULT, OTHER, READ_EXECUTE) };

    short permExpected = (short)010775;

    AclEntry[] fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    AclEntry[] subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);

    restart(fs, persistNamespace);
    checkpoint("AFTER_FIRST_RESTART");

    fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);

    aclSpec = Lists.newArrayList(aclEntry(DEFAULT, USER, "foo", READ_WRITE));
    fs.modifyAclEntries(dirPath, aclSpec);
    checkpoint("AFTER_ACL_MODIFY");

    fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);

    restart(fs, persistNamespace);
    checkpoint("AFTER_SECOND_RESTART");

    fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);

    fs.removeAcl(dirPath);
    checkpoint("AFTER_ACL_REMOVE");

    fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);

    restart(fs, persistNamespace);
    checkpoint("AFTER_THIRD_RESTART");

    fileReturned = fs.getAclStatus(filePath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(fileExpected, fileReturned);
    subdirReturned = fs.getAclStatus(subdirPath).getEntries()
      .toArray(new AclEntry[0]);
    Assert.assertArrayEquals(subdirExpected, subdirReturned);
    assertPermission(fs, subdirPath, permExpected);
  }

  @Test
  public void testFsImageDefaultAclNewChildren() throws Exception {
    doTestDefaultAclNewChildren(true);
  }

  @Test
  public void testEditLogDefaultAclNewChildren() throws Exception {
    doTestDefaultAclNewChildren(false);
  }

  @Test
  public void testRootACLAfterLoadingFsImage() throws Exception {
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_ACLS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem fs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path rootdir = new Path("/");
    AclEntry e1 = new AclEntry.Builder().setName("foo")
        .setPermission(ALL).setScope(ACCESS).setType(GROUP).build();
    AclEntry e2 = new AclEntry.Builder().setName("bar")
        .setPermission(READ).setScope(ACCESS).setType(GROUP).build();
    fs.modifyAclEntries(rootdir, Lists.newArrayList(e1, e2));
    checkpoint("AFTER_ACL_MODIFY");

    // Use client-side API instead of cluster.getNamesystem().getAclStatus()
    AclStatus s = fs.getAclStatus(rootdir);
    AclEntry[] returned =
        Lists.newArrayList(s.getEntries()).toArray(new AclEntry[0]);
    Assert.assertArrayEquals(
        new AclEntry[] { aclEntry(ACCESS, GROUP, READ_EXECUTE),
            aclEntry(ACCESS, GROUP, "bar", READ),
            aclEntry(ACCESS, GROUP, "foo", ALL) }, returned);

    // restart - hence save and load from fsimage
    restart(fs, true);
    checkpoint("AFTER_RESTART");

    // Use client-side API
    s = fs.getAclStatus(rootdir);
    returned = Lists.newArrayList(s.getEntries()).toArray(new AclEntry[0]);
    Assert.assertArrayEquals(
        new AclEntry[] { aclEntry(ACCESS, GROUP, READ_EXECUTE),
            aclEntry(ACCESS, GROUP, "bar", READ),
            aclEntry(ACCESS, GROUP, "foo", ALL) }, returned);
  }

  /**
   * Restart the NameNode, optionally saving a new checkpoint.
   *
   * @param fs DistributedFileSystem used for saving namespace
   * @param persistNamespace boolean true to save a new checkpoint
   * @throws Exception if restart fails
   */
  private void restart(DistributedFileSystem fs, boolean persistNamespace)
      throws Exception {
    if (persistNamespace) {
      fs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
      fs.saveNamespace();
      fs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
    }

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
  }
}

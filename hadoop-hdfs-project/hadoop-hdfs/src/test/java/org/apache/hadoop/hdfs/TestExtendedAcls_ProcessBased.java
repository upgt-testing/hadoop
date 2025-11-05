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
package org.apache.hadoop.hdfs;

import org.apache.hadoop.thirdparty.com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.hadoop.fs.permission.AclEntryScope.ACCESS;
import static org.apache.hadoop.fs.permission.AclEntryType.MASK;
import static org.apache.hadoop.fs.permission.AclEntryType.USER;
import static org.apache.hadoop.fs.permission.FsAction.NONE;
import static org.apache.hadoop.fs.permission.FsAction.READ;
import static org.apache.hadoop.fs.permission.FsAction.READ_EXECUTE;
import static org.apache.hadoop.fs.permission.FsAction.READ_WRITE;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_ACLS_ENABLED_KEY;
import static org.apache.hadoop.hdfs.server.namenode.AclTestHelpers.aclEntry;
import static org.apache.hadoop.fs.permission.AclEntryScope.DEFAULT;
import static org.apache.hadoop.fs.permission.FsAction.ALL;
import static org.apache.hadoop.fs.permission.AclEntryType.GROUP;
import static org.apache.hadoop.fs.permission.AclEntryType.OTHER;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestExtendedAcls}.
 *
 * Tests HDFS directory and file ACL behavior with support for
 * process-based testing and upgrade scenarios.
 *
 * @see TestExtendedAcls Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestExtendedAcls_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final short REPLICATION = 3;
  private DistributedFileSystem hdfs;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_ACL_SETUP",
      "BEFORE_VERIFICATION"
    );
  }

  private void setupCluster() throws Exception {
    conf.setBoolean(DFS_NAMENODE_ACLS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();
    hdfs = (DistributedFileSystem) cluster.getFileSystem();
  }

  /**
   * Set default ACL to a directory.
   * Create subdirectory, it must have default acls set.
   * Create sub file and it should have default acls.
   * @throws Exception
   */
  @Test
  public void testDefaultAclNewChildDirFile() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path parent = new Path("/testDefaultAclNewChildDirFile");
    List<AclEntry> acls = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", ALL));

    hdfs.mkdirs(parent);
    hdfs.setAcl(parent, acls);

    checkpoint("AFTER_ACL_SETUP");

    // create sub directory
    Path childDir = new Path(parent, "childDir");
    hdfs.mkdirs(childDir);

    checkpoint("BEFORE_VERIFICATION");

    // the sub directory should have the default acls
    AclEntry[] childDirExpectedAcl = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", ALL),
      aclEntry(ACCESS, GROUP, READ_EXECUTE),
      aclEntry(DEFAULT, USER, ALL),
      aclEntry(DEFAULT, USER, "foo", ALL),
      aclEntry(DEFAULT, GROUP, READ_EXECUTE),
      aclEntry(DEFAULT, MASK, ALL),
      aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());

    // create sub file
    Path childFile = new Path(parent, "childFile");
    hdfs.create(childFile).close();
    // the sub file should have the default acls
    AclEntry[] childFileExpectedAcl = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", ALL),
      aclEntry(ACCESS, GROUP, READ_EXECUTE)
    };
    AclStatus childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    hdfs.delete(parent, true);
  }

  /**
   * Set default ACL to a directory and make sure existing sub dirs/files
   * does not have default acl.
   * @throws Exception
   */
  @Test
  public void testDefaultAclExistingDirFile() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path parent = new Path("/testDefaultAclExistingDirFile");
    hdfs.mkdirs(parent);
    // the old acls
    List<AclEntry> acls1 = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", ALL));
    // the new acls
    List<AclEntry> acls2 = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE));
    // set parent to old acl
    hdfs.setAcl(parent, acls1);

    Path childDir = new Path(parent, "childDir");
    hdfs.mkdirs(childDir);

    checkpoint("AFTER_ACL_SETUP");

    // the sub directory should also have the old acl
    AclEntry[] childDirExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", ALL),
        aclEntry(ACCESS, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, USER, ALL),
        aclEntry(DEFAULT, USER, "foo", ALL),
        aclEntry(DEFAULT, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, MASK, ALL),
        aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());

    Path childFile = new Path(childDir, "childFile");
    // the sub file should also have the old acl
    hdfs.create(childFile).close();
    AclEntry[] childFileExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", ALL),
        aclEntry(ACCESS, GROUP, READ_EXECUTE)
    };
    AclStatus childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    checkpoint("BEFORE_VERIFICATION");

    // now change parent to new acls
    hdfs.setAcl(parent, acls2);

    // sub directory and sub file should still have the old acls
    childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());
    childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    // now remove the parent acls
    hdfs.removeAcl(parent);

    // sub directory and sub file should still have the old acls
    childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());
    childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    // check changing the access mode of the file
    // mask out the access of group other for testing
    hdfs.setPermission(childFile, new FsPermission((short)0640));
    boolean canAccess =
        tryAccess(childFile, "other", new String[]{"other"}, READ);
    assertFalse(canAccess);
    hdfs.delete(parent, true);
  }

  /**
   * Verify that access acl does not get inherited on newly created subdir/file.
   * @throws Exception
   */
  @Test
  public void testAccessAclNotInherited() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path parent = new Path("/testAccessAclNotInherited");
    hdfs.mkdirs(parent);
    // parent have both access acl and default acl
    List<AclEntry> acls = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, USER, READ_WRITE),
        aclEntry(ACCESS, GROUP, READ),
        aclEntry(ACCESS, OTHER, READ),
        aclEntry(ACCESS, USER, "bar", ALL));
    hdfs.setAcl(parent, acls);

    Path childDir = new Path(parent, "childDir");
    hdfs.mkdirs(childDir);

    checkpoint("AFTER_ACL_SETUP");

    // subdirectory should only have the default acl inherited
    AclEntry[] childDirExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ),
        aclEntry(DEFAULT, USER, READ_WRITE),
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, READ),
        aclEntry(DEFAULT, MASK, READ_EXECUTE),
        aclEntry(DEFAULT, OTHER, READ)
    };
    AclStatus childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());

    Path childFile = new Path(parent, "childFile");
    hdfs.create(childFile).close();

    checkpoint("BEFORE_VERIFICATION");

    // sub file should only have the default acl inherited
    AclEntry[] childFileExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ)
    };
    AclStatus childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    hdfs.delete(parent, true);
  }

  /**
   * Create a parent dir and set default acl to allow foo read/write access.
   * Create a sub dir and set default acl to allow bar group read/write access.
   * parent dir/file can not be viewed/appended by bar group.
   * parent dir/child dir/file can be viewed/appended by bar group.
   * @throws Exception
   */
  @Test
  public void testGradSubdirMoreAccess() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path parent = new Path("/testGradSubdirMoreAccess");
    hdfs.mkdirs(parent);
    List<AclEntry> aclsParent = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE));
    List<AclEntry> aclsChild = Lists.newArrayList(
        aclEntry(DEFAULT, GROUP, "bar", READ_WRITE));

    hdfs.setAcl(parent, aclsParent);
    AclEntry[] parentDirExpectedAcl = new AclEntry[] {
        aclEntry(DEFAULT, USER, ALL),
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, MASK, READ_EXECUTE),
        aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus parentAcl = hdfs.getAclStatus(parent);
    assertArrayEquals(parentDirExpectedAcl, parentAcl.getEntries().toArray());

    Path childDir = new Path(parent, "childDir");
    hdfs.mkdirs(childDir);
    hdfs.modifyAclEntries(childDir, aclsChild);

    checkpoint("AFTER_ACL_SETUP");

    // child dir should inherit the default acls from parent, plus bar group
    AclEntry[] childDirExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, USER, ALL),
        aclEntry(DEFAULT, USER, "foo", READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, "bar", READ_WRITE),
        aclEntry(DEFAULT, MASK, ALL),
        aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());

    Path parentFile = new Path(parent, "parentFile");
    hdfs.create(parentFile).close();
    hdfs.setPermission(parentFile, new FsPermission((short)0640));
    // parent dir/parent file allows foo to access but not bar group
    AclEntry[] parentFileExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
        aclEntry(ACCESS, GROUP, READ_EXECUTE)
    };
    AclStatus parentFileAcl = hdfs.getAclStatus(parentFile);
    assertArrayEquals(parentFileExpectedAcl,
        parentFileAcl.getEntries().toArray());

    Path childFile = new Path(childDir, "childFile");
    hdfs.create(childFile).close();
    hdfs.setPermission(childFile, new FsPermission((short)0640));
    // child dir/child file allows foo user and bar group to access
    AclEntry[] childFileExpectedAcl = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", READ_EXECUTE),
      aclEntry(ACCESS, GROUP, READ_EXECUTE),
      aclEntry(ACCESS, GROUP, "bar", READ_WRITE)
    };
    AclStatus childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    checkpoint("BEFORE_VERIFICATION");

    // parent file should not be accessible for bar group
    assertFalse(tryAccess(parentFile, "barUser", new String[]{"bar"}, READ));
    // child file should be accessible for bar group
    assertTrue(tryAccess(childFile, "barUser", new String[]{"bar"}, READ));
    // parent file should be accessible for foo user
    assertTrue(tryAccess(parentFile, "foo", new String[]{"fooGroup"}, READ));
    // child file should be accessible for foo user
    assertTrue(tryAccess(childFile, "foo", new String[]{"fooGroup"}, READ));

    hdfs.delete(parent, true);
  }

  /**
   * Verify that sub directory can restrict acl with acl inherited from parent.
   * Create a parent dir and set default to allow foo and bar full access
   * Create a sub dir and set default to restrict bar to empty access
   *
   * parent dir/file can be viewed by foo
   * parent dir/child dir/file can be viewed by foo
   * parent dir/child dir/file can not be viewed by bar
   *
   * @throws Exception
   */
  @Test
  public void testRestrictAtSubDir() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path parent = new Path("/testRestrictAtSubDir");
    hdfs.mkdirs(parent);
    List<AclEntry> aclsParent = Lists.newArrayList(
        aclEntry(DEFAULT, USER, "foo", ALL),
        aclEntry(DEFAULT, GROUP, "bar", ALL)
    );
    hdfs.setAcl(parent, aclsParent);
    AclEntry[] parentDirExpectedAcl = new AclEntry[] {
        aclEntry(DEFAULT, USER, ALL),
        aclEntry(DEFAULT, USER, "foo", ALL),
        aclEntry(DEFAULT, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, "bar", ALL),
        aclEntry(DEFAULT, MASK, ALL),
        aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus parentAcl = hdfs.getAclStatus(parent);
    assertArrayEquals(parentDirExpectedAcl, parentAcl.getEntries().toArray());

    Path parentFile = new Path(parent, "parentFile");
    hdfs.create(parentFile).close();
    hdfs.setPermission(parentFile, new FsPermission((short)0640));
    AclEntry[] parentFileExpectedAcl = new AclEntry[] {
      aclEntry(ACCESS, USER, "foo", ALL),
      aclEntry(ACCESS, GROUP, READ_EXECUTE),
      aclEntry(ACCESS, GROUP, "bar", ALL),
    };
    AclStatus parentFileAcl = hdfs.getAclStatus(parentFile);
    assertArrayEquals(
        parentFileExpectedAcl, parentFileAcl.getEntries().toArray());

    Path childDir = new Path(parent, "childDir");
    hdfs.mkdirs(childDir);
    List<AclEntry> newAclsChild = Lists.newArrayList(
        aclEntry(DEFAULT, GROUP, "bar", NONE)
    );
    hdfs.modifyAclEntries(childDir, newAclsChild);

    checkpoint("AFTER_ACL_SETUP");

    AclEntry[] childDirExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", ALL),
        aclEntry(ACCESS, GROUP, READ_EXECUTE),
        aclEntry(ACCESS, GROUP, "bar", ALL),
        aclEntry(DEFAULT, USER, ALL),
        aclEntry(DEFAULT, USER, "foo", ALL),
        aclEntry(DEFAULT, GROUP, READ_EXECUTE),
        aclEntry(DEFAULT, GROUP, "bar", NONE),
        aclEntry(DEFAULT, MASK, ALL),
        aclEntry(DEFAULT, OTHER, READ_EXECUTE)
    };
    AclStatus childDirAcl = hdfs.getAclStatus(childDir);
    assertArrayEquals(childDirExpectedAcl, childDirAcl.getEntries().toArray());

    Path childFile = new Path(childDir, "childFile");
    hdfs.create(childFile).close();
    hdfs.setPermission(childFile, new FsPermission((short)0640));
    AclEntry[] childFileExpectedAcl = new AclEntry[] {
        aclEntry(ACCESS, USER, "foo", ALL),
        aclEntry(ACCESS, GROUP, READ_EXECUTE),
        aclEntry(ACCESS, GROUP, "bar", NONE)
    };
    AclStatus childFileAcl = hdfs.getAclStatus(childFile);
    assertArrayEquals(
        childFileExpectedAcl, childFileAcl.getEntries().toArray());

    checkpoint("BEFORE_VERIFICATION");

    // child file should not be accessible for bar group
    assertFalse(tryAccess(childFile, "barUser", new String[]{"bar"}, READ));
    // child file should be accessible for foo user
    assertTrue(tryAccess(childFile, "foo", new String[]{"fooGroup"}, READ));
    // parent file should be accessible for bar group
    assertTrue(tryAccess(parentFile, "barUser", new String[]{"bar"}, READ));
    // parent file should be accessible for foo user
    assertTrue(tryAccess(parentFile, "foo", new String[]{"fooGroup"}, READ));

    hdfs.delete(parent, true);
  }

  private boolean tryAccess(Path path, String user,
      String[] group, FsAction action) throws Exception {
    UserGroupInformation testUser =
        UserGroupInformation.createUserForTesting(
            user, group);
    FileSystem fs = testUser.doAs(new PrivilegedExceptionAction<FileSystem>() {
      @Override
      public FileSystem run() throws Exception {
        return FileSystem.get(conf);
      }
    });

    boolean canAccess;
    try {
      fs.access(path, action);
      canAccess = true;
    } catch (AccessControlException e) {
      canAccess = false;
    }
    return canAccess;
  }
}

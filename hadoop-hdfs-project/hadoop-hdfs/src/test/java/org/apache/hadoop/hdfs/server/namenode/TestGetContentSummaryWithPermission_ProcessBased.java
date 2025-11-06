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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
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

import static org.apache.hadoop.fs.permission.FsAction.READ_EXECUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestGetContentSummaryWithPermission}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios. Tests
 * getContentSummary permission checking with different users.
 *
 * @see TestGetContentSummaryWithPermission Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestGetContentSummaryWithPermission_ProcessBased extends ProcessBasedUpgradeTestBase {

  protected static final short REPLICATION = 3;
  protected static final long BLOCKSIZE = 1024;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_CREATE",
      "AFTER_PERMISSION_CHANGE",
      "AFTER_VERIFICATION"
    );
  }

  /**
   * Test getContentSummary for super user. For super user, whatever
   * permission the directories are with, always allowed to access
   *
   * @throws Exception
   */
  @Test
  public void testGetContentSummarySuperUser() throws Exception {
    final Path foo = new Path("/fooSuper");
    final Path bar = new Path(foo, "barSuper");
    final Path baz = new Path(bar, "bazSuper");

    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem dfs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    dfs.mkdirs(bar);
    DFSTestUtil.createFile(dfs, baz, 10, REPLICATION, 0L);
    checkpoint("AFTER_FILE_CREATE");

    ContentSummary summary;

    // Use client-side API instead of cluster.getNameNodeRpc().getContentSummary()
    summary = dfs.getContentSummary(foo);
    verifySummary(summary, 2, 1, 10);

    dfs.setPermission(foo, new FsPermission((short)0));
    checkpoint("AFTER_PERMISSION_CHANGE");

    summary = dfs.getContentSummary(foo);
    verifySummary(summary, 2, 1, 10);

    dfs.setPermission(bar, new FsPermission((short)0));
    checkpoint("AFTER_VERIFICATION");

    summary = dfs.getContentSummary(foo);
    verifySummary(summary, 2, 1, 10);

    dfs.setPermission(baz, new FsPermission((short)0));

    summary = dfs.getContentSummary(foo);
    verifySummary(summary, 2, 1, 10);
  }

  /**
   * Test getContentSummary for non-super, non-owner. Such users are restricted
   * by permission of subdirectories. Namely if there is any subdirectory that
   * does not have READ_EXECUTE access, AccessControlException will be thrown.
   *
   * @throws Exception
   */
  @Test
  public void testGetContentSummaryNonSuperUser() throws Exception {
    final Path foo = new Path("/fooNoneSuper");
    final Path bar = new Path(foo, "barNoneSuper");
    final Path baz = new Path(bar, "bazNoneSuper");
    // run as some random non-superuser, non-owner user.
    final UserGroupInformation userUgi  =
        UserGroupInformation.createUserForTesting(
            "randomUser", new String[]{"randomGroup"});

    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem dfs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    dfs.mkdirs(bar);
    DFSTestUtil.createFile(dfs, baz, 10, REPLICATION, 0L);
    checkpoint("AFTER_FILE_CREATE");

    // by default, permission is rwxr-xr-x, as long as READ and EXECUTE are set,
    // content summary should accessible
    FileStatus fileStatus;
    fileStatus = dfs.getFileStatus(foo);
    assertEquals((short)755, fileStatus.getPermission().toOctal());
    fileStatus = dfs.getFileStatus(bar);
    assertEquals((short)755, fileStatus.getPermission().toOctal());
    // file has no EXECUTE, it is rw-r--r-- default
    fileStatus = dfs.getFileStatus(baz);
    assertEquals((short)644, fileStatus.getPermission().toOctal());

    // by default, can get content summary
    // Use client-side API instead of cluster.getNameNodeRpc().getContentSummary()
    ContentSummary summary =
        userUgi.doAs((PrivilegedExceptionAction<ContentSummary>)
            () -> {
              DistributedFileSystem userDfs = (DistributedFileSystem)
                  DistributedFileSystem.get(cluster.getURI(0), conf);
              return userDfs.getContentSummary(foo);
            });
    verifySummary(summary, 2, 1, 10);
    checkpoint("AFTER_VERIFICATION");

    // set empty access on root dir, should disallow content summary
    dfs.setPermission(foo, new FsPermission((short)0));
    checkpoint("AFTER_PERMISSION_CHANGE");

    try {
      userUgi.doAs((PrivilegedExceptionAction<ContentSummary>)
          () -> {
            DistributedFileSystem userDfs = (DistributedFileSystem)
                DistributedFileSystem.get(cluster.getURI(0), conf);
            return userDfs.getContentSummary(foo);
          });
      fail("Should've fail due to access control exception.");
    } catch (AccessControlException e) {
      assertTrue(e.getMessage().contains("Permission denied"));
    }

    // restore foo's permission to allow READ_EXECUTE
    dfs.setPermission(foo,
        new FsPermission(READ_EXECUTE, READ_EXECUTE, READ_EXECUTE));

    // set empty access on subdir, should disallow content summary from root dir
    dfs.setPermission(bar, new FsPermission((short)0));

    try {
      userUgi.doAs((PrivilegedExceptionAction<ContentSummary>)
          () -> {
            DistributedFileSystem userDfs = (DistributedFileSystem)
                DistributedFileSystem.get(cluster.getURI(0), conf);
            return userDfs.getContentSummary(foo);
          });
      fail("Should've fail due to access control exception.");
    } catch (AccessControlException e) {
      assertTrue(e.getMessage().contains("Permission denied"));
    }

    // restore the permission of subdir to READ_EXECUTE. enable
    // getContentSummary again for root
    dfs.setPermission(bar,
        new FsPermission(READ_EXECUTE, READ_EXECUTE, READ_EXECUTE));

    summary = userUgi.doAs((PrivilegedExceptionAction<ContentSummary>)
        () -> {
          DistributedFileSystem userDfs = (DistributedFileSystem)
              DistributedFileSystem.get(cluster.getURI(0), conf);
          return userDfs.getContentSummary(foo);
        });
    verifySummary(summary, 2, 1, 10);

    // permission of files under the directory does not affect
    // getContentSummary
    dfs.setPermission(baz, new FsPermission((short)0));
    summary = userUgi.doAs((PrivilegedExceptionAction<ContentSummary>)
        () -> {
          DistributedFileSystem userDfs = (DistributedFileSystem)
              DistributedFileSystem.get(cluster.getURI(0), conf);
          return userDfs.getContentSummary(foo);
        });
    verifySummary(summary, 2, 1, 10);
  }

  private void verifySummary(ContentSummary summary, int dirCount,
      int fileCount, int length) {
    assertEquals(dirCount, summary.getDirectoryCount());
    assertEquals(fileCount, summary.getFileCount());
    assertEquals(length, summary.getLength());
  }
}

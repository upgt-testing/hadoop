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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.TestTrash;
import org.apache.hadoop.fs.Trash;
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
import org.mockito.Mockito;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestHDFSTrash}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests trash functionality in HDFS including trash operations, permissions,
 * non-default filesystem handling, and user-specific trash management.
 *
 * @see TestHDFSTrash Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestHDFSTrash_ProcessBased extends ProcessBasedUpgradeTestBase {

  public static final Logger LOG = LoggerFactory.getLogger(TestHDFSTrash_ProcessBased.class);

  private final static Path TEST_ROOT = new Path("/TestHDFSTrash-ROOT");
  private final static Path TRASH_ROOT = new Path("/TestHDFSTrash-TRASH");

  final private static String GROUP1_NAME = "group1";
  final private static String GROUP2_NAME = "group2";
  final private static String GROUP3_NAME = "group3";
  final private static String USER1_NAME = "user1";
  final private static String USER2_NAME = "user2";

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_SETUP",
      "AFTER_TRASH_SHELL_TEST",
      "AFTER_NON_DEFAULT_FS_TEST",
      "AFTER_TRASH_PERMISSION_TEST",
      "AFTER_EMPTY_DIR_TEST",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * Return a {@link Trash} instance using giving configuration.
   * The trash root directory is set to an unique directory under
   * {@link #TRASH_ROOT}. Use this method to isolate trash
   * directories for different users.
   */
  private Trash getPerUserTrash(UserGroupInformation ugi,
      FileSystem fileSystem, Configuration config) throws IOException {
    // generate an unique path per instance
    UUID trashId = UUID.randomUUID();
    StringBuffer sb = new StringBuffer()
        .append(ugi.getUserName())
        .append("-")
        .append(trashId.toString());
    Path userTrashRoot = new Path(TRASH_ROOT, sb.toString());
    FileSystem spyUserFs = Mockito.spy(fileSystem);
    Mockito.when(spyUserFs.getTrashRoot(Mockito.any()))
        .thenReturn(userTrashRoot);
    return new Trash(spyUserFs, config);
  }

  @Test
  public void testTrashFunctionality() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();

    // Setup users
    UserGroupInformation superUser = UserGroupInformation.getCurrentUser();
    UserGroupInformation user1 = UserGroupInformation.createUserForTesting(USER1_NAME,
        new String[] {GROUP1_NAME, GROUP2_NAME});
    UserGroupInformation user2 = UserGroupInformation.createUserForTesting(USER2_NAME,
        new String[] {GROUP2_NAME, GROUP3_NAME});

    // Init test and trash root dirs in HDFS
    fs.mkdirs(TEST_ROOT);
    fs.setPermission(TEST_ROOT, new FsPermission((short) 0777));
    DFSTestUtil.verifyFilePermission(
        fs.getFileStatus(TEST_ROOT),
        superUser.getShortUserName(),
        null, FsAction.ALL, FsAction.ALL, FsAction.ALL);

    fs.mkdirs(TRASH_ROOT);
    fs.setPermission(TRASH_ROOT, new FsPermission((short) 0777));
    DFSTestUtil.verifyFilePermission(
        fs.getFileStatus(TRASH_ROOT),
        superUser.getShortUserName(),
        null, FsAction.ALL, FsAction.ALL, FsAction.ALL);

    checkpoint("AFTER_SETUP");

    // Test 1: testTrash
    TestTrash.trashShell(cluster.getFileSystem(), new Path("/"));

    checkpoint("AFTER_TRASH_SHELL_TEST");

    // Test 2: testNonDefaultFS
    FileSystem fileSystem = cluster.getFileSystem();
    Configuration config = fileSystem.getConf();
    config.set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
        fileSystem.getUri().toString());
    TestTrash.trashNonDefaultFS(config);

    checkpoint("AFTER_NON_DEFAULT_FS_TEST");

    // Test 3: testHDFSTrashPermission
    config.set(CommonConfigurationKeys.FS_TRASH_INTERVAL_KEY, "0.2");
    TestTrash.verifyTrashPermission(fileSystem, config);

    checkpoint("AFTER_TRASH_PERMISSION_TEST");

    // Test 4: testMoveEmptyDirToTrash
    config.set(CommonConfigurationKeys.FS_TRASH_INTERVAL_KEY, "1");
    TestTrash.verifyMoveEmptyDirToTrash(fileSystem, config);

    checkpoint("AFTER_EMPTY_DIR_TEST");

    // Test 5: testDeleteTrash
    Configuration testConf = new Configuration(conf);
    testConf.set(CommonConfigurationKeys.FS_TRASH_INTERVAL_KEY, "10");

    Path user1Tmp = new Path(TEST_ROOT, "test-del-u1");
    Path user2Tmp = new Path(TEST_ROOT, "test-del-u2");

    // login as user1, move something to trash
    // verify user1 can remove its own trash dir
    FileSystem fs1 = DFSTestUtil.login(fs, testConf, user1);
    fs1.mkdirs(user1Tmp);
    Trash u1Trash = getPerUserTrash(user1, fs1, testConf);
    Path u1t = u1Trash.getCurrentTrashDir(user1Tmp);
    assertTrue(String.format("Failed to move %s to trash", user1Tmp),
        u1Trash.moveToTrash(user1Tmp));
    assertTrue(
        String.format(
            "%s should be allowed to remove its own trash directory %s",
            user1.getUserName(), u1t),
        fs1.delete(u1t, true));
    assertFalse(fs1.exists(u1t));

    // login as user2, move something to trash
    FileSystem fs2 = DFSTestUtil.login(fs, testConf, user2);
    fs2.mkdirs(user2Tmp);
    Trash u2Trash = getPerUserTrash(user2, fs2, testConf);
    u2Trash.moveToTrash(user2Tmp);
    Path u2t = u2Trash.getCurrentTrashDir(user2Tmp);

    try {
      // user1 should not be able to remove user2's trash dir
      fs1 = DFSTestUtil.login(fs, testConf, user1);
      fs1.delete(u2t, true);
      fail(String.format("%s should not be able to remove %s trash directory",
              USER1_NAME, USER2_NAME));
    } catch (AccessControlException e) {
      assertTrue(e instanceof AccessControlException);
      assertTrue("Permission denied messages must carry the username",
          e.getMessage().contains(USER1_NAME));
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

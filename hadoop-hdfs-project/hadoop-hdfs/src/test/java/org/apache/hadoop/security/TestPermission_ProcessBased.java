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
package org.apache.hadoop.security;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestPermission}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests file and directory permission handling in HDFS with upgrades.
 *
 * @see TestPermission Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestPermission_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestPermission_ProcessBased.class);

  final private static Path ROOT_PATH = new Path("/data");
  final private static Path CHILD_DIR1 = new Path(ROOT_PATH, "child1");
  final private static Path CHILD_DIR2 = new Path(ROOT_PATH, "child2");
  final private static Path CHILD_DIR3 = new Path(ROOT_PATH, "child3");
  final private static Path CHILD_FILE1 = new Path(ROOT_PATH, "file1");
  final private static Path CHILD_FILE2 = new Path(ROOT_PATH, "file2");
  final private static Path CHILD_FILE3 = new Path(ROOT_PATH, "file3");

  final private static int FILE_LEN = 100;
  final private static Random RAN = new Random();
  final private static String USER_NAME = "user" + RAN.nextInt();
  final private static String[] GROUP_NAMES = {"group1", "group2"};

  private FileSystem nnfs;
  private FileSystem userfs;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_PERMISSION_SETUP",
      "AFTER_DIR_CREATE",
      "AFTER_FILE_CREATE",
      "AFTER_PERMISSION_CHANGE",
      "AFTER_MULTI_USER_SETUP",
      "BEFORE_VERIFICATION"
    );
  }

  static FsPermission checkPermission(FileSystem filesystem,
      String path, FsPermission expected) throws IOException {
    FileStatus s = filesystem.getFileStatus(new Path(path));
    LOG.info(s.getPath() + ": " + s.isDirectory() + " " + s.getPermission()
        + ":" + s.getOwner() + ":" + s.getGroup());
    if (expected != null) {
      assertEquals(expected, s.getPermission());
      assertEquals(expected.toShort(), s.getPermission().toShort());
    }
    return s.getPermission();
  }

  static boolean canMkdirs(FileSystem filesystem, Path dir) throws IOException {
    try {
      filesystem.mkdirs(dir);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  static boolean canCreate(FileSystem filesystem, Path file) throws IOException {
    try {
      filesystem.create(file).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  static boolean canOpen(FileSystem filesystem, Path file) throws IOException {
    try {
      filesystem.open(file).close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  static boolean canRename(FileSystem filesystem, Path src, Path dst) throws IOException {
    try {
      return filesystem.rename(src, dst);
    } catch (IOException e) {
      return false;
    }
  }

  @Test
  public void testCreate() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, true);
    testConf.set(FsPermission.UMASK_LABEL, "000");

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    FsPermission rootPerm = checkPermission(fs, "/", null);
    FsPermission inheritPerm = FsPermission.createImmutable(
        (short)(rootPerm.toShort() | 0300));
    checkpoint("AFTER_PERMISSION_SETUP");

    FsPermission dirPerm = new FsPermission((short)0777);
    fs.mkdirs(new Path("/a1/a2/a3"), dirPerm);
    checkPermission(fs, "/a1", dirPerm);
    checkPermission(fs, "/a1/a2", dirPerm);
    checkPermission(fs, "/a1/a2/a3", dirPerm);
    checkpoint("AFTER_DIR_CREATE");

    dirPerm = new FsPermission((short)0123);
    FsPermission permission = FsPermission.createImmutable(
      (short)(dirPerm.toShort() | 0300));
    fs.mkdirs(new Path("/aa/1/aa/2/aa/3"), dirPerm);
    checkPermission(fs, "/aa/1", permission);
    checkPermission(fs, "/aa/1/aa/2", permission);
    checkPermission(fs, "/aa/1/aa/2/aa/3", dirPerm);

    FsPermission filePerm = new FsPermission((short)0444);
    Path p = new Path("/b1/b2/b3.txt");
    FSDataOutputStream out = fs.create(p, filePerm,
        true, testConf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096),
        fs.getDefaultReplication(p), fs.getDefaultBlockSize(p), null);
    out.write(123);
    out.close();
    checkpoint("AFTER_FILE_CREATE");

    checkPermission(fs, "/b1", inheritPerm);
    checkPermission(fs, "/b1/b2", inheritPerm);
    checkPermission(fs, "/b1/b2/b3.txt", filePerm);

    testConf.set(FsPermission.UMASK_LABEL, "022");
    permission =
      FsPermission.createImmutable((short)0666);
    FileSystem.mkdirs(fs, new Path("/c1"), new FsPermission(permission));
    FileSystem.create(fs, new Path("/c1/c2.txt"),
        new FsPermission(permission)).close();
    checkpoint("AFTER_PERMISSION_CHANGE");

    checkPermission(fs, "/c1", permission);
    checkPermission(fs, "/c1/c2.txt", permission);
    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testFilePermission() throws Exception {
    final Configuration testConf = new HdfsConfiguration(conf);
    testConf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, true);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    nnfs = cluster.getFileSystem();
    // test permissions on files that do not exist
    assertFalse(nnfs.exists(CHILD_FILE1));
    try {
      nnfs.setPermission(CHILD_FILE1, new FsPermission((short)0777));
      assertTrue(false);
    }
    catch(java.io.FileNotFoundException e) {
      LOG.info("GOOD: got " + e);
    }

    // make sure nn can take user specified permission (with default fs
    // permission umask applied)
    FSDataOutputStream out = nnfs.create(CHILD_FILE1, new FsPermission(
        (short) 0777), true, 1024, (short) 1, 1024, null);
    out.close();
    checkpoint("AFTER_FILE_CREATE");

    FileStatus status = nnfs.getFileStatus(CHILD_FILE1);
    // FS_PERMISSIONS_UMASK_DEFAULT is 0022
    assertTrue(status.getPermission().toString().equals("rwxr-xr-x"));
    nnfs.delete(CHILD_FILE1, false);

    // following dir/file creations are legal
    nnfs.mkdirs(CHILD_DIR1);
    checkpoint("AFTER_DIR_CREATE");

    status = nnfs.getFileStatus(CHILD_DIR1);
    assertThat("Expect 755 = 777 (default dir) - 022 (default umask)",
        status.getPermission().toString(), is("rwxr-xr-x"));
    out = nnfs.create(CHILD_FILE1);
    status = nnfs.getFileStatus(CHILD_FILE1);
    assertTrue(status.getPermission().toString().equals("rw-r--r--"));
    byte data[] = new byte[FILE_LEN];
    RAN.nextBytes(data);
    out.write(data);
    out.close();
    checkpoint("AFTER_DATA_WRITE");

    nnfs.setPermission(CHILD_FILE1, new FsPermission("700"));
    status = nnfs.getFileStatus(CHILD_FILE1);
    assertTrue(status.getPermission().toString().equals("rwx------"));
    checkpoint("AFTER_PERMISSION_CHANGE");

    // mkdirs with null permission
    nnfs.mkdirs(CHILD_DIR3, null);
    status = nnfs.getFileStatus(CHILD_DIR3);
    assertThat("Expect 755 = 777 (default dir) - 022 (default umask)",
        status.getPermission().toString(), is("rwxr-xr-x"));

    // following read is legal
    byte dataIn[] = new byte[FILE_LEN];
    FSDataInputStream fin = nnfs.open(CHILD_FILE1);
    int bytesRead = fin.read(dataIn);
    fin.close();
    assertTrue(bytesRead == FILE_LEN);
    for(int i=0; i<FILE_LEN; i++) {
      assertEquals(data[i], dataIn[i]);
    }

    // test execution bit support for files
    nnfs.setPermission(CHILD_FILE1, new FsPermission("755"));
    status = nnfs.getFileStatus(CHILD_FILE1);
    assertTrue(status.getPermission().toString().equals("rwxr-xr-x"));
    nnfs.setPermission(CHILD_FILE1, new FsPermission("744"));
    status = nnfs.getFileStatus(CHILD_FILE1);
    assertTrue(status.getPermission().toString().equals("rwxr--r--"));
    nnfs.setPermission(CHILD_FILE1, new FsPermission("700"));

    ////////////////////////////////////////////////////////////////
    // test illegal file/dir creation
    UserGroupInformation userGroupInfo =
      UserGroupInformation.createUserForTesting(USER_NAME, GROUP_NAMES);

    userfs = DFSTestUtil.getFileSystemAs(userGroupInfo, testConf);
    checkpoint("AFTER_MULTI_USER_SETUP");

    // make sure mkdir of a existing directory that is not owned by
    // this user does not throw an exception.
    userfs.mkdirs(CHILD_DIR1);

    // illegal mkdir
    assertTrue(!canMkdirs(userfs, CHILD_DIR2));

    // illegal file creation
    assertTrue(!canCreate(userfs, CHILD_FILE2));

    // illegal file open
    assertTrue(!canOpen(userfs, CHILD_FILE1));

    nnfs.setPermission(ROOT_PATH, new FsPermission((short)0755));
    nnfs.setPermission(CHILD_DIR1, new FsPermission("777"));
    nnfs.setPermission(new Path("/"), new FsPermission((short)0777));
    final Path RENAME_PATH = new Path("/foo/bar");
    userfs.mkdirs(RENAME_PATH);
    assertTrue(canRename(userfs, RENAME_PATH, CHILD_DIR1));
    checkpoint("BEFORE_VERIFICATION");

    // test permissions on files that do not exist
    assertFalse(userfs.exists(CHILD_FILE3));
    try {
      userfs.setPermission(CHILD_FILE3, new FsPermission((short) 0777));
      fail("setPermission should fail for non-exist file");
    } catch (java.io.FileNotFoundException ignored) {
      LOG.info("GOOD: got expected FileNotFoundException");
    }
  }
}

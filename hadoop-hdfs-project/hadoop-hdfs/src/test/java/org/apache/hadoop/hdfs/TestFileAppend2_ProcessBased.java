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

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppend2} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during file append operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestFileAppend2 Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestFileAppend2_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  /**
   * Define upgrade checkpoints for parameterized test execution.
   *
   * @return collection of checkpoint names
   */
  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        // Baseline - no upgrade
        UpgradeCheckpoints.NO_UPGRADE,

        // Cluster lifecycle
        UpgradeCheckpoints.AFTER_CLUSTER_START,

        // First write phase
        UpgradeCheckpoints.AFTER_FILE_CREATE,
        UpgradeCheckpoints.AFTER_FIRST_WRITE,
        UpgradeCheckpoints.AFTER_FIRST_CLOSE,

        // Second write phase
        "AFTER_FIRST_APPEND_REOPEN",
        UpgradeCheckpoints.AFTER_SECOND_WRITE,
        "AFTER_SECOND_CLOSE",

        // Third write phase
        "AFTER_SECOND_APPEND_REOPEN",
        "AFTER_THIRD_WRITE",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION,
        UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  private byte[] fileContents = null;

  /**
   * Creates one file, writes a few bytes to it and then closed it.
   * Reopens the same file for appending, write all blocks and then close.
   * Verify that all data exists in file.
   *
   * <p>With 12 checkpoints, this single test method generates 12 test executions,
   * each testing upgrade at a different point in the append workflow.
   *
   * @throws Exception an exception might be thrown
   */
  @Test
  public void testSimpleAppend() throws Exception {
    conf.setInt(DFSConfigKeys.DFS_DATANODE_HANDLER_COUNT_KEY, 50);
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    { // test appending to a file.

      // create a new file.
      Path file1 = new Path("/simpleAppend.dat");
      FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
      System.out.println("Created file simpleAppend.dat");

      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      // write to file
      int mid = 186;   // io.bytes.per.checksum bytes
      System.out.println("Writing " + mid + " bytes to file " + file1);
      stm.write(fileContents, 0, mid);

      checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

      stm.close();
      System.out.println("Wrote and Closed first part of file.");

      checkpoint(UpgradeCheckpoints.AFTER_FIRST_CLOSE);

      // write to file
      int mid2 = 607;   // io.bytes.per.checksum bytes
      System.out.println("Writing " + mid + " bytes to file " + file1);
      stm = fs.append(file1);

      checkpoint("AFTER_FIRST_APPEND_REOPEN");

      stm.write(fileContents, mid, mid2-mid);

      checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

      stm.close();
      System.out.println("Wrote and Closed second part of file.");

      checkpoint("AFTER_SECOND_CLOSE");

      // write the remainder of the file
      stm = fs.append(file1);

      checkpoint("AFTER_SECOND_APPEND_REOPEN");

      // ensure getPos is set to reflect existing size of the file
      assertTrue(stm.getPos() > 0);

      System.out.println("Writing " + (AppendTestUtil.FILE_SIZE - mid2) +
          " bytes to file " + file1);
      stm.write(fileContents, mid2, AppendTestUtil.FILE_SIZE - mid2);

      checkpoint("AFTER_THIRD_WRITE");

      System.out.println("Written second part of file");
      stm.close();
      System.out.println("Wrote and Closed second part of file.");

      checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
          fileContents, "Read 2");

      checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
    }

    { // test appending to an non-existing file.
      FSDataOutputStream out = null;
      try {
        out = fs.append(new Path("/non-existing.dat"));
        fail("Expected to have FileNotFoundException");
      }
      catch(java.io.FileNotFoundException fnfe) {
        System.out.println("Good: got " + fnfe);
        fnfe.printStackTrace(System.out);
      }
      finally {
        IOUtils.closeStream(out);
      }
    }

    { // test append permission.

      //set root to all writable
      Path root = new Path("/");
      fs.setPermission(root, new FsPermission((short)0777));
      fs.close();

      // login as a different user
      final UserGroupInformation superuser =
        UserGroupInformation.getCurrentUser();
      String username = "testappenduser";
      String group = "testappendgroup";
      assertFalse(superuser.getShortUserName().equals(username));
      assertFalse(Arrays.asList(superuser.getGroupNames()).contains(group));
      UserGroupInformation appenduser =
        UserGroupInformation.createUserForTesting(username, new String[]{group});

      fs = (DistributedFileSystem) DFSTestUtil.getFileSystemAs(appenduser, conf);

      // create a file
      Path dir = new Path(root, getClass().getSimpleName());
      Path foo = new Path(dir, "foo.dat");
      FSDataOutputStream out = null;
      int offset = 0;
      try {
        out = fs.create(foo);
        int len = 10 + AppendTestUtil.nextInt(100);
        out.write(fileContents, offset, len);
        offset += len;
      }
      finally {
        IOUtils.closeStream(out);
      }

      // change dir and foo to minimal permissions.
      fs.setPermission(dir, new FsPermission((short)0100));
      fs.setPermission(foo, new FsPermission((short)0200));

      // try append, should success
      out = null;
      try {
        out = fs.append(foo);
        int len = 10 + AppendTestUtil.nextInt(100);
        out.write(fileContents, offset, len);
        offset += len;
      }
      finally {
        IOUtils.closeStream(out);
      }

      // change dir and foo to all but no write on foo.
      fs.setPermission(foo, new FsPermission((short)0577));
      fs.setPermission(dir, new FsPermission((short)0777));

      // try append, should fail
      out = null;
      try {
        out = fs.append(foo);
        fail("Expected to have AccessControlException");
      }
      catch(AccessControlException ace) {
        System.out.println("Good: got " + ace);
        ace.printStackTrace(System.out);
      }
      finally {
        IOUtils.closeStream(out);
      }
    }
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

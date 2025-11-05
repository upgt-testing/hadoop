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

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.InvalidPathException;
import org.apache.hadoop.fs.ParentNotDirectoryException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.util.Time;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSMkdirs}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSMkdirs Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSMkdirs_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final String[] NON_CANONICAL_PATHS = new String[] {
      "//test1",
      "/test2/..",
      "/test2//bar",
      "/test2/../test4",
      "/test5/."
  };

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_MKDIR",
      "BEFORE_VERIFICATION"
    );
  }

  /**
   * Tests mkdirs can create a directory that does not exist and will
   * not create a subdirectory off a file. Regression test for HADOOP-281.
   */
  @Test
  public void testDFSMkdirs() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitClusterUp();
    FileSystem fileSys = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // First create a new directory with mkdirs
    Path myPath = new Path("/test/mkdirs");
    assertTrue(fileSys.mkdirs(myPath));
    assertTrue(fileSys.exists(myPath));
    assertTrue(fileSys.mkdirs(myPath));

    checkpoint("AFTER_MKDIR");

    // Second, create a file in that directory.
    Path myFile = new Path("/test/mkdirs/myFile");
    DFSTestUtil.writeFile(fileSys, myFile, "hello world");

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // Third, use mkdir to create a subdirectory off of that file,
    // and check that it fails.
    Path myIllegalPath = new Path("/test/mkdirs/myFile/subdir");
    Boolean exist = true;
    try {
      fileSys.mkdirs(myIllegalPath);
    } catch (IOException e) {
      exist = false;
    }

    checkpoint("BEFORE_VERIFICATION");

    assertFalse(exist);
    assertFalse(fileSys.exists(myIllegalPath));
    fileSys.delete(myFile, true);
  }

  /**
   * Tests mkdir will not create directory when parent is missing.
   */
  @Test
  public void testMkdir() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();
    DistributedFileSystem dfs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create a dir in root dir, should succeed
    assertTrue(dfs.mkdir(new Path("/mkdir-" + Time.now()),
        FsPermission.getDefault()));

    checkpoint("AFTER_MKDIR");

    // Create a dir when parent dir exists as a file, should fail
    IOException expectedException = null;
    String filePath = "/mkdir-file-" + Time.now();
    DFSTestUtil.writeFile(dfs, new Path(filePath), "hello world");

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    try {
      dfs.mkdir(new Path(filePath + "/mkdir"), FsPermission.getDefault());
    } catch (IOException e) {
      expectedException = e;
    }
    assertTrue("Create a directory when parent dir exists as file using"
        + " mkdir() should throw ParentNotDirectoryException ",
        expectedException != null
            && expectedException instanceof ParentNotDirectoryException);

    checkpoint("BEFORE_VERIFICATION");

    // Create a dir in a non-exist directory, should fail
    expectedException = null;
    try {
      dfs.mkdir(new Path("/non-exist/mkdir-" + Time.now()),
          FsPermission.getDefault());
    } catch (IOException e) {
      expectedException = e;
    }
    assertTrue("Create a directory in a non-exist parent dir using"
        + " mkdir() should throw FileNotFoundException ",
        expectedException != null
            && expectedException instanceof FileNotFoundException);
  }

  /**
   * Regression test for HDFS-3626. Creates a file using a non-canonical path
   * (i.e. with extra slashes between components) and makes sure that the NN
   * rejects it.
   */
  @Test
  public void testMkdirRpcNonCanonicalPath() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(0).build();
    cluster.waitClusterUp();
    DistributedFileSystem dfs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Use DFSClient to get NameNode RPC proxy
    ClientProtocol nnrpc = dfs.getClient().getNamenode();

    for (String pathStr : NON_CANONICAL_PATHS) {
      try {
        nnrpc.mkdirs(pathStr, new FsPermission((short)0755), true);
        fail("Did not fail when called with a non-canonicalized path: "
           + pathStr);
      } catch (InvalidPathException ipe) {
        // expected
      }
    }

    checkpoint("BEFORE_VERIFICATION");
  }
}

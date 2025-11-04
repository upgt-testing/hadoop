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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.event.Level;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileStatus}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestFileStatus Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFileStatus_ProcessBased extends ProcessBasedUpgradeTestBase {
  static {
    GenericTestUtils.setLogLevel(FSNamesystem.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(FileSystem.LOG, Level.TRACE);
  }

  static final long seed = 0xDEADBEEFL;
  static final int blockSize = 8192;
  static final int fileSize = 16384;

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_DIRECTORY_CREATE",
      "AFTER_FILE_STATUS_CHECK",
      "AFTER_LIST_STATUS",
      "BEFORE_VERIFICATION"
    );
  }

  private void checkFile(FileSystem fileSys, Path name, int repl)
      throws IOException, InterruptedException, TimeoutException {
    DFSTestUtil.waitReplication(fileSys, name, (short) repl);
  }

  /** Test calling getFileInfo directly on the client */
  @Test
  public void testGetFileInfo() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_LIST_LIMIT, 2);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    DFSClient dfsClient = new DFSClient(DFSUtilClient.getNNAddress(conf), conf);

    // Check that / exists
    Path path = new Path("/");
    assertTrue("/ should be a directory",
               fs.getFileStatus(path).isDirectory());
    ContractTestUtils.assertNotErasureCoded(fs, path);

    checkpoint("AFTER_ROOT_CHECK");

    // Make sure getFileInfo returns null for files which do not exist
    HdfsFileStatus fileInfo = dfsClient.getFileInfo("/noSuchFile");
    assertEquals("Non-existant file should result in null", null, fileInfo);

    Path path1 = new Path("/name1");
    Path path2 = new Path("/name1/name2");
    assertTrue(fs.mkdirs(path1));
    checkpoint("AFTER_DIRECTORY_CREATE");

    FSDataOutputStream out = fs.create(path2, false);
    out.close();
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    fileInfo = dfsClient.getFileInfo(path1.toString());
    assertEquals(1, fileInfo.getChildrenNum());
    fileInfo = dfsClient.getFileInfo(path2.toString());
    assertEquals(0, fileInfo.getChildrenNum());

    checkpoint("AFTER_FILE_STATUS_CHECK");

    // Test getFileInfo throws the right exception given a non-absolute path.
    try {
      dfsClient.getFileInfo("non-absolute");
      fail("getFileInfo for a non-absolute path did not throw IOException");
    } catch (RemoteException re) {
      assertTrue("Wrong exception for invalid file name: "+re,
          re.toString().contains("Absolute path required"));
    }

    checkpoint("BEFORE_VERIFICATION");

    dfsClient.close();
  }

  /** Test the FileStatus obtained calling getFileStatus on a file */
  @Test
  public void testGetFileStatusOnFile() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_LIST_LIMIT, 2);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path file1 = new Path("filestatus.dat");
    DFSTestUtil.createFile(fs, file1, fileSize, fileSize, blockSize, (short) 1, seed);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    checkFile(fs, file1, 1);
    checkpoint("AFTER_REPLICATION_CHECK");

    // test getFileStatus on a file
    FileStatus status = fs.getFileStatus(file1);
    assertFalse(file1 + " should be a file", status.isDirectory());
    assertEquals(blockSize, status.getBlockSize());
    assertEquals(1, status.getReplication());
    assertEquals(fileSize, status.getLen());
    ContractTestUtils.assertNotErasureCoded(fs, file1);
    assertEquals(file1.makeQualified(fs.getUri(),
        fs.getWorkingDirectory()).toString(),
        status.getPath().toString());
    assertTrue(file1 + " should have erasure coding unset in " +
            "FileStatus#toString(): " + status,
        status.toString().contains("isErasureCoded=false"));

    checkpoint("BEFORE_VERIFICATION");
  }

  /** Test the FileStatus obtained calling listStatus on a file */
  @Test
  public void testListStatusOnFile() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_LIST_LIMIT, 2);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    FileContext fc = FileContext.getFileContext(cluster.getURI(), conf);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path file1 = new Path("filestatus.dat");
    DFSTestUtil.createFile(fs, file1, fileSize, fileSize, blockSize, (short) 1, seed);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    FileStatus[] stats = fs.listStatus(file1);
    assertEquals(1, stats.length);
    FileStatus status = stats[0];
    assertFalse(file1 + " should be a file", status.isDirectory());
    assertEquals(blockSize, status.getBlockSize());
    assertEquals(1, status.getReplication());
    assertEquals(fileSize, status.getLen());
    ContractTestUtils.assertNotErasureCoded(fs, file1);
    assertEquals(file1.makeQualified(fs.getUri(),
        fs.getWorkingDirectory()).toString(),
        status.getPath().toString());

    checkpoint("AFTER_LIST_STATUS");

    RemoteIterator<FileStatus> itor = fc.listStatus(file1);
    status = itor.next();
    assertEquals(stats[0], status);
    assertFalse(file1 + " should be a file", status.isDirectory());

    checkpoint("BEFORE_VERIFICATION");
  }

  /** Test getting a FileStatus object using a non-existant path */
  @Test
  public void testGetFileStatusOnNonExistantFileDir() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_LIST_LIMIT, 2);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    FileContext fc = FileContext.getFileContext(cluster.getURI(), conf);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path dir = new Path("/test/mkdirs");
    try {
      fs.listStatus(dir);
      fail("listStatus of non-existent path should fail");
    } catch (FileNotFoundException fe) {
      assertEquals("File " + dir + " does not exist.",fe.getMessage());
    }

    checkpoint("AFTER_LIST_STATUS_CHECK");

    try {
      fc.listStatus(dir);
      fail("listStatus of non-existent path should fail");
    } catch (FileNotFoundException fe) {
      assertEquals("File " + dir + " does not exist.", fe.getMessage());
    }
    try {
      fs.getFileStatus(dir);
      fail("getFileStatus of non-existent path should fail");
    } catch (FileNotFoundException fe) {
      assertTrue("Exception doesn't indicate non-existant path",
          fe.getMessage().startsWith("File does not exist"));
    }

    checkpoint("BEFORE_VERIFICATION");
  }

  /** Test FileStatus objects obtained from a directory */
  @Test
  public void testGetFileStatusOnDir() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_LIST_LIMIT, 2);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    FileContext fc = FileContext.getFileContext(cluster.getURI(), conf);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create the directory
    Path dir = new Path("/test/mkdirs");
    assertTrue("mkdir failed", fs.mkdirs(dir));
    assertTrue("mkdir failed", fs.exists(dir));
    checkpoint("AFTER_DIRECTORY_CREATE");

    // test getFileStatus on an empty directory
    FileStatus status = fs.getFileStatus(dir);
    assertTrue(dir + " should be a directory", status.isDirectory());
    assertTrue(dir + " should be zero size ", status.getLen() == 0);
    ContractTestUtils.assertNotErasureCoded(fs, dir);
    assertEquals(dir.makeQualified(fs.getUri(),
        fs.getWorkingDirectory()).toString(),
        status.getPath().toString());

    checkpoint("AFTER_EMPTY_DIR_CHECK");

    // test listStatus on an empty directory
    FileStatus[] stats = fs.listStatus(dir);
    assertEquals(dir + " should be empty", 0, stats.length);
    assertEquals(dir + " should be zero size ",
        0, fs.getContentSummary(dir).getLength());

    RemoteIterator<FileStatus> itor = fc.listStatus(dir);
    assertFalse(dir + " should be empty", itor.hasNext());

    itor = fs.listStatusIterator(dir);
    assertFalse(dir + " should be empty", itor.hasNext());

    checkpoint("AFTER_EMPTY_LIST_CHECK");

    // create another file that is smaller than a block.
    Path file2 = new Path(dir, "filestatus2.dat");
    DFSTestUtil.createFile(fs, file2, blockSize/4, blockSize/4, blockSize,
        (short) 1, seed);
    checkFile(fs, file2, 1);
    checkpoint("AFTER_FILE2_CREATE");

    // verify file attributes
    status = fs.getFileStatus(file2);
    assertEquals(blockSize, status.getBlockSize());
    assertEquals(1, status.getReplication());
    file2 = fs.makeQualified(file2);
    assertEquals(file2.toString(), status.getPath().toString());

    checkpoint("AFTER_FILE2_STATUS_CHECK");

    // Create another file in the same directory
    Path file3 = new Path(dir, "filestatus3.dat");
    DFSTestUtil.createFile(fs, file3, blockSize/4, blockSize/4, blockSize,
        (short) 1, seed);
    checkFile(fs, file3, 1);
    file3 = fs.makeQualified(file3);
    checkpoint("AFTER_FILE3_CREATE");

    // Verify that the size of the directory increased by the size
    // of the two files
    final int expected = blockSize/2;
    assertEquals(dir + " size should be " + expected,
        expected, fs.getContentSummary(dir).getLength());

    checkpoint("AFTER_DIR_SIZE_CHECK");

    // Test listStatus on a non-empty directory
    stats = fs.listStatus(dir);
    assertEquals(dir + " should have two entries", 2, stats.length);
    assertEquals(file2.toString(), stats[0].getPath().toString());
    assertEquals(file3.toString(), stats[1].getPath().toString());

    itor = fc.listStatus(dir);
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());
    assertFalse("Unexpected additional file", itor.hasNext());

    itor = fs.listStatusIterator(dir);
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());
    assertFalse("Unexpected additional file", itor.hasNext());

    checkpoint("AFTER_TWO_FILES_LIST");

    // Test iterative listing. Now dir has 2 entries, create one more.
    Path dir3 = fs.makeQualified(new Path(dir, "dir3"));
    fs.mkdirs(dir3);
    dir3 = fs.makeQualified(dir3);
    checkpoint("AFTER_DIR3_CREATE");

    stats = fs.listStatus(dir);
    assertEquals(dir + " should have three entries", 3, stats.length);
    assertEquals(dir3.toString(), stats[0].getPath().toString());
    assertEquals(file2.toString(), stats[1].getPath().toString());
    assertEquals(file3.toString(), stats[2].getPath().toString());

    itor = fc.listStatus(dir);
    assertEquals(dir3.toString(), itor.next().getPath().toString());
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());
    assertFalse("Unexpected additional file", itor.hasNext());

    itor = fs.listStatusIterator(dir);
    assertEquals(dir3.toString(), itor.next().getPath().toString());
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());
    assertFalse("Unexpected additional file", itor.hasNext());

    checkpoint("AFTER_THREE_ENTRIES_LIST");

    // Now dir has 3 entries, create two more
    Path dir4 = fs.makeQualified(new Path(dir, "dir4"));
    fs.mkdirs(dir4);
    dir4 = fs.makeQualified(dir4);
    Path dir5 = fs.makeQualified(new Path(dir, "dir5"));
    fs.mkdirs(dir5);
    dir5 = fs.makeQualified(dir5);
    checkpoint("AFTER_DIR4_DIR5_CREATE");

    stats = fs.listStatus(dir);
    assertEquals(dir + " should have five entries", 5, stats.length);
    assertEquals(dir3.toString(), stats[0].getPath().toString());
    assertEquals(dir4.toString(), stats[1].getPath().toString());
    assertEquals(dir5.toString(), stats[2].getPath().toString());
    assertEquals(file2.toString(), stats[3].getPath().toString());
    assertEquals(file3.toString(), stats[4].getPath().toString());

    itor = fc.listStatus(dir);
    assertEquals(dir3.toString(), itor.next().getPath().toString());
    assertEquals(dir4.toString(), itor.next().getPath().toString());
    assertEquals(dir5.toString(), itor.next().getPath().toString());
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());

    assertFalse(itor.hasNext());

    itor = fs.listStatusIterator(dir);
    assertEquals(dir3.toString(), itor.next().getPath().toString());
    assertEquals(dir4.toString(), itor.next().getPath().toString());
    assertEquals(dir5.toString(), itor.next().getPath().toString());
    assertEquals(file2.toString(), itor.next().getPath().toString());
    assertEquals(file3.toString(), itor.next().getPath().toString());

    assertFalse(itor.hasNext());

    checkpoint("AFTER_FIVE_ENTRIES_LIST");

    itor = fs.listStatusIterator(dir);
    assertEquals(dir3.toString(), itor.next().getPath().toString());
    assertEquals(dir4.toString(), itor.next().getPath().toString());
    fs.delete(dir.getParent(), true);
    checkpoint("AFTER_DELETE");

    try {
      itor.hasNext();
      fail("FileNotFoundException expected");
    } catch (FileNotFoundException fnfe) {
      // Expected
    }

    // Recreate directories for final test
    fs.mkdirs(file2);
    fs.mkdirs(dir3);
    fs.mkdirs(dir4);
    fs.mkdirs(dir5);
    checkpoint("AFTER_RECREATE");

    itor = fs.listStatusIterator(dir);
    int count = 0;
    try {
      fs.delete(dir.getParent(), true);
      while (itor.next() != null) {
        count++;
      }
      fail("FileNotFoundException expected");
    } catch (FileNotFoundException fnfe) {
      // Expected
    }
    assertEquals(2, count);

    checkpoint("BEFORE_VERIFICATION");
  }
}

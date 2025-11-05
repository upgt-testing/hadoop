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
package org.apache.hadoop.fs.viewfs;


/**
 * ProcessBasedMiniDFSCluster version of {@link TestViewFsFileStatusHdfs}.
 *
 * The FileStatus is being serialized in MR as jobs are submitted.
 * Since viewfs has overlayed ViewFsFileStatus, we ran into
 * serialization problems. This test is test the fix.
 *
 * @see TestViewFsFileStatusHdfs Original test using MiniDFSCluster
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import javax.security.auth.login.LoginException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemTestHelper;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestViewFsFileStatusHdfs_ProcessBased extends ProcessBasedUpgradeTestBase {

  static final String testfilename = "/tmp/testFileStatusSerialziation";
  static final String someFile = "/hdfstmp/someFileForTestGetFileChecksum";

  private static final FileSystemTestHelper fileSystemTestHelper = new FileSystemTestHelper();
  private Path defaultWorkingDirectory;
  private FileSystem fHdfs;
  private FileSystem vfs;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_VIEWFS_SETUP",
      "AFTER_FILE_CREATE",
      "BEFORE_SERIALIZATION",
      "AFTER_SERIALIZATION",
      "BEFORE_CHECKSUM_VERIFY",
      "AFTER_CHECKSUM_VERIFY"
    );
  }

  @Test
  public void testFileStatusSerialziation()
      throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fHdfs = cluster.getFileSystem();
    defaultWorkingDirectory = fHdfs.makeQualified(new Path("/user/" +
        UserGroupInformation.getCurrentUser().getShortUserName()));
    fHdfs.mkdirs(defaultWorkingDirectory);

    // Setup the ViewFS to be used for all tests.
    Configuration viewConf = ViewFileSystemTestSetup.createConfig();
    ConfigUtil.addLink(viewConf, "/vfstmp", new URI(fHdfs.getUri() + "/hdfstmp"));
    ConfigUtil.addLink(viewConf, "/tmp", new URI(fHdfs.getUri() + "/tmp"));
    vfs = FileSystem.get(FsConstants.VIEWFS_URI, viewConf);
    assertEquals(ViewFileSystem.class, vfs.getClass());
    checkpoint("AFTER_VIEWFS_SETUP");

    long len = fileSystemTestHelper.createFile(fHdfs, testfilename);
    checkpoint("AFTER_FILE_CREATE");

    FileStatus stat = vfs.getFileStatus(new Path(testfilename));
    assertEquals(len, stat.getLen());
    checkpoint("BEFORE_SERIALIZATION");

    // check serialization/deserialization
    DataOutputBuffer dob = new DataOutputBuffer();
    stat.write(dob);
    DataInputBuffer dib = new DataInputBuffer();
    dib.reset(dob.getData(), 0, dob.getLength());
    FileStatus deSer = new FileStatus();
    deSer.readFields(dib);
    assertEquals(len, deSer.getLen());
    checkpoint("AFTER_SERIALIZATION");

    // Cleanup
    fHdfs.delete(new Path(testfilename), true);
  }

  @Test
  public void testGetFileChecksum() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fHdfs = cluster.getFileSystem();
    defaultWorkingDirectory = fHdfs.makeQualified(new Path("/user/" +
        UserGroupInformation.getCurrentUser().getShortUserName()));
    fHdfs.mkdirs(defaultWorkingDirectory);

    // Setup the ViewFS
    Configuration viewConf = ViewFileSystemTestSetup.createConfig();
    ConfigUtil.addLink(viewConf, "/vfstmp", new URI(fHdfs.getUri() + "/hdfstmp"));
    ConfigUtil.addLink(viewConf, "/tmp", new URI(fHdfs.getUri() + "/tmp"));
    vfs = FileSystem.get(FsConstants.VIEWFS_URI, viewConf);
    assertEquals(ViewFileSystem.class, vfs.getClass());
    checkpoint("AFTER_VIEWFS_SETUP");

    // Create two different files in HDFS
    fileSystemTestHelper.createFile(fHdfs, someFile);
    fileSystemTestHelper.createFile(fHdfs, fileSystemTestHelper
      .getTestRootPath(fHdfs, someFile + "other"), 1, 512);
    checkpoint("AFTER_FILE_CREATE");

    // Get checksum through ViewFS
    FileChecksum viewFSCheckSum = vfs.getFileChecksum(
      new Path("/vfstmp/someFileForTestGetFileChecksum"));
    checkpoint("BEFORE_CHECKSUM_VERIFY");

    // Get checksum through HDFS.
    FileChecksum hdfsCheckSum = fHdfs.getFileChecksum(
      new Path(someFile));
    // Get checksum of different file in HDFS
    FileChecksum otherHdfsFileCheckSum = fHdfs.getFileChecksum(
      new Path(someFile+"other"));

    // Checksums of the same file (got through HDFS and ViewFS should be same)
    assertEquals("HDFS and ViewFS checksums were not the same", viewFSCheckSum,
      hdfsCheckSum);
    // Checksum of different files should be different.
    assertFalse("Some other HDFS file which should not have had the same " +
      "checksum as viewFS did!", viewFSCheckSum.equals(otherHdfsFileCheckSum));
    checkpoint("AFTER_CHECKSUM_VERIFY");

    // Cleanup
    fHdfs.delete(new Path(testfilename), true);
    fHdfs.delete(new Path(someFile), true);
    fHdfs.delete(new Path(someFile + "other"), true);
  }

}

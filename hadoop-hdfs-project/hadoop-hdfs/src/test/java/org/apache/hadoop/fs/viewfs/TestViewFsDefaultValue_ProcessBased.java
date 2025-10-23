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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_REPLICATION_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_REPLICATION_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemTestHelper;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.QuotaUsage;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * Process-based version of TestViewFsDefaultValue for quota usage with storage types.
 *
 * NOTE: Only contains the testGetQuotaUsageWithStorageTypes test method.
 */
public class TestViewFsDefaultValue_ProcessBased {

  static final String testFileDir = "/tmp/test/";
  static final String testFileName = testFileDir + "testFileStatusSerialziation";
  static final String NOT_IN_MOUNTPOINT_FILENAME = "/NotInMountpointFile";
  private static ProcessBasedMiniDFSCluster cluster;
  private static final FileSystemTestHelper fileSystemTestHelper = new FileSystemTestHelper();
  private static final Configuration CONF = new HdfsConfiguration();
  private static FileSystem fHdfs;
  private static FileSystem vfs;
  private static Path testFileDirPath;
  private String hadoopHome;

  @Rule
  public Timeout globalTimeout = new Timeout(300000);

  @Before
  public void setUp() {
    hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster tests", hadoopHome);
  }

  @BeforeClass
  public static void clusterSetupAtBegining() throws IOException,
      URISyntaxException, java.util.concurrent.TimeoutException {

    CONF.setInt(DFS_REPLICATION_KEY, DFS_REPLICATION_DEFAULT + 1);

    cluster = new ProcessBasedMiniDFSCluster.Builder(CONF)
        .numDataNodes(DFS_REPLICATION_DEFAULT + 1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fHdfs = cluster.getFileSystem();
    fileSystemTestHelper.createFile(fHdfs, testFileName);
    fileSystemTestHelper.createFile(fHdfs, NOT_IN_MOUNTPOINT_FILENAME);
    Configuration conf = ViewFileSystemTestSetup.createConfig();
    conf.setInt(DFS_REPLICATION_KEY, DFS_REPLICATION_DEFAULT + 1);
    ConfigUtil.addLink(conf, "/tmp", new URI(fHdfs.getUri().toString() +
      "/tmp"));
    vfs = FileSystem.get(FsConstants.VIEWFS_URI, conf);
    testFileDirPath = new Path (testFileDir);
  }

  /**
   * Test that getQuotaUsage can be retrieved on the client side if
   * storage types are defined.
   */
  @Test
  public void testGetQuotaUsageWithStorageTypes() throws IOException {
    final DistributedFileSystem dfs = (DistributedFileSystem)fHdfs;
    dfs.setQuotaByStorageType(testFileDirPath, StorageType.SSD, 500);
    dfs.setQuotaByStorageType(testFileDirPath, StorageType.DISK, 600);
    QuotaUsage qu = vfs.getQuotaUsage(testFileDirPath);
    assertEquals(500, qu.getTypeQuota(StorageType.SSD));
    assertEquals(600, qu.getTypeQuota(StorageType.DISK));
  }

  @AfterClass
  public static void cleanup() throws IOException {
    if (fHdfs != null) {
      fHdfs.delete(new Path(testFileName), true);
      fHdfs.delete(new Path(NOT_IN_MOUNTPOINT_FILENAME), true);
    }
    if (cluster != null) {
      cluster.shutdown();
    }
  }
}

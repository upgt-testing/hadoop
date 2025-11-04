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

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileSystemTestHelper;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.QuotaUsage;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Process-based version of TestViewFsDefaultValue with parameterized upgrade checkpoints.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during ViewFS quota operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestViewFsDefaultValue_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  static final String testFileDir = "/tmp/test/";
  static final String testFileName = testFileDir + "testFileStatusSerialziation";
  static final String NOT_IN_MOUNTPOINT_FILENAME = "/NotInMountpointFile";
  private static final FileSystemTestHelper fileSystemTestHelper = new FileSystemTestHelper();

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

        // File creation
        "AFTER_FILE_SETUP",

        // Quota operations
        "AFTER_QUOTA_SETUP",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * Test that getQuotaUsage can be retrieved on the client side if
   * storage types are defined.
   */
  @Test(timeout = 300000)
  public void testGetQuotaUsageWithStorageTypes() throws Exception {
    conf.setInt(DFS_REPLICATION_KEY, DFS_REPLICATION_DEFAULT + 1);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DFS_REPLICATION_DEFAULT + 1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    fileSystemTestHelper.createFile(fs, testFileName);
    fileSystemTestHelper.createFile(fs, NOT_IN_MOUNTPOINT_FILENAME);

    ConfigUtil.addLink(conf, "/tmp", new URI(fs.getUri().toString() + "/tmp"));
    FileSystem vfs = FileSystem.get(FsConstants.VIEWFS_URI, conf);
    Path testFileDirPath = new Path(testFileDir);

    checkpoint("AFTER_FILE_SETUP");

    final DistributedFileSystem dfs = (DistributedFileSystem) fs;
    dfs.setQuotaByStorageType(testFileDirPath, StorageType.SSD, 500);
    dfs.setQuotaByStorageType(testFileDirPath, StorageType.DISK, 600);

    checkpoint("AFTER_QUOTA_SETUP");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    QuotaUsage qu = vfs.getQuotaUsage(testFileDirPath);
    assertEquals(500, qu.getTypeQuota(StorageType.SSD));
    assertEquals(600, qu.getTypeQuota(StorageType.DISK));
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

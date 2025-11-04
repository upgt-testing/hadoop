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

import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME;
import static org.apache.hadoop.fs.viewfs.Constants.CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
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

/**
 * ProcessBasedMiniDFSCluster version of {@link TestViewFileSystemOverloadSchemeHdfsFileSystemContract}
 * with parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during ViewFS operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestViewFileSystemOverloadSchemeHdfsFileSystemContract Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestViewFileSystemOverloadSchemeHdfsFileSystemContract_ProcessBased extends ProcessBasedUpgradeTestBase {

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

        // ViewFS configuration
        "AFTER_VIEWFS_SETUP",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test(expected = AccessControlException.class, timeout = 60000)
  public void testRenameRootDirForbidden() throws Exception {
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "062");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Configure ViewFileSystemOverloadScheme
    conf.set(String.format("fs.%s.impl", "hdfs"),
        ViewFileSystemOverloadScheme.class.getName());
    conf.set(String.format(
        FsConstants.FS_VIEWFS_OVERLOAD_SCHEME_TARGET_FS_IMPL_PATTERN,
        "hdfs"),
        DistributedFileSystem.class.getName());
    conf.setBoolean(CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME,
        CONFIG_VIEWFS_IGNORE_PORT_IN_MOUNT_TABLE_NAME_DEFAULT);

    URI defaultFSURI =
        URI.create(conf.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY));
    String defaultWorkingDirectory =
        "/user/" + UserGroupInformation.getCurrentUser().getShortUserName();

    ConfigUtil.addLink(conf, defaultFSURI.getAuthority(), "/user",
        defaultFSURI);
    ConfigUtil.addLink(conf, defaultFSURI.getAuthority(),
        "/FileSystemContractBaseTest/",
        new URI(defaultFSURI.toString() + "/FileSystemContractBaseTest/"));

    FileSystem viewFs = FileSystem.get(conf);

    checkpoint("AFTER_VIEWFS_SETUP");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // Test that renaming root directory is forbidden
    // This should throw AccessControlException
    viewFs.rename(new Path("/"), new Path("/testRenameRootDirForbidden"));
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestHDFSFileSystemContract}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests HDFS FileSystem contract compliance, specifically append functionality.
 * Note: Only explicit test methods from the original class are transformed;
 * inherited test methods from FileSystemContractBaseTest are not included.
 *
 * @see TestHDFSFileSystemContract Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestHDFSFileSystemContract_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_APPEND_TEST",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test
  public void testAppend() throws Exception {
    conf = new HdfsConfiguration();
    conf.set(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY, "000");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    String defaultWorkingDirectory = "/user/" +
        UserGroupInformation.getCurrentUser().getShortUserName();

    // Ensure working directory exists
    Path workDir = new Path(defaultWorkingDirectory);
    if (!fs.exists(workDir)) {
      fs.mkdirs(workDir);
    }

    // Test append functionality
    AppendTestUtil.testAppend(fs, new Path("/testAppend/f"));

    checkpoint("AFTER_APPEND_TEST");
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

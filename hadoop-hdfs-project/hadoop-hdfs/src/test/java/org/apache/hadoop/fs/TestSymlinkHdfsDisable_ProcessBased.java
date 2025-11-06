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
package org.apache.hadoop.fs;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSymlinkHdfsDisable}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests that symlink resolution can be disabled via configuration.
 *
 * @see TestSymlinkHdfsDisable Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSymlinkHdfsDisable_ProcessBased extends ProcessBasedUpgradeTestBase {

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
      "AFTER_SYMLINK_CREATE",
      "BEFORE_CLEANUP"
    );
  }

  @Test(timeout=60000)
  public void testSymlinkHdfsDisable() throws Exception {
    conf = new HdfsConfiguration();
    // disable symlink resolution
    conf.setBoolean(
        CommonConfigurationKeys.FS_CLIENT_RESOLVE_REMOTE_SYMLINKS_KEY, false);
    // spin up minicluster, get dfs and filecontext
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    FileContext fc = FileContext.getFileContext(cluster.getURI(0), conf);
    // Create test files/links
    FileContextTestHelper helper = new FileContextTestHelper(
        "/tmp/TestSymlinkHdfsDisable_ProcessBased");
    Path root = helper.getTestRootPath(fc);
    Path target = new Path(root, "target");
    Path link = new Path(root, "link");
    DFSTestUtil.createFile(fs, target, 4096, (short)1, 0xDEADDEAD);
    fc.createSymlink(target, link, false);

    checkpoint("AFTER_SYMLINK_CREATE");

    // Try to resolve links with FileSystem and FileContext
    try {
      fc.open(link);
      fail("Expected error when attempting to resolve link");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains("resolution is disabled", e);
    }
    try {
      fs.open(link);
      fail("Expected error when attempting to resolve link");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains("resolution is disabled", e);
    }

    checkpoint("BEFORE_CLEANUP");
  }
}

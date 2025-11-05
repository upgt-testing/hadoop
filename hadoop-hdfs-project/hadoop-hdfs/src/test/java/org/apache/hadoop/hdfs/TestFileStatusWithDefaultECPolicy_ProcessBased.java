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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileStatusWithDefaultECPolicy}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This test ensures the statuses of EC files with the default policy.
 *
 * @see TestFileStatusWithDefaultECPolicy Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFileStatusWithDefaultECPolicy_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_EC_POLICY_ENABLED",
      "AFTER_DIR_CREATE",
      "AFTER_FILE_CREATE_WITHOUT_EC",
      "AFTER_FILE_DELETE",
      "AFTER_EC_POLICY_SET_ON_DIR",
      "AFTER_FILE_CREATE_WITH_EC",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  public ErasureCodingPolicy getEcPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  @Test(timeout=300000)
  public void testFileStatusWithECPolicy() throws Exception {
    HdfsConfiguration conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    DFSClient client = dfs.getClient();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    dfs.enableErasureCodingPolicy(getEcPolicy().getName());

    checkpoint("AFTER_EC_POLICY_ENABLED");

    // test directory doesn't have an EC policy
    final Path dir = new Path("/foo");
    assertTrue(dfs.mkdir(dir, FsPermission.getDirDefault()));
    ContractTestUtils.assertNotErasureCoded(dfs, dir);
    assertNull(client.getFileInfo(dir.toString()).getErasureCodingPolicy());

    checkpoint("AFTER_DIR_CREATE");

    // test file doesn't have an EC policy
    final Path file = new Path(dir, "foo");
    dfs.create(file).close();
    assertNull(client.getFileInfo(file.toString()).getErasureCodingPolicy());
    ContractTestUtils.assertNotErasureCoded(dfs, file);

    checkpoint("AFTER_FILE_CREATE_WITHOUT_EC");

    dfs.delete(file, true);

    checkpoint("AFTER_FILE_DELETE");

    final ErasureCodingPolicy ecPolicy1 = getEcPolicy();
    // set EC policy on dir
    dfs.setErasureCodingPolicy(dir, ecPolicy1.getName());
    ContractTestUtils.assertErasureCoded(dfs, dir);
    final ErasureCodingPolicy ecPolicy2 =
        client.getFileInfo(dir.toUri().getPath()).getErasureCodingPolicy();
    assertNotNull(ecPolicy2);
    assertTrue(ecPolicy1.equals(ecPolicy2));

    checkpoint("AFTER_EC_POLICY_SET_ON_DIR");

    // test file with EC policy
    dfs.create(file).close();
    final ErasureCodingPolicy ecPolicy3 =
        dfs.getClient().getFileInfo(file.toUri().getPath())
            .getErasureCodingPolicy();
    assertNotNull(ecPolicy3);
    assertTrue(ecPolicy1.equals(ecPolicy3));
    ContractTestUtils.assertErasureCoded(dfs, file);

    checkpoint("AFTER_FILE_CREATE_WITH_EC");

    FileStatus status = dfs.getFileStatus(file);
    assertTrue(file + " should have erasure coding set in " +
            "FileStatus#toString(): " + status,
        status.toString().contains("isErasureCoded=true"));

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // Cleanup handled by @After in base class
  }
}

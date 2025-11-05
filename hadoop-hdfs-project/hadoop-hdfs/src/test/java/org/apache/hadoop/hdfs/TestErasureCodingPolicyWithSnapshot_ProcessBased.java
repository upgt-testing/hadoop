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
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.hdfs.protocol.SystemErasureCodingPolicies;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestErasureCodingPolicyWithSnapshot}.
 *
 * Tests erasure coding policy behavior with snapshots, including policy
 * changes, NameNode restarts, and snapshot operations.
 *
 * @see TestErasureCodingPolicyWithSnapshot Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestErasureCodingPolicyWithSnapshot_ProcessBased extends ProcessBasedUpgradeTestBase {

  private DistributedFileSystem dfs;
  private final static int SUCCESS = 0;
  private ErasureCodingPolicy ecPolicy;
  private short groupSize;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_ECDIR_CREATE",
      "AFTER_SNAPSHOT_ALLOWED",
      "AFTER_EC_POLICY_SET",
      "AFTER_FIRST_SNAPSHOT",
      "AFTER_DIR_DELETE",
      "AFTER_DIR_RECREATE",
      "AFTER_SECOND_SNAPSHOT",
      UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  private ErasureCodingPolicy getEcPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  /**
   * Test correctness of successive snapshot creation and deletion with erasure
   * coding policies. Create snapshot of ecDir's parent directory.
   */
  @Test(timeout = 120000)
  public void testSnapshotsOnErasureCodingDirsParentDir() throws Exception {
    setupCluster();

    final int len = 1024;
    final Path ecDirParent = new Path("/parent");
    final Path ecDir = new Path(ecDirParent, "ecdir");
    final Path ecFile = new Path(ecDir, "ecfile");

    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDirParent);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    // set erasure coding policy
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    DFSTestUtil.createFile(dfs, ecFile, len, (short) 1, 0xFEED);
    String contents = DFSTestUtil.readFile(dfs, ecFile);

    final Path snap1 = dfs.createSnapshot(ecDirParent, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    final Path snap1ECDir = new Path(snap1, ecDir.getName());
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap1ECDir));

    // Now delete the dir which has erasure coding policy
    dfs.delete(ecDir, true);
    checkpoint("AFTER_DIR_DELETE");

    // Re-create the dir again, and take another snapshot
    dfs.mkdir(ecDir, FsPermission.getDirDefault());
    checkpoint("AFTER_DIR_RECREATE");

    final Path snap2 = dfs.createSnapshot(ecDirParent, "snap2");
    checkpoint("AFTER_SECOND_SNAPSHOT");

    final Path snap2ECDir = new Path(snap2, ecDir.getName());
    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap2ECDir));

    // Make dir again with system default ec policy
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    final Path snap3 = dfs.createSnapshot(ecDirParent, "snap3");
    final Path snap3ECDir = new Path(snap3, ecDir.getName());

    // Check that snap3's ECPolicy has the correct settings
    ErasureCodingPolicy ezSnap3 = dfs.getErasureCodingPolicy(snap3ECDir);
    assertEquals("Got unexpected erasure coding policy", ecPolicy, ezSnap3);

    // Check that older snapshots still have the old ECPolicy settings
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap1ECDir));
    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap2ECDir));

    // Verify contents of the snapshotted file
    final Path snapshottedECFile = new Path(snap1.toString() + "/"
        + ecDir.getName() + "/" + ecFile.getName());
    assertEquals("Contents of snapshotted file have changed unexpectedly",
        contents, DFSTestUtil.readFile(dfs, snapshottedECFile));

    // Now delete the snapshots out of order and verify the EC policy correctness
    dfs.deleteSnapshot(ecDirParent, snap2.getName());
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap1ECDir));
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap3ECDir));

    dfs.deleteSnapshot(ecDirParent, snap1.getName());
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap3ECDir));

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  /**
   * Test creation of snapshot on directory has erasure coding policy.
   */
  @Test(timeout = 120000)
  public void testSnapshotsOnErasureCodingDir() throws Exception {
    setupCluster();

    final Path ecDir = new Path("/ecdir");
    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDir);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    final Path snap1 = dfs.createSnapshot(ecDir, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap1));

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  /**
   * Test verify erasure coding policy is present after restarting the NameNode.
   */
  @Test(timeout = 120000)
  public void testSnapshotsOnErasureCodingDirAfterNNRestart() throws Exception {
    setupCluster();

    final Path ecDir = new Path("/ecdir");
    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDir);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    // set erasure coding policy
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    final Path snap1 = dfs.createSnapshot(ecDir, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    ErasureCodingPolicy ecSnap = dfs.getErasureCodingPolicy(snap1);
    assertEquals("Got unexpected erasure coding policy", ecPolicy, ecSnap);

    // save namespace, restart namenode, and check ec policy correctness
    dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
    dfs.saveNamespace();
    dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
    checkpoint("BEFORE_NN_RESTART");

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    checkpoint("AFTER_NN_RESTART");

    ErasureCodingPolicy ecSnap1 = dfs.getErasureCodingPolicy(snap1);
    assertEquals("Got unexpected erasure coding policy", ecPolicy, ecSnap1);
    assertEquals("Got unexpected ecSchema", ecSnap.getSchema(),
        ecSnap1.getSchema());

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  /**
   * Test copy a snapshot will not preserve its erasure coding policy info.
   */
  @Test(timeout = 120000)
  public void testCopySnapshotWillNotPreserveErasureCodingPolicy() throws Exception {
    setupCluster();

    final int len = 1024;
    final Path ecDir = new Path("/ecdir");
    final Path ecFile = new Path(ecDir, "ecFile");

    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDir);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    // set erasure coding policy
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    DFSTestUtil.createFile(dfs, ecFile, len, (short) 1, 0xFEED);
    final Path snap1 = dfs.createSnapshot(ecDir, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    Path snap1Copy = new Path(ecDir.toString() + "-copy");
    final Path snap1CopyECDir = new Path("/ecdir-copy");

    String[] argv = new String[] { "-cp", "-px", snap1.toUri().toString(),
        snap1Copy.toUri().toString() };
    int ret = ToolRunner.run(new FsShell(conf), argv);
    assertEquals("cp -px is not working on a snapshot", SUCCESS, ret);
    checkpoint("AFTER_SNAPSHOT_COPY");

    assertNull("Got unexpected erasure coding policy",
        dfs.getErasureCodingPolicy(snap1CopyECDir));
    assertEquals("Got unexpected erasure coding policy", ecPolicy,
        dfs.getErasureCodingPolicy(snap1));

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  @Test(timeout = 300000)
  public void testFileStatusAcrossNNRestart() throws Exception {
    setupCluster();

    final int len = 1024;
    final Path normalFile = new Path("/", "normalFile");
    DFSTestUtil.createFile(dfs, normalFile, len, (short) 1, 0xFEED);
    checkpoint("AFTER_NORMAL_FILE_CREATE");

    final Path ecDir = new Path("/ecdir");
    final Path ecFile = new Path(ecDir, "ecFile");
    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    // Set erasure coding policy
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    DFSTestUtil.createFile(dfs, ecFile, len, (short) 1, 0xFEED);
    checkpoint("AFTER_EC_FILE_CREATE");

    // Verify FileStatus for normal and EC files
    ContractTestUtils.assertNotErasureCoded(dfs, normalFile);
    ContractTestUtils.assertErasureCoded(dfs, ecFile);
    checkpoint("BEFORE_NN_RESTART");

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    checkpoint("AFTER_NN_RESTART");

    // Verify FileStatus for normal and EC files after restart
    ContractTestUtils.assertNotErasureCoded(dfs, normalFile);
    ContractTestUtils.assertErasureCoded(dfs, ecFile);

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  @Test(timeout = 120000)
  public void testErasureCodingPolicyOnDotSnapshotDir() throws Exception {
    setupCluster();

    final Path ecDir = new Path("/ecdir");
    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDir);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    // set erasure coding policy and create snapshot
    dfs.setErasureCodingPolicy(ecDir, ecPolicy.getName());
    checkpoint("AFTER_EC_POLICY_SET");

    final Path snap = dfs.createSnapshot(ecDir, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    // verify the EC policy correctness
    ErasureCodingPolicy ecSnap = dfs.getErasureCodingPolicy(snap);
    assertEquals("Got unexpected erasure coding policy", ecPolicy, ecSnap);

    // verify the EC policy is null, not an exception
    final Path ecDotSnapshotDir = new Path(ecDir, ".snapshot");
    ErasureCodingPolicy ecSnap1 = dfs.getErasureCodingPolicy(ecDotSnapshotDir);
    assertNull("Got unexpected erasure coding policy", ecSnap1);

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  /**
   * Test creation of snapshot on directory which changes its
   * erasure coding policy.
   */
  @Test(timeout = 120000)
  public void testSnapshotsOnErasureCodingDirAfterECPolicyChanges() throws Exception {
    setupCluster();

    final Path ecDir = new Path("/ecdir");
    dfs.mkdirs(ecDir);
    checkpoint("AFTER_ECDIR_CREATE");

    dfs.allowSnapshot(ecDir);
    checkpoint("AFTER_SNAPSHOT_ALLOWED");

    final Path snap1 = dfs.createSnapshot(ecDir, "snap1");
    checkpoint("AFTER_FIRST_SNAPSHOT");

    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap1));

    // Set erasure coding policy
    final ErasureCodingPolicy ec63Policy = SystemErasureCodingPolicies
        .getByID(SystemErasureCodingPolicies.RS_6_3_POLICY_ID);
    dfs.setErasureCodingPolicy(ecDir, ec63Policy.getName());
    checkpoint("AFTER_EC63_POLICY_SET");

    final Path snap2 = dfs.createSnapshot(ecDir, "snap2");
    checkpoint("AFTER_SECOND_SNAPSHOT");

    assertEquals("Got unexpected erasure coding policy", ec63Policy,
        dfs.getErasureCodingPolicy(snap2));

    // Verify the EC policy correctness after the unset operation
    dfs.unsetErasureCodingPolicy(ecDir);
    checkpoint("AFTER_EC_POLICY_UNSET");

    final Path snap3 = dfs.createSnapshot(ecDir, "snap3");
    checkpoint("AFTER_THIRD_SNAPSHOT");

    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap3));

    // Change the erasure coding policy and take another snapshot
    final ErasureCodingPolicy ec32Policy = SystemErasureCodingPolicies
        .getByID(SystemErasureCodingPolicies.RS_3_2_POLICY_ID);
    dfs.enableErasureCodingPolicy(ec32Policy.getName());
    dfs.setErasureCodingPolicy(ecDir, ec32Policy.getName());
    checkpoint("AFTER_EC32_POLICY_SET");

    final Path snap4 = dfs.createSnapshot(ecDir, "snap4");
    checkpoint("AFTER_FOURTH_SNAPSHOT");

    assertEquals("Got unexpected erasure coding policy", ec32Policy,
        dfs.getErasureCodingPolicy(snap4));

    // Check that older snapshot still have the old ECPolicy settings
    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap1));
    assertEquals("Got unexpected erasure coding policy", ec63Policy,
        dfs.getErasureCodingPolicy(snap2));
    assertNull("Expected null erasure coding policy",
        dfs.getErasureCodingPolicy(snap3));

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
  }

  private void setupCluster() throws Exception {
    ecPolicy = getEcPolicy();
    groupSize = (short) (ecPolicy.getNumDataUnits() + ecPolicy.getNumParityUnits());

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(groupSize)
        .format(true)
        .build();
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    dfs.enableErasureCodingPolicy(ecPolicy.getName());

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
  }
}

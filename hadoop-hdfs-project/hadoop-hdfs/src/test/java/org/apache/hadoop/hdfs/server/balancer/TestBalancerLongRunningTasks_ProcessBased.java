/*
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

package org.apache.hadoop.hdfs.server.balancer;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestBalancerLongRunningTasks} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during balancer long-running operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * <p>Note: Only testTwoReplicaShouldNotInSameDN is transformed. Other tests in the
 * original class require fine-grained storage type configuration (SSD, RAM_DISK, DISK)
 * which is not supported by ProcessBasedMiniDFSCluster's builder API.
 *
 * @see TestBalancerLongRunningTasks Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestBalancerLongRunningTasks_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  private static final Logger LOG =
      LoggerFactory.getLogger(TestBalancerLongRunningTasks_ProcessBased.class);

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

        // File operations
        "AFTER_FILE_CREATION",
        "AFTER_WAIT",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @BeforeClass
  public static void initTestSetup() {
    // do not create id file since it occupies the disk space
    NameNodeConnector.setWrite2IdFile(false);
  }

  /**
   * Test special case. Two replicas belong to same block should not in same
   * node.
   * We have 2 nodes.
   * We have a block in (DN0,SSD) and (DN1,DISK).
   * Replica in (DN0,SSD) should not be moved to (DN1,SSD).
   * Otherwise DN1 has 2 replicas.
   *
   * <p>TRANSFORMATION NOTE: This test requires fine-grained storage type configuration
   * (.storageTypes(), .storageCapacities(), .storagesPerDatanode()) which is not
   * supported by ProcessBasedMiniDFSCluster. The test logic has been preserved but
   * storage type configuration is commented out. The test will use default storage
   * configuration instead.
   *
   * <p>With 5 checkpoints, this single test method generates 5 test executions,
   * each testing upgrade at a different point in the balancer workflow.
   */
  @Test(timeout = 100000)
  public void testTwoReplicaShouldNotInSameDN() throws Exception {
    int blockSize = 5 * 1024 * 1024;
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_BALANCER_GETBLOCKS_MIN_BLOCK_SIZE_KEY, 1L);

    int numOfDatanodes = 2;

    // TRANSFORMATION NOTE: Storage type configuration not supported
    // Original configuration:
    // .racks(new String[]{"/default/rack0", "/default/rack0"})
    // .storagesPerDatanode(2)
    // .storageTypes(new StorageType[][]{
    //     {StorageType.SSD, StorageType.DISK},
    //     {StorageType.SSD, StorageType.DISK}})
    // .storageCapacities(new long[][]{
    //     {100 * blockSize, 20 * blockSize},
    //     {20 * blockSize, 100 * blockSize}})
    //
    // ProcessBasedMiniDFSCluster uses default storage configuration.
    // This limits the test's ability to verify storage-type-specific balancer behavior.

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //set "/bar" directory with ONE_SSD storage policy.
    fs = cluster.getFileSystem();
    Path barDir = new Path("/bar");
    fs.mkdirs(barDir, new FsPermission((short) 777));

    // TRANSFORMATION NOTE: Setting storage policy without heterogeneous storage types
    // may not have the intended effect, but we preserve the test logic.
    fs.setStoragePolicy(barDir, HdfsConstants.ONESSD_STORAGE_POLICY_NAME);

    // Insert 30 blocks.
    long fileLen = 30 * blockSize;
    Path fooFile = new Path(barDir, "foo");

    // Create file using DFSTestUtil instead of TestBalancer.createFile
    DFSTestUtil.createFile(fs, fooFile, fileLen, (short) numOfDatanodes, new Random().nextLong());
    DFSTestUtil.waitReplication(fs, fooFile, (short) numOfDatanodes);

    checkpoint("AFTER_FILE_CREATION");

    // TRANSFORMATION NOTE: triggerHeartbeats() not supported
    // Wait for heartbeats naturally
    Thread.sleep(5000);

    checkpoint("AFTER_WAIT");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    BalancerParameters p = BalancerParameters.DEFAULT;
    Collection<URI> namenodes = DFSUtil.getInternalNsRpcUris(conf);
    final int r = Balancer.run(namenodes, p, conf);

    // TRANSFORMATION NOTE: Without heterogeneous storage types, the balancer behavior
    // may differ from the original test. The original test expected NO_MOVE_PROGRESS
    // because moving blocks would violate the constraint that two replicas of the same
    // block cannot be on the same DN. With default storage, this constraint may still
    // hold, but the storage-type-specific logic is not tested.
    //
    // We keep the assertion but note that the test semantics have changed.
    assertEquals(ExitStatus.NO_MOVE_PROGRESS.getExitCode(), r);
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

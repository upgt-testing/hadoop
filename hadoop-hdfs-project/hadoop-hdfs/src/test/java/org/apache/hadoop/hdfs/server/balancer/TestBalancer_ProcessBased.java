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
package org.apache.hadoop.hdfs.server.balancer;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.balancer.BalancerParameters;
import org.apache.hadoop.hdfs.server.balancer.ExitStatus;
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
 * ProcessBasedMiniDFSCluster version of {@link TestBalancer} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during balancer operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestBalancer Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestBalancer_ProcessBased extends ProcessBasedUpgradeTestBase {

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

        // File operations
        "AFTER_FILE_CREATION",
        "AFTER_WAIT",

        // Balancer operations
        "AFTER_PREPARE_UPGRADE",
        "AFTER_FIRST_BALANCER_RUN",
        "AFTER_SECOND_BALANCER_RUN",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * Check that the balancer exits when there is an unfinalized upgrade.
   *
   * <p>TRANSFORMATION NOTE: This test has been modified from the original.
   * The original test dynamically adds a DataNode using cluster.startDataNodes(),
   * which is not supported by ProcessBasedMiniDFSCluster. Instead, we start with
   * 2 DataNodes from the beginning. This changes the test semantics slightly,
   * but still tests the core functionality of balancer behavior during rolling upgrade.
   *
   * <p>With 8 checkpoints, this single test method generates 8 test executions,
   * each testing upgrade at a different point in the balancer workflow.
   */
  @Test(timeout=300000)
  public void testBalancerDuringUpgrade() throws Exception {
    final int SEED = 0xFADED;
    conf.setLong(DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 500);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1);
    conf.setLong(DFSConfigKeys.DFS_BALANCER_GETBLOCKS_MIN_BLOCK_SIZE_KEY, 1L);

    final int BLOCK_SIZE = 1024*1024;

    // TRANSFORMATION: Start with 2 DataNodes instead of 1, then adding another.
    // ProcessBasedMiniDFSCluster doesn't support storageCapacities/storageTypes/storagesPerDatanode
    cluster = new ProcessBasedMiniDFSCluster
        .Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create a file on the DN
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    final Path path1 = new Path("/" + METHOD_NAME + ".01.dat");

    fs = cluster.getFileSystem();
    DFSTestUtil.createFile(fs, path1, BLOCK_SIZE, BLOCK_SIZE * 2, BLOCK_SIZE,
        (short) 1, SEED);

    checkpoint("AFTER_FILE_CREATION");

    // TRANSFORMATION NOTE: Removed cluster.startDataNodes() and cluster.triggerHeartbeats()
    // as they are not supported. The cluster already has 2 DataNodes.
    // Wait for heartbeats naturally
    Thread.sleep(3000);

    checkpoint("AFTER_WAIT");

    Collection<URI> namenodes = DFSUtil.getInternalNsRpcUris(conf);

    // Run balancer
    final BalancerParameters p = BalancerParameters.DEFAULT;

    fs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_ENTER);
    fs.rollingUpgrade(HdfsConstants.RollingUpgradeAction.PREPARE);
    fs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_LEAVE);

    checkpoint("AFTER_PREPARE_UPGRADE");

    // Rolling upgrade should abort the balancer
    assertEquals(ExitStatus.UNFINALIZED_UPGRADE.getExitCode(),
        Balancer.run(namenodes, p, conf));

    checkpoint("AFTER_FIRST_BALANCER_RUN");

    // Should work with the -runDuringUpgrade flag.
    BalancerParameters.Builder b =
        new BalancerParameters.Builder();
    b.setRunDuringUpgrade(true);
    final BalancerParameters runDuringUpgrade = b.build();
    assertEquals(ExitStatus.SUCCESS.getExitCode(),
        Balancer.run(namenodes, runDuringUpgrade, conf));

    checkpoint("AFTER_SECOND_BALANCER_RUN");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // Finalize the rolling upgrade
    fs.rollingUpgrade(HdfsConstants.RollingUpgradeAction.FINALIZE);

    // Should also work after finalization.
    assertEquals(ExitStatus.SUCCESS.getExitCode(),
        Balancer.run(namenodes, p, conf));
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

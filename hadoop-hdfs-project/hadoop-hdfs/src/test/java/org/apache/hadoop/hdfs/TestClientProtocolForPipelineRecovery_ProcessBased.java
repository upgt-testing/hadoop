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

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestClientProtocolForPipelineRecovery}
 * with parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during pipeline recovery.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestClientProtocolForPipelineRecovery Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestClientProtocolForPipelineRecovery_ProcessBased extends ProcessBasedUpgradeTestBase {

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

        // File creation
        UpgradeCheckpoints.AFTER_FILE_CREATE,

        // Read operations
        "BEFORE_READ",
        "AFTER_INPUT_OPEN",
        UpgradeCheckpoints.AFTER_READ,
        UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  /**
   * Test recovery on restart OOB message. It also tests the delivery of
   * OOB ack originating from the primary datanode. Since there is only
   * one node in the cluster, failure of restart-recovery will fail the
   * test.
   *
   * <p>With 7 checkpoints, this single test method generates 7 test executions,
   * each testing upgrade at a different point in the pipeline recovery workflow.
   *
   * @throws Exception if test fails
   */
  @Test(timeout=90000)
  public void testPipelineRecoveryForLastBlock() throws Exception {
    DFSClientFaultInjector faultInjector
        = Mockito.mock(DFSClientFaultInjector.class);
    DFSClientFaultInjector oldInjector = DFSClientFaultInjector.get();
    DFSClientFaultInjector.set(faultInjector);

    try {
      conf.setInt(HdfsClientConfigKeys.BlockWrite.LOCATEFOLLOWINGBLOCK_RETRIES_KEY, 3);

      int numDataNodes = 3;
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(numDataNodes)
          .format(true)
          .build();
      cluster.waitClusterUp();

      checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

      fs = cluster.getFileSystem();

      Path file = new Path("dataprotocol1.dat");
      Mockito.when(faultInjector.failPacket()).thenReturn(true);
      DFSTestUtil.createFile(fs, file, 68000000L, (short)numDataNodes, 0L);

      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      checkpoint("BEFORE_READ");

      // At this point, NN should have accepted only valid replicas.
      // Read should succeed.
      FSDataInputStream in = fs.open(file);

      checkpoint("AFTER_INPUT_OPEN");

      try {
        in.read();

        checkpoint(UpgradeCheckpoints.AFTER_READ);
        // Test will fail with BlockMissingException if NN does not update the
        // replica state based on the latest report.
      } catch (org.apache.hadoop.hdfs.BlockMissingException bme) {
        Assert.fail("Block is missing because the file was closed with"
            + " corrupt replicas.");
      }

      checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
    } finally {
      DFSClientFaultInjector.set(oldInjector);
      // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
    }
  }
}

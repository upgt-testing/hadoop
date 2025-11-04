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
package org.apache.hadoop.hdfs.server.namenode;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSUtilClient;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestINodeFile} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during INode path operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestINodeFile Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestINodeFile_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG = LoggerFactory.getLogger(TestINodeFile_ProcessBased.class);

  /**
   * Define upgrade checkpoints for parameterized test execution.
   *
   * @return collection of checkpoint names
   */
  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        // Baseline - no upgrade
        UpgradeCheckpoints.NO_UPGRADE,

        // Cluster lifecycle
        UpgradeCheckpoints.AFTER_CLUSTER_START,

        // Directory operations
        "AFTER_DIR_CREATION",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * Test INode path resolution using dotdot (..) references.
   *
   * <p>With 4 checkpoints, this single test method generates 4 test executions,
   * each testing upgrade at a different point in the INode path workflow.
   */
  @Test
  public void testDotdotInodePath() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final DistributedFileSystem hdfs = cluster.getFileSystem();

    // TRANSFORMATION: Instead of accessing FSDirectory directly,
    // use DFSClient API to get inode IDs from HdfsFileStatus
    final Path dir = new Path("/dir");
    hdfs.mkdirs(dir);

    DFSClient client = new DFSClient(DFSUtilClient.getNNAddress(conf), conf);

    // Get inode ID via client API (HdfsFileStatus has getFileId())
    long dirId = client.getFileInfo(dir.toString()).getFileId();
    long parentId = client.getFileInfo("/").getFileId();

    checkpoint("AFTER_DIR_CREATION");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    String testPath = "/.reserved/.inodes/" + dirId + "/..";
    HdfsFileStatus status = client.getFileInfo(testPath);
    assertTrue(parentId == status.getFileId());

    // Test root's parent is still root
    testPath = "/.reserved/.inodes/" + parentId + "/..";
    status = client.getFileInfo(testPath);
    assertTrue(parentId == status.getFileId());

    IOUtils.cleanupWithLogger(LOG, client);
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

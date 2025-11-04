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

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.NameNodeProxies;
import org.apache.hadoop.hdfs.StripedFileTestUtil;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of TestStripedINodeFile with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during striped file operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * <p>NOTE: Only contains the testUnsuitableStoragePoliciesWithECStripedMode test
 * which was blocked waiting for .storageTypes() support in ProcessBasedMiniDFSCluster.
 *
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestStripedINodeFile_ProcessBased extends ProcessBasedUpgradeTestBase {

  // use hard coded policy - see HDFS-9816
  private static final ErasureCodingPolicy testECPolicy
      = StripedFileTestUtil.getDefaultECPolicy();

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

        // File operations
        "AFTER_FILE_CREATION",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @BeforeClass
  public static void init() throws IOException {
    ErasureCodingPolicyManager.getInstance().init(new org.apache.hadoop.hdfs.HdfsConfiguration());
  }

  /**
   * Test unsuitable storage policies with EC striped mode.
   *
   * <p>This test verifies that when an unsuitable storage policy (ONE_SSD) is set on an
   * erasure-coded directory, blocks are still stored on DISK type storage.
   *
   * <p>With 4 checkpoints, this single test method generates 4 test executions,
   * each testing upgrade at a different point in the striped file workflow.
   */
  @Test(timeout = 300000)
  public void testUnsuitableStoragePoliciesWithECStripedMode()
      throws Exception {
    int defaultStripedBlockSize = testECPolicy.getCellSize() * 4;
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, defaultStripedBlockSize);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        1L);

    // start 10 datanodes
    int numOfDatanodes = 10;
    int storagesPerDatanode = 2;

    // NOTE: .storageCapacities() is not supported in ProcessBasedMiniDFSCluster,
    // so we skip setting capacities. The test should still work as it's testing
    // storage type selection, not capacity.

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numOfDatanodes)
        .storagesPerDatanode(storagesPerDatanode)
        .storageTypes(
            new StorageType[][] { { StorageType.SSD, StorageType.DISK },
                { StorageType.SSD, StorageType.DISK },
                { StorageType.SSD, StorageType.DISK },
                { StorageType.SSD, StorageType.DISK },
                { StorageType.SSD, StorageType.DISK },
                { StorageType.DISK, StorageType.SSD },
                { StorageType.DISK, StorageType.SSD },
                { StorageType.DISK, StorageType.SSD },
                { StorageType.DISK, StorageType.SSD },
                { StorageType.DISK, StorageType.SSD } })
        .format(true)
        .build();

    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    cluster.getFileSystem().enableErasureCodingPolicy(
        StripedFileTestUtil.getDefaultECPolicy().getName());

    // set "/foo" directory with ONE_SSD storage policy.
    ClientProtocol client = NameNodeProxies.createProxy(conf,
        cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
    String fooDir = "/foo";
    client.mkdirs(fooDir, new FsPermission((short) 777), true);
    client.setStoragePolicy(fooDir, HdfsConstants.ONESSD_STORAGE_POLICY_NAME);
    // set an EC policy on "/foo" directory
    client.setErasureCodingPolicy(fooDir,
        StripedFileTestUtil.getDefaultECPolicy().getName());

    // write file to fooDir
    final String barFile = "/foo/bar";
    long fileLen = 20 * defaultStripedBlockSize;
    DFSTestUtil.createFile(cluster.getFileSystem(), new Path(barFile),
        fileLen, (short) 3, 0);

    checkpoint("AFTER_FILE_CREATION");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // verify storage types and locations
    LocatedBlocks locatedBlocks = client.getBlockLocations(barFile, 0,
        fileLen);
    for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
      for (StorageType type : lb.getStorageTypes()) {
        Assert.assertEquals(StorageType.DISK, type);
      }
    }
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

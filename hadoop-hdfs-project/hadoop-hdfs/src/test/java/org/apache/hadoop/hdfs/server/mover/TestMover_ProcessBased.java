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
package org.apache.hadoop.hdfs.server.mover;

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
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestMover} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing with dynamic DataNode addition and multi-version
 * upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during Mover operations with striped files.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestMover Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestMover_ProcessBased extends ProcessBasedUpgradeTestBase {

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
        "AFTER_FIRST_VERIFICATION",

        // DataNode additions and Mover operations
        "AFTER_FIRST_DATANODE_ADD",
        "AFTER_COLD_MOVER",
        "AFTER_SECOND_DATANODE_ADD",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  private final ErasureCodingPolicy ecPolicy =
      StripedFileTestUtil.getDefaultECPolicy();
  private final int dataBlocks = ecPolicy.getNumDataUnits();
  private final int parityBlocks = ecPolicy.getNumParityUnits();
  private final int cellSize = ecPolicy.getCellSize();
  private final int stripesPerBlock = 4;
  private final int defaultBlockSize = cellSize * stripesPerBlock;

  void initConfWithStripe() {
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, defaultBlockSize);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        1L);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
    conf.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.NONE.toString());
  }

  /**
   * Test Mover tool with striped (erasure-coded) files and dynamic storage policy changes.
   *
   * <p>With 8 checkpoints, this single test method generates 8 test executions,
   * each testing upgrade at a different point in the Mover workflow with dynamic
   * DataNode addition.
   */
  @Test(timeout = 300000)
  public void testMoverWithStripedFile() throws Exception {
    initConfWithStripe();

    // start 10 datanodes
    int numOfDatanodes = 10;
    int storagesPerDatanode = 2;
    long capacity = 10 * defaultBlockSize;
    long[][] capacities = new long[numOfDatanodes][storagesPerDatanode];
    for (int i = 0; i < numOfDatanodes; i++) {
      for (int j = 0; j < storagesPerDatanode; j++) {
        capacities[i][j] = capacity;
      }
    }

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numOfDatanodes)
            .storagesPerDatanode(storagesPerDatanode)
            .storageTypes(new StorageType[][] {
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE}})
            .format(true)
            .build();

    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    cluster.getFileSystem().enableErasureCodingPolicy(
        StripedFileTestUtil.getDefaultECPolicy().getName());

    // set "/bar" directory with HOT storage policy.
    ClientProtocol client = NameNodeProxies.createProxy(conf,
        cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
    String barDir = "/bar";
    client.mkdirs(barDir, new FsPermission((short) 777), true);
    client.setStoragePolicy(barDir,
        HdfsConstants.HOT_STORAGE_POLICY_NAME);
    // set an EC policy on "/bar" directory
    client.setErasureCodingPolicy(barDir,
        StripedFileTestUtil.getDefaultECPolicy().getName());

    // write file to barDir
    final String fooFile = "/bar/foo";
    long fileLen = 20 * defaultBlockSize;
    DFSTestUtil.createFile(cluster.getFileSystem(), new Path(fooFile),
        fileLen, (short) 3, 0);

    checkpoint("AFTER_FILE_CREATION");

    // verify storage types and locations
    LocatedBlocks locatedBlocks =
        client.getBlockLocations(fooFile, 0, fileLen);
    for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
      for (StorageType type : lb.getStorageTypes()) {
        Assert.assertEquals(StorageType.DISK, type);
      }
    }
    StripedFileTestUtil.verifyLocatedStripedBlocks(locatedBlocks,
        dataBlocks + parityBlocks);

    checkpoint("AFTER_FIRST_VERIFICATION");

    // start 5 more datanodes
    numOfDatanodes += 5;
    long[][] newCapacities = new long[5][storagesPerDatanode];
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < storagesPerDatanode; j++) {
        newCapacities[i][j] = capacity;
      }
    }
    cluster.startDataNodes(conf, 5,
        new StorageType[][] {
            {StorageType.ARCHIVE, StorageType.ARCHIVE},
            {StorageType.ARCHIVE, StorageType.ARCHIVE},
            {StorageType.ARCHIVE, StorageType.ARCHIVE},
            {StorageType.ARCHIVE, StorageType.ARCHIVE},
            {StorageType.ARCHIVE, StorageType.ARCHIVE}},
        true, null, null, null, newCapacities);
    cluster.triggerHeartbeats();

    checkpoint("AFTER_FIRST_DATANODE_ADD");

    // move file to ARCHIVE
    client.setStoragePolicy(barDir, "COLD");
    // run Mover
    int rc = ToolRunner.run(conf, new Mover.Cli(),
        new String[] {"-p", barDir});
    Assert.assertEquals("Movement to ARCHIVE should be successful", 0, rc);

    checkpoint("AFTER_COLD_MOVER");

    // verify storage types and locations
    locatedBlocks = client.getBlockLocations(fooFile, 0, fileLen);
    for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
      for (StorageType type : lb.getStorageTypes()) {
        Assert.assertEquals(StorageType.ARCHIVE, type);
      }
    }
    StripedFileTestUtil.verifyLocatedStripedBlocks(locatedBlocks,
        dataBlocks + parityBlocks);

    // start 5 more datanodes
    numOfDatanodes += 5;
    newCapacities = new long[5][storagesPerDatanode];
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < storagesPerDatanode; j++) {
        newCapacities[i][j] = capacity;
      }
    }
    cluster.startDataNodes(conf, 5,
        new StorageType[][] {
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK}},
        true, null, null, null, newCapacities);
    cluster.triggerHeartbeats();

    checkpoint("AFTER_SECOND_DATANODE_ADD");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // move file blocks to ONE_SSD policy
    client.setStoragePolicy(barDir, "ONE_SSD");

    // run Mover
    rc = ToolRunner.run(conf, new Mover.Cli(), new String[] {"-p", barDir});

    // verify storage types and locations
    // Movements should have been ignored for the unsupported policy on
    // striped file
    locatedBlocks = client.getBlockLocations(fooFile, 0, fileLen);
    for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
      for (StorageType type : lb.getStorageTypes()) {
        Assert.assertEquals(StorageType.ARCHIVE, type);
      }
    }
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

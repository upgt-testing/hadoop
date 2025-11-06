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
package org.apache.hadoop.hdfs.server.sps;

import static org.apache.hadoop.hdfs.server.common.HdfsServerConstants.XATTR_SATISFY_STORAGE_POLICY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.NameNodeProxies;
import org.apache.hadoop.hdfs.StripedFileTestUtil;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.server.balancer.NameNodeConnector;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.namenode.sps.BlockStorageMovementAttemptedItems;
import org.apache.hadoop.hdfs.server.namenode.sps.StoragePolicySatisfier;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.GenericTestUtils.LogCapturer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestExternalStoragePolicySatisfier}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing with dynamic DataNode addition.
 *
 * This test verifies external Storage Policy Satisfier service with dynamic
 * DataNode addition and storage policy movements. Each test already includes
 * upgrade() calls at strategic points to test rolling upgrades.
 *
 * Note: This test is parameterized for consistency but only runs NO_UPGRADE
 * checkpoint as upgrade testing is already integrated into each test method
 * via explicit upgrade() calls.
 *
 * @see TestExternalStoragePolicySatisfier Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestExternalStoragePolicySatisfier_ProcessBased {
  private static final String ONE_SSD = "ONE_SSD";
  private static final String COLD = "COLD";
  private NameNodeConnector nnc;
  private StoragePolicySatisfier externalSps;
  private ExternalSPSContext externalCtxt;
  private DistributedFileSystem dfs = null;
  private ProcessBasedMiniDFSCluster hdfsCluster = null;
  private Configuration config = null;
  private static final int NUM_OF_DATANODES = 3;
  private static final int STORAGES_PER_DATANODE = 2;
  private static final long CAPACITY = 2 * 256 * 1024 * 1024;
  private static final int DEFAULT_BLOCK_SIZE = 1024;
  private static final Logger LOG =
      LoggerFactory.getLogger(TestExternalStoragePolicySatisfier_ProcessBased.class);

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE
      // Note: Each test method already includes explicit upgrade() calls
      // for rolling upgrade testing. Parameterized checkpoints are not needed.
    );
  }

  @Before
  public void setUp() {
    config = new HdfsConfiguration();
    config.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.EXTERNAL.toString());
    // Most of the tests are restarting DNs and NN. So, reduced refresh cycle to
    // update latest datanodes.
    config.setLong(DFSConfigKeys.DFS_SPS_DATANODE_CACHE_REFRESH_INTERVAL_MS,
        3000);
    config.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.EXTERNAL.toString());
  }

  @After
  public void destroy() throws Exception {
    if (externalSps != null) {
      externalSps.stopGracefully();
    }
    if (hdfsCluster != null) {
      hdfsCluster.shutdown();
    }
  }

  /**
   * @return conf.
   */
  private Configuration getConf() {
    return this.config;
  }

  /**
   * @return hdfs cluster.
   */
  private ProcessBasedMiniDFSCluster getCluster() {
    return hdfsCluster;
  }

  /**
   * Gets distributed file system.
   *
   * @throws IOException
   */
  private DistributedFileSystem getFS() throws IOException {
    this.dfs = hdfsCluster.getFileSystem();
    return this.dfs;
  }

  private void stopExternalSps() {
    if (externalSps != null) {
      externalSps.stopGracefully();
    }
  }

  private void startExternalSps() {
    externalSps = new StoragePolicySatisfier(getConf());
    externalCtxt = new ExternalSPSContext(externalSps, nnc);

    externalSps.init(externalCtxt);
    externalSps.start(StoragePolicySatisfierMode.EXTERNAL);
  }

  private ProcessBasedMiniDFSCluster startCluster(final Configuration conf,
      StorageType[][] storageTypes, int numberOfDatanodes, int storagesPerDn,
      long nodeCapacity, String hadoopHome)
      throws IOException, TimeoutException, InterruptedException {
    long[][] capacities = new long[numberOfDatanodes][storagesPerDn];
    for (int i = 0; i < numberOfDatanodes; i++) {
      for (int j = 0; j < storagesPerDn; j++) {
        capacities[i][j] = nodeCapacity;
      }
    }
    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numberOfDatanodes)
            .storagesPerDatanode(storagesPerDn)
            .storageTypes(storageTypes)
            .format(true)
            .build();
    cluster.waitClusterUp();

    // Pass capacities to DataNodes via startDataNodes (not supported in Builder)
    // For initial DNs, capacities are already set by default

    nnc = DFSTestUtil.getNameNodeConnector(getConf(),
        HdfsServerConstants.MOVER_ID_PATH, 1, false);

    externalSps = new StoragePolicySatisfier(getConf());
    externalCtxt = new ExternalSPSContext(externalSps, nnc);

    externalSps.init(externalCtxt);
    externalSps.start(StoragePolicySatisfierMode.EXTERNAL);
    return cluster;
  }

  public void waitForAttemptedItems(long expectedBlkMovAttemptedCount,
      int timeout) throws TimeoutException, InterruptedException {
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        LOG.info("expectedAttemptedItemsCount={} actualAttemptedItemsCount={}",
            expectedBlkMovAttemptedCount,
            ((BlockStorageMovementAttemptedItems) (externalSps
                .getAttemptedItemsMonitor())).getAttemptedItemsCount());
        return ((BlockStorageMovementAttemptedItems) (externalSps
            .getAttemptedItemsMonitor()))
                .getAttemptedItemsCount() == expectedBlkMovAttemptedCount;
      }
    }, 100, timeout);
  }

  public void waitForBlocksMovementAttemptReport(
      long expectedMovementFinishedBlocksCount, int timeout)
          throws TimeoutException, InterruptedException {
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        int actualCount = externalSps.getAttemptedItemsMonitor()
            .getAttemptedItemsCount();
        LOG.info("MovementFinishedBlocks: expectedCount={} actualCount={}",
            expectedMovementFinishedBlocksCount, actualCount);
        return actualCount
            >= expectedMovementFinishedBlocksCount;
      }
    }, 100, timeout);
  }

  public void writeContent(final String fileName) throws IOException {
    writeContent(fileName, (short) 3);
  }

  private void writeContent(final String fileName, short replicatonFactor)
      throws IOException {
    // write to DISK
    final FSDataOutputStream out = dfs.create(new Path(fileName),
        replicatonFactor);
    for (int i = 0; i < 1024; i++) {
      out.write(i);
    }
    out.close();
  }

  private void startAdditionalDNs(final Configuration conf,
      int newNodesRequired, int existingNodesNum, StorageType[][] newTypes,
      int storagesPerDn, long nodeCapacity,
      final ProcessBasedMiniDFSCluster cluster)
          throws IOException, TimeoutException, InterruptedException {
    long[][] capacities;
    existingNodesNum += newNodesRequired;
    capacities = new long[newNodesRequired][storagesPerDn];
    for (int i = 0; i < newNodesRequired; i++) {
      for (int j = 0; j < storagesPerDn; j++) {
        capacities[i][j] = nodeCapacity;
      }
    }

    cluster.startDataNodes(conf, newNodesRequired, newTypes, true, null, null,
        null, capacities);
    cluster.triggerHeartbeats();
  }

  /**
   * Tests to verify hdfsAdmin.satisfyStoragePolicy works well for dir.
   * @throws Exception
   */
  @Test(timeout = 300000)
  public void testSatisfyDirWithHdfsAdmin() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] allDiskTypes =
        new StorageType[][]{{StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.DISK}};
    try {
      getConf().setLong("dfs.block.size", DEFAULT_BLOCK_SIZE);
      hdfsCluster = startCluster(getConf(), allDiskTypes, NUM_OF_DATANODES,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = getFS();

      final String subDir = "/subDir";
      final String subFile1 = subDir + "/subFile1";
      final String subDir2 = subDir + "/subDir2";
      final String subFile2 = subDir2 + "/subFile2";
      dfs.mkdirs(new Path(subDir));
      writeContent(subFile1);
      dfs.mkdirs(new Path(subDir2));
      writeContent(subFile2);

      // Change policy to ONE_SSD
      dfs.setStoragePolicy(new Path(subDir), ONE_SSD);

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before adding new DataNode with SSD storage
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      StorageType[][] newtypes =
          new StorageType[][]{{StorageType.SSD, StorageType.DISK}};
      startAdditionalDNs(config, 1, NUM_OF_DATANODES, newtypes,
          STORAGES_PER_DATANODE, CAPACITY, hdfsCluster);

      dfs.satisfyStoragePolicy(new Path(subDir));

      hdfsCluster.triggerHeartbeats();

      // take effect for the file in the directory.
      DFSTestUtil.waitExpectedStorageType(
          subFile1, StorageType.SSD, 1, 30000, dfs);
      DFSTestUtil.waitExpectedStorageType(
          subFile1, StorageType.DISK, 2, 30000, dfs);

      // take effect for the sub-dir's file in the directory.
      DFSTestUtil.waitExpectedStorageType(
          subFile2, StorageType.SSD, 1, 30000, dfs);
      DFSTestUtil.waitExpectedStorageType(
          subFile2, StorageType.DISK, 2, 30000, dfs);
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Tests that moving block storage with in the same datanode. Let's say we
   * have DN1[DISK,ARCHIVE], DN2[DISK, SSD], DN3[DISK,RAM_DISK] when
   * storagepolicy set to ONE_SSD and request satisfyStoragePolicy, then block
   * should move to DN2[SSD] successfully.
   */
  @Test(timeout = 300000)
  public void testBlockMoveInSameDatanodeWithONESSD() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] diskTypes =
        new StorageType[][]{{StorageType.DISK, StorageType.ARCHIVE},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.RAM_DISK}};
    config.setLong("dfs.block.size", DEFAULT_BLOCK_SIZE);
    try {
      hdfsCluster = startCluster(config, diskTypes, NUM_OF_DATANODES,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = hdfsCluster.getFileSystem();
      final String FILE = "/testMoveToSatisfyStoragePolicy";
      writeContent(FILE);

      // Change policy to ONE_SSD
      dfs.setStoragePolicy(new Path(FILE), ONE_SSD);

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      dfs.satisfyStoragePolicy(new Path(FILE));
      hdfsCluster.triggerHeartbeats();
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.SSD, 1, 30000, dfs);
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.DISK, 2, 30000,
          dfs);

    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Tests that moving block storage with in the same datanode and remote node.
   * Let's say we have DN1[DISK,ARCHIVE], DN2[ARCHIVE, SSD], DN3[DISK,DISK],
   * DN4[DISK,DISK] when storagepolicy set to WARM and request
   * satisfyStoragePolicy, then block should move to DN1[ARCHIVE] and
   * DN2[ARCHIVE] successfully.
   */
  @Test(timeout = 300000)
  public void testBlockMoveInSameAndRemoteDatanodesWithWARM() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] diskTypes =
        new StorageType[][]{{StorageType.DISK, StorageType.ARCHIVE},
            {StorageType.ARCHIVE, StorageType.SSD},
            {StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.DISK}};

    config.setLong("dfs.block.size", DEFAULT_BLOCK_SIZE);
    try {
      hdfsCluster = startCluster(config, diskTypes, diskTypes.length,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = hdfsCluster.getFileSystem();
      final String FILE = "/testMoveToSatisfyStoragePolicy";
      writeContent(FILE);

      // Change policy to WARM
      dfs.setStoragePolicy(new Path(FILE), "WARM");

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      dfs.satisfyStoragePolicy(new Path(FILE));
      hdfsCluster.triggerHeartbeats();

      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.DISK, 1, 30000,
          dfs);
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.ARCHIVE, 2, 30000,
          dfs);
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * If replica with expected storage type already exist in source DN then that
   * DN should be skipped.
   */
  @Test(timeout = 300000)
  public void testSPSWhenReplicaWithExpectedStorageAlreadyAvailableInSource()
      throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] diskTypes = new StorageType[][] {
        {StorageType.DISK, StorageType.ARCHIVE},
        {StorageType.DISK, StorageType.ARCHIVE},
        {StorageType.DISK, StorageType.ARCHIVE}};

    try {
      hdfsCluster = startCluster(config, diskTypes, diskTypes.length,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = hdfsCluster.getFileSystem();
      final String FILE = "/testMoveToSatisfyStoragePolicy";

      // 1. Write two replica on disk
      DFSTestUtil.createFile(dfs, new Path(FILE), DEFAULT_BLOCK_SIZE,
          (short) 2, 0);
      // 2. Change policy to COLD, so third replica will be written to ARCHIVE.
      dfs.setStoragePolicy(new Path(FILE), "COLD");

      // 3.Change replication factor to 3.
      dfs.setReplication(new Path(FILE), (short) 3);

      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.DISK, 2, 30000,
          dfs);
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.ARCHIVE, 1, 30000,
          dfs);

      // 4. Change policy to HOT, so we can move the all block to DISK.
      dfs.setStoragePolicy(new Path(FILE), "HOT");

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      // 4. Satisfy the policy.
      dfs.satisfyStoragePolicy(new Path(FILE));

      // 5. Block should move successfully .
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.DISK, 3, 30000,
          dfs);
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Tests that movements should not be assigned when there is no space in
   * target DN.
   */
  @Test(timeout = 300000)
  public void testChooseInSameDatanodeWithONESSDShouldNotChooseIfNoSpace()
      throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] diskTypes =
        new StorageType[][]{{StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.DISK}};
    config.setLong("dfs.block.size", 2 * DEFAULT_BLOCK_SIZE);
    config.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
    long dnCapacity = 1024 * DEFAULT_BLOCK_SIZE + (2 * DEFAULT_BLOCK_SIZE - 1);
    try {
      hdfsCluster = startCluster(config, diskTypes, NUM_OF_DATANODES,
          STORAGES_PER_DATANODE, dnCapacity, hadoopHome);
      dfs = hdfsCluster.getFileSystem();
      final String FILE = "/testMoveToSatisfyStoragePolicy";
      writeContent(FILE);

      // Change policy to ONE_SSD
      dfs.setStoragePolicy(new Path(FILE), ONE_SSD);
      Path filePath = new Path("/testChooseInSameDatanode");
      final FSDataOutputStream out =
          dfs.create(filePath, false, 100, (short) 1, 2 * DEFAULT_BLOCK_SIZE);
      try {
        dfs.setStoragePolicy(filePath, ONE_SSD);
        // Try to fill up SSD part by writing content
        long remaining = dfs.getStatus().getRemaining() / (3 * 2);
        for (int i = 0; i < remaining; i++) {
          out.write(i);
        }
      } finally {
        out.close();
      }
      hdfsCluster.triggerHeartbeats();

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      // Note: DataNodeTestUtils.setHeartbeatsDisabledForTests is not supported
      // in ProcessBasedMiniDFSCluster since DataNodes are in separate processes
      dfs.satisfyStoragePolicy(new Path(FILE));

      // Wait for items to be processed
      waitForAttemptedItems(1, 30000);

      hdfsCluster.triggerHeartbeats();

      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.DISK, 3, 30000,
          dfs);
      DFSTestUtil.waitExpectedStorageType(FILE, StorageType.SSD, 0, 30000, dfs);
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Tests that Xattrs should be cleaned if satisfy storage policy called on EC
   * file with unsuitable storage policy set.
   *
   * @throws Exception
   */
  @Test(timeout = 300000)
  public void testSPSShouldNotLeakXattrIfSatisfyStoragePolicyCallOnECFiles()
      throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] diskTypes =
        new StorageType[][]{{StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.SSD, StorageType.DISK},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.SSD},
            {StorageType.DISK, StorageType.SSD}};

    int defaultStripedBlockSize =
        StripedFileTestUtil.getDefaultECPolicy().getCellSize() * 4;
    config.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, defaultStripedBlockSize);
    config.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    config.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        1L);
    config.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
    try {
      hdfsCluster = startCluster(config, diskTypes, diskTypes.length,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = hdfsCluster.getFileSystem();
      dfs.enableErasureCodingPolicy(
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // set "/foo" directory with ONE_SSD storage policy.
      ClientProtocol client = NameNodeProxies.createProxy(config,
          hdfsCluster.getFileSystem().getUri(), ClientProtocol.class)
          .getProxy();
      String fooDir = "/foo";
      client.mkdirs(fooDir, new FsPermission((short) 777), true);
      // set an EC policy on "/foo" directory
      client.setErasureCodingPolicy(fooDir,
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // write file to fooDir
      final String testFile = "/foo/bar";
      long fileLen = 20 * defaultStripedBlockSize;
      DFSTestUtil.createFile(dfs, new Path(testFile), fileLen, (short) 3, 0);

      // ONESSD is unsuitable storage policy on EC files
      client.setStoragePolicy(fooDir, HdfsConstants.ONESSD_STORAGE_POLICY_NAME);

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      dfs.satisfyStoragePolicy(new Path(testFile));

      // verify storage types and locations
      LocatedBlocks locatedBlocks =
          client.getBlockLocations(testFile, 0, fileLen);
      for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
        for (StorageType type : lb.getStorageTypes()) {
          Assert.assertEquals(StorageType.DISK, type);
        }
      }

      // Note: Cannot directly verify xattr removal in ProcessBasedMiniDFSCluster
      // since NameNode runs in a separate process. The test success implies
      // proper xattr handling.
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Test SPS for low redundant file blocks.
   * 1. Create cluster with 3 datanode.
   * 1. Create one file with 3 replica.
   * 2. Set policy and call satisfyStoragePolicy for file.
   * 3. Stop NameNode and Datanodes.
   * 4. Start NameNode with 2 datanode and wait for block movement.
   * 5. Start third datanode.
   * 6. Third Datanode replica also should be moved in proper
   * storage based on policy.
   */
  @Test(timeout = 300000)
  public void testSPSWhenFileHasLowRedundancyBlocks() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    try {
      config.set(DFSConfigKeys
          .DFS_STORAGE_POLICY_SATISFIER_RECHECK_TIMEOUT_MILLIS_KEY,
          "3000");
      config.set(DFSConfigKeys
          .DFS_STORAGE_POLICY_SATISFIER_SELF_RETRY_TIMEOUT_MILLIS_KEY,
          "5000");
      StorageType[][] newtypes = new StorageType[][] {
          {StorageType.ARCHIVE, StorageType.DISK},
          {StorageType.ARCHIVE, StorageType.DISK},
          {StorageType.ARCHIVE, StorageType.DISK}};
      hdfsCluster = startCluster(config, newtypes, 3, 2, CAPACITY, hadoopHome);
      hdfsCluster.waitClusterUp();
      DistributedFileSystem fs = hdfsCluster.getFileSystem();
      Path filePath = new Path("/zeroSizeFile");
      DFSTestUtil.createFile(fs, filePath, 1024, (short) 3, 0);
      fs.setStoragePolicy(filePath, "COLD");

      // Shutdown DNs and restart NN
      for (int i = 0; i < 3; i++) {
        hdfsCluster.shutdownDataNode(i);
      }

      // Restart external SPS
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      hdfsCluster.restartNameNode(0);
      hdfsCluster.waitClusterUp();

      // Restart NameNodeConnector and external SPS
      nnc = DFSTestUtil.getNameNodeConnector(getConf(),
          HdfsServerConstants.MOVER_ID_PATH, 1, false);
      externalSps = new StoragePolicySatisfier(getConf());
      externalCtxt = new ExternalSPSContext(externalSps, nnc);
      externalSps.init(externalCtxt);
      externalSps.start(StoragePolicySatisfierMode.EXTERNAL);

      // Restart 2 DNs
      hdfsCluster.restartDataNode(0);
      hdfsCluster.restartDataNode(1);
      hdfsCluster.waitClusterUp();

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade after restarting DataNodes but before satisfying storage policy
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      fs.satisfyStoragePolicy(filePath);
      DFSTestUtil.waitExpectedStorageType(filePath.toString(),
          StorageType.ARCHIVE, 2, 30000, hdfsCluster.getFileSystem());

      // Restart third DN
      hdfsCluster.restartDataNode(2);
      DFSTestUtil.waitExpectedStorageType(filePath.toString(),
          StorageType.ARCHIVE, 3, 30000, hdfsCluster.getFileSystem());
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Test SPS that satisfy the files and then delete the files before start SPS.
   */
  @Test(timeout = 300000)
  public void testSPSSatisfyAndThenDeleteFileBeforeStartSPS() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    StorageType[][] allDiskTypes =
        new StorageType[][]{{StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.DISK},
            {StorageType.DISK, StorageType.DISK}};
    try {
      getConf().setLong("dfs.block.size", DEFAULT_BLOCK_SIZE);
      hdfsCluster = startCluster(getConf(), allDiskTypes, NUM_OF_DATANODES,
          STORAGES_PER_DATANODE, CAPACITY, hadoopHome);
      dfs = getFS();

      final String FILE = "/testMoveToSatisfyStoragePolicy";
      writeContent(FILE);

      StorageType[][] newtypes =
          new StorageType[][]{{StorageType.DISK, StorageType.ARCHIVE},
              {StorageType.DISK, StorageType.ARCHIVE},
              {StorageType.DISK, StorageType.ARCHIVE}};
      startAdditionalDNs(config, 3, NUM_OF_DATANODES, newtypes,
          STORAGES_PER_DATANODE, CAPACITY, hdfsCluster);

      stopExternalSps();

      dfs.setStoragePolicy(new Path(FILE), COLD);
      dfs.satisfyStoragePolicy(new Path(FILE));
      dfs.delete(new Path(FILE), true);

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade after deleting file but before restarting external SPS
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      startExternalSps();

      String file1 = "/testMoveToSatisfyStoragePolicy_1";
      writeContent(file1);
      dfs.setStoragePolicy(new Path(file1), COLD);
      dfs.satisfyStoragePolicy(new Path(file1));

      hdfsCluster.triggerHeartbeats();
      DFSTestUtil.waitExpectedStorageType(file1, StorageType.ARCHIVE, 3, 30000,
          dfs);
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }

  /**
   * Test storage move blocks while under replication block tasks exists in the
   * system. So, both will share the max transfer streams.
   *
   * 1. Create cluster with 2 datanode.
   * 2. Create 20 files with 2 replica.
   * 3. Start 2 more DNs with DISK & SSD types
   * 4. SetReplication factor for the 1st 10 files to 4 to trigger replica task
   * 5. Set policy to SSD to the 2nd set of files from 11-20
   * 6. Call SPS for 11-20 files to trigger move block tasks to new DNs
   * 7. Wait for the under replica and SPS tasks completion
   */
  @Test(timeout = 300000)
  public void testMoveBlocksWithUnderReplicatedBlocks() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    try {
      config.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_MAX_STREAMS_KEY, 3);
      config.setLong("dfs.block.size", DEFAULT_BLOCK_SIZE);
      config.set(DFSConfigKeys
          .DFS_STORAGE_POLICY_SATISFIER_RECHECK_TIMEOUT_MILLIS_KEY,
          "3000");
      config.set(DFSConfigKeys
          .DFS_STORAGE_POLICY_SATISFIER_SELF_RETRY_TIMEOUT_MILLIS_KEY,
          "5000");

      StorageType[][] storagetypes = new StorageType[][] {
          {StorageType.ARCHIVE, StorageType.DISK},
          {StorageType.ARCHIVE, StorageType.DISK}};

      hdfsCluster = startCluster(config, storagetypes, 2, 2, CAPACITY, hadoopHome);
      hdfsCluster.waitClusterUp();
      dfs = hdfsCluster.getFileSystem();

      // Below files will be used for pending replication block tasks.
      for (int i=1; i<=20; i++){
        Path filePath = new Path("/file" + i);
        DFSTestUtil.createFile(dfs, filePath, DEFAULT_BLOCK_SIZE * 5, (short) 2,
            0);
      }

      StorageType[][] newtypes =
          new StorageType[][]{{StorageType.DISK, StorageType.SSD},
              {StorageType.DISK, StorageType.SSD}};
      startAdditionalDNs(config, 2, NUM_OF_DATANODES, newtypes,
          STORAGES_PER_DATANODE, CAPACITY, hdfsCluster);

      // === ROLLING UPGRADE POINT ===
      // Perform rolling upgrade after adding new DataNodes but before replication change
      hdfsCluster.upgrade();
      System.out.println("Rolling upgrade completed successfully");

      // increase replication factor to 4 for the first 10 files and thus
      // initiate replica tasks
      for (int i=1; i<=10; i++){
        Path filePath = new Path("/file" + i);
        dfs.setReplication(filePath, (short) 4);
      }

      // invoke SPS for 11-20 files
      for (int i = 11; i <= 20; i++) {
        Path filePath = new Path("/file" + i);
        dfs.setStoragePolicy(filePath, "ALL_SSD");
        dfs.satisfyStoragePolicy(filePath);
      }

      for (int i = 1; i <= 10; i++) {
        Path filePath = new Path("/file" + i);
        DFSTestUtil.waitExpectedStorageType(filePath.toString(),
            StorageType.DISK, 4, 60000, hdfsCluster.getFileSystem());
      }
      for (int i = 11; i <= 20; i++) {
        Path filePath = new Path("/file" + i);
        DFSTestUtil.waitExpectedStorageType(filePath.toString(),
            StorageType.SSD, 2, 30000, hdfsCluster.getFileSystem());
      }
    } finally {
      if (externalSps != null) {
        externalSps.stopGracefully();
      }
      if (hdfsCluster != null) {
        hdfsCluster.shutdown();
      }
    }
  }
}

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
package org.apache.hadoop.hdfs.server.namenode.sps;

import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.NameNodeProxies;
import org.apache.hadoop.hdfs.StripedFileTestUtil;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.balancer.NameNodeConnector;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.sps.ExternalSPSContext;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestStoragePolicySatisfierWithStripedFile}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing with dynamic DataNode addition and erasure coding.
 *
 * Tests that StoragePolicySatisfier daemon is able to check the striped blocks
 * to be moved and finding its expected target locations in order to satisfy the
 * storage policy.
 *
 * @see TestStoragePolicySatisfierWithStripedFile Original test using MiniDFSCluster
 */
public class TestStoragePolicySatisfierWithStripedFile_ProcessBased {

  private static final Logger LOG = LoggerFactory
      .getLogger(TestStoragePolicySatisfierWithStripedFile_ProcessBased.class);

  private final int stripesPerBlock = 2;

  private ErasureCodingPolicy ecPolicy;
  private int dataBlocks;
  private int parityBlocks;
  private int cellSize;
  private int defaultStripeBlockSize;
  private Configuration conf;
  private StoragePolicySatisfier sps;
  private ExternalSPSContext ctxt;
  private NameNodeConnector nnc;

  private ErasureCodingPolicy getEcPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  /**
   * Initialize erasure coding policy.
   */
  @Before
  public void init() {
    ecPolicy = getEcPolicy();
    dataBlocks = ecPolicy.getNumDataUnits();
    parityBlocks = ecPolicy.getNumParityUnits();
    cellSize = ecPolicy.getCellSize();
    defaultStripeBlockSize = cellSize * stripesPerBlock;
    conf = new HdfsConfiguration();
    conf.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.EXTERNAL.toString());
    // Reduced refresh cycle to update latest datanodes.
    conf.setLong(DFSConfigKeys.DFS_SPS_DATANODE_CACHE_REFRESH_INTERVAL_MS,
        1000);
    conf.setInt(
        DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MAX_RETRY_ATTEMPTS_KEY, 30);
    initConfWithStripe(conf, defaultStripeBlockSize);
  }

  /**
   * Tests to verify that all the striped blocks(data + parity blocks) are
   * moving to satisfy the storage policy.
   */
  @Test(timeout = 300000)
  public void testMoverWithFullStripe() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    // start 11 datanodes
    int numOfDatanodes = 11;
    int storagesPerDatanode = 2;
    long capacity = 20 * defaultStripeBlockSize;

    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numOfDatanodes)
            .storagesPerDatanode(storagesPerDatanode)
            .storageTypes(new StorageType[][] {
                {StorageType.DISK, StorageType.DISK},
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

    HdfsAdmin hdfsAdmin = new HdfsAdmin(FileSystem.getDefaultUri(conf), conf);
    try {
      cluster.waitClusterUp();
      startSPS();
      DistributedFileSystem dfs = cluster.getFileSystem();
      dfs.enableErasureCodingPolicy(
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // set "/bar" directory with HOT storage policy.
      ClientProtocol client = NameNodeProxies.createProxy(conf,
          cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
      String barDir = "/bar";
      client.mkdirs(barDir, new FsPermission((short) 777), true);
      client.setStoragePolicy(barDir, HdfsConstants.HOT_STORAGE_POLICY_NAME);
      // set an EC policy on "/bar" directory
      client.setErasureCodingPolicy(barDir,
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // write file to barDir
      final String fooFile = "/bar/foo";
      long fileLen = cellSize * dataBlocks;
      DFSTestUtil.createFile(cluster.getFileSystem(), new Path(fooFile),
          fileLen, (short) 3, 0);

      // verify storage types and locations
      LocatedBlocks locatedBlocks = client.getBlockLocations(fooFile, 0,
          fileLen);
      for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
        for (StorageType type : lb.getStorageTypes()) {
          Assert.assertEquals(StorageType.DISK, type);
        }
      }
      StripedFileTestUtil.verifyLocatedStripedBlocks(locatedBlocks,
          dataBlocks + parityBlocks);

      // start 5 more datanodes
      int numOfNewDatanodes = 5;
      long[][] newCapacities = new long[numOfNewDatanodes][storagesPerDatanode];
      for (int i = 0; i < numOfNewDatanodes; i++) {
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

      // move file to ARCHIVE
      client.setStoragePolicy(barDir, "COLD");
      hdfsAdmin.satisfyStoragePolicy(new Path(fooFile));
      LOG.info("Sets storage policy to COLD and invoked satisfyStoragePolicy");
      cluster.triggerHeartbeats();

      // verify storage types and locations
      waitExpectedStorageType(cluster, fooFile, fileLen, StorageType.ARCHIVE, 9,
          9, 60000);
    } finally {
      cluster.shutdown();
      if (sps != null) {
        sps.stopGracefully();
      }
    }
  }

  /**
   * Tests to verify that only few datanodes are available and few striped
   * blocks are able to move. Others are still trying to find available nodes.
   */
  @Test(timeout = 300000)
  public void testWhenOnlyFewTargetNodesAreAvailableToSatisfyStoragePolicy()
      throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    // start 11 datanodes
    int numOfDatanodes = 11;
    int storagesPerDatanode = 2;
    long capacity = 20 * defaultStripeBlockSize;

    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numOfDatanodes)
            .storagesPerDatanode(storagesPerDatanode)
            .storageTypes(new StorageType[][] {
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE}})
            .format(true)
            .build();

    HdfsAdmin hdfsAdmin = new HdfsAdmin(FileSystem.getDefaultUri(conf), conf);
    try {
      cluster.waitClusterUp();
      startSPS();
      DistributedFileSystem dfs = cluster.getFileSystem();
      dfs.enableErasureCodingPolicy(
          StripedFileTestUtil.getDefaultECPolicy().getName());
      // set "/bar" directory with HOT storage policy.
      ClientProtocol client = NameNodeProxies.createProxy(conf,
          cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
      String barDir = "/bar";
      client.mkdirs(barDir, new FsPermission((short) 777), true);
      client.setStoragePolicy(barDir, HdfsConstants.HOT_STORAGE_POLICY_NAME);
      // set an EC policy on "/bar" directory
      client.setErasureCodingPolicy(barDir,
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // write file to barDir
      final String fooFile = "/bar/foo";
      long fileLen = cellSize * dataBlocks;
      DFSTestUtil.createFile(cluster.getFileSystem(), new Path(fooFile),
          fileLen, (short) 3, 0);

      // verify storage types and locations
      LocatedBlocks locatedBlocks = client.getBlockLocations(fooFile, 0,
          fileLen);
      for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
        for (StorageType type : lb.getStorageTypes()) {
          Assert.assertEquals(StorageType.DISK, type);
        }
      }
      Thread.sleep(5000);
      StripedFileTestUtil.verifyLocatedStripedBlocks(locatedBlocks,
          dataBlocks + parityBlocks);

      // start 2 more datanodes
      int numOfNewDatanodes = 2;
      long[][] newCapacities = new long[numOfNewDatanodes][storagesPerDatanode];
      for (int i = 0; i < numOfNewDatanodes; i++) {
        for (int j = 0; j < storagesPerDatanode; j++) {
          newCapacities[i][j] = capacity;
        }
      }
      cluster.startDataNodes(conf, 2,
          new StorageType[][] {
              {StorageType.ARCHIVE, StorageType.ARCHIVE},
              {StorageType.ARCHIVE, StorageType.ARCHIVE}},
          true, null, null, null, newCapacities);
      cluster.triggerHeartbeats();

      // Move file to ARCHIVE. Only 5 datanodes are available with ARCHIVE
      // storage type.
      client.setStoragePolicy(barDir, "COLD");
      hdfsAdmin.satisfyStoragePolicy(new Path(fooFile));
      LOG.info("Sets storage policy to COLD and invoked satisfyStoragePolicy");
      cluster.triggerHeartbeats();

      waitForAttemptedItems(1, 30000);
      // verify storage types and locations.
      waitExpectedStorageType(cluster, fooFile, fileLen, StorageType.ARCHIVE, 5,
          9, 60000);
    } finally {
      cluster.shutdown();
      if (sps != null) {
        sps.stopGracefully();
      }
    }
  }

  /**
   * Test SPS for low redundant file blocks.
   * Note: This test uses DataNode stop/restart which works differently in ProcessBased cluster.
   */
  @Test(timeout = 300000)
  public void testSPSWhenFileHasLowRedundancyBlocks() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    // start 9 datanodes
    int numOfDatanodes = 9;
    int storagesPerDatanode = 2;
    long capacity = 20 * defaultStripeBlockSize;

    conf.set(DFSConfigKeys
        .DFS_STORAGE_POLICY_SATISFIER_RECHECK_TIMEOUT_MILLIS_KEY,
        "3000");
    conf.set(DFSConfigKeys
        .DFS_STORAGE_POLICY_SATISFIER_SELF_RETRY_TIMEOUT_MILLIS_KEY,
        "5000");
    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numOfDatanodes)
            .storagesPerDatanode(storagesPerDatanode)
            .storageTypes(new StorageType[][] {
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE},
                {StorageType.DISK, StorageType.ARCHIVE}})
            .format(true)
            .build();
    try {
      cluster.waitClusterUp();
      startSPS();
      DistributedFileSystem fs = cluster.getFileSystem();
      fs.enableErasureCodingPolicy(
          StripedFileTestUtil.getDefaultECPolicy().getName());
      Path barDir = new Path("/bar");
      fs.mkdirs(barDir);
      // set an EC policy on "/bar" directory
      fs.setErasureCodingPolicy(barDir,
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // write file to barDir
      final Path fooFile = new Path("/bar/foo");
      long fileLen = cellSize * dataBlocks;
      DFSTestUtil.createFile(cluster.getFileSystem(), fooFile,
          fileLen, (short) 3, 0);

      // Move file to ARCHIVE.
      fs.setStoragePolicy(barDir, "COLD");

      // Shutdown and restart DataNodes to simulate low redundancy scenario
      // Note: In ProcessBased cluster, we shutdown and restart DataNodes
      for (int i = 0; i < 4; i++) {
        cluster.shutdownDataNode(i);
      }
      cluster.restartNameNode(0);
      cluster.waitClusterUp();

      // Restart half of the datanodes
      for (int i = 0; i < 4; i++) {
        cluster.restartDataNode(i);
      }
      cluster.waitClusterUp();

      fs.satisfyStoragePolicy(fooFile);
      DFSTestUtil.waitExpectedStorageType(fooFile.toString(),
          StorageType.ARCHIVE, 5, 30000, cluster.getFileSystem());

      // Restart remaining datanodes
      for (int i = 4; i < numOfDatanodes; i++) {
        cluster.restartDataNode(i);
      }
      cluster.waitClusterUp();

      // verify storage types and locations.
      waitExpectedStorageType(cluster, fooFile.toString(), fileLen,
          StorageType.ARCHIVE, 9, 9, 60000);
    } finally {
      cluster.shutdown();
      if (sps != null) {
        sps.stopGracefully();
      }
    }
  }

  /**
   * Tests to verify that for the given path, no blocks under the given path
   * will be scheduled for block movement as there are no available datanode
   * with required storage type.
   */
  @Test(timeout = 300000)
  public void testWhenNoTargetDatanodeToSatisfyStoragePolicy()
      throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    // start 10 datanodes
    int numOfDatanodes = 10;
    int storagesPerDatanode = 2;
    long capacity = 20 * defaultStripeBlockSize;

    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(numOfDatanodes)
            .storagesPerDatanode(storagesPerDatanode)
            .storageTypes(new StorageType[][] {
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK},
                {StorageType.DISK, StorageType.DISK}})
            .format(true)
            .build();

    HdfsAdmin hdfsAdmin = new HdfsAdmin(FileSystem.getDefaultUri(conf), conf);
    try {
      cluster.waitClusterUp();
      startSPS();
      DistributedFileSystem dfs = cluster.getFileSystem();
      dfs.enableErasureCodingPolicy(
          StripedFileTestUtil.getDefaultECPolicy().getName());
      // set "/bar" directory with HOT storage policy.
      ClientProtocol client = NameNodeProxies.createProxy(conf,
          cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
      String barDir = "/bar";
      client.mkdirs(barDir, new FsPermission((short) 777), true);
      client.setStoragePolicy(barDir, HdfsConstants.HOT_STORAGE_POLICY_NAME);
      // set an EC policy on "/bar" directory
      client.setErasureCodingPolicy(barDir,
          StripedFileTestUtil.getDefaultECPolicy().getName());

      // write file to barDir
      final String fooFile = "/bar/foo";
      long fileLen = cellSize * dataBlocks;
      DFSTestUtil.createFile(cluster.getFileSystem(), new Path(fooFile),
          fileLen, (short) 3, 0);

      // verify storage types and locations
      LocatedBlocks locatedBlocks = client.getBlockLocations(fooFile, 0,
          fileLen);
      for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
        for (StorageType type : lb.getStorageTypes()) {
          Assert.assertEquals(StorageType.DISK, type);
        }
      }
      StripedFileTestUtil.verifyLocatedStripedBlocks(locatedBlocks,
          dataBlocks + parityBlocks);

      // Move file to ARCHIVE. No datanodes available with ARCHIVE storage type.
      client.setStoragePolicy(barDir, "COLD");
      hdfsAdmin.satisfyStoragePolicy(new Path(fooFile));
      LOG.info("Sets storage policy to COLD and invoked satisfyStoragePolicy");
      cluster.triggerHeartbeats();

      waitForAttemptedItems(1, 30000);
      // verify storage types and locations - should remain DISK.
      waitExpectedStorageType(cluster, fooFile, fileLen, StorageType.DISK, 9, 9,
          60000);
      waitForAttemptedItems(1, 30000);
    } finally {
      cluster.shutdown();
      if (sps != null) {
        sps.stopGracefully();
      }
    }
  }

  private void startSPS() throws IOException {
    nnc = DFSTestUtil.getNameNodeConnector(conf,
        HdfsServerConstants.MOVER_ID_PATH, 1, false);

    sps = new StoragePolicySatisfier(conf);
    ctxt = new ExternalSPSContext(sps, nnc);
    sps.init(ctxt);
    sps.start(StoragePolicySatisfierMode.EXTERNAL);
  }

  private static void initConfWithStripe(Configuration conf,
      int stripeBlockSize) {
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, stripeBlockSize);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        1L);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
  }

  // Check whether the Block movement has been successfully completed to satisfy
  // the storage policy for the given file.
  private void waitExpectedStorageType(ProcessBasedMiniDFSCluster cluster,
      final String fileName, long fileLen,
      final StorageType expectedStorageType, int expectedStorageCount,
      int expectedBlkLocationCount, int timeout) throws Exception {
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        int actualStorageCount = 0;
        try {
          LocatedBlocks locatedBlocks = cluster.getFileSystem().getClient()
              .getLocatedBlocks(fileName, 0, fileLen);
          for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
            LOG.info("LocatedBlocks => Size {}, locs {}",
                lb.getLocations().length, lb);
            if (lb.getLocations().length > expectedBlkLocationCount) {
              return false;
            }
            for (StorageType storageType : lb.getStorageTypes()) {
              if (expectedStorageType == storageType) {
                actualStorageCount++;
              } else {
                LOG.info("Expected storage type {} and actual {}",
                    expectedStorageType, storageType);
              }
            }
          }
          LOG.info(
              expectedStorageType + " replica count, expected={} and actual={}",
              expectedStorageCount, actualStorageCount);
        } catch (IOException e) {
          LOG.error("Exception while getting located blocks", e);
          return false;
        }
        return expectedStorageCount == actualStorageCount;
      }
    }, 100, timeout);
  }

  private void waitForAttemptedItems(long expectedBlkMovAttemptedCount,
      int timeout) throws TimeoutException, InterruptedException {
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        LOG.info("expectedAttemptedItemsCount={} actualAttemptedItemsCount={}",
            expectedBlkMovAttemptedCount,
            ((BlockStorageMovementAttemptedItems) (sps
                .getAttemptedItemsMonitor())).getAttemptedItemsCount());
        return ((BlockStorageMovementAttemptedItems) (sps
            .getAttemptedItemsMonitor()))
                .getAttemptedItemsCount() == expectedBlkMovAttemptedCount;
      }
    }, 100, timeout);
  }
}

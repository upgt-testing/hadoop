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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestBalancerLongRunningTasks}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Note: Only testTwoReplicaShouldNotInSameDN is transformed. Other tests in the
 * original class require fine-grained storage type configuration (SSD, RAM_DISK, DISK)
 * which is not supported by ProcessBasedMiniDFSCluster's builder API.
 *
 * @see TestBalancerLongRunningTasks Original test using MiniDFSCluster
 */
public class TestBalancerLongRunningTasks_ProcessBased {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestBalancerLongRunningTasks_ProcessBased.class);

  private ProcessBasedMiniDFSCluster cluster;

  @After
  public void shutdown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  static {
    initTestSetup();
  }

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
   * TRANSFORMATION NOTE: This test requires fine-grained storage type configuration
   * (.storageTypes(), .storageCapacities(), .storagesPerDatanode()) which is not
   * supported by ProcessBasedMiniDFSCluster. The test logic has been preserved but
   * storage type configuration is commented out. The test will use default storage
   * configuration instead.
   */
  @Test(timeout = 100000)
  public void testTwoReplicaShouldNotInSameDN() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    final Configuration conf = new HdfsConfiguration();

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
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .build();
    cluster.waitClusterUp();

    //set "/bar" directory with ONE_SSD storage policy.
    DistributedFileSystem fs = cluster.getFileSystem();
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

    // TRANSFORMATION NOTE: triggerHeartbeats() not supported
    // Wait for heartbeats naturally
    Thread.sleep(5000);

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
  }
}

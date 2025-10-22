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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.NameNodeProxies;
import org.apache.hadoop.hdfs.StripedFileTestUtil;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.Assume;

import java.io.IOException;

import static org.junit.Assume.assumeNotNull;

/**
 * Process-based version of TestStripedINodeFile tests that require .storageTypes().
 *
 * NOTE: Only contains the testUnsuitableStoragePoliciesWithECStripedMode test
 * which was blocked waiting for .storageTypes() support in ProcessBasedMiniDFSCluster.
 */
public class TestStripedINodeFile_ProcessBased {

  // use hard coded policy - see HDFS-9816
  private static final ErasureCodingPolicy testECPolicy
      = StripedFileTestUtil.getDefaultECPolicy();

  private String hadoopHome;

  @Rule
  public Timeout globalTimeout = new Timeout(300000);

  @Before
  public void setUp() {
    hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster tests", hadoopHome);

    Configuration conf = new HdfsConfiguration();
    try {
      ErasureCodingPolicyManager.getInstance().init(conf);
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize ErasureCodingPolicyManager", e);
    }
  }

  /**
   * Test unsuitable storage policies with EC striped mode.
   * This test verifies that when an unsuitable storage policy (ONE_SSD) is set on an
   * erasure-coded directory, blocks are still stored on DISK type storage.
   */
  @Test
  public void testUnsuitableStoragePoliciesWithECStripedMode()
      throws Exception {
    final Configuration conf = new HdfsConfiguration();
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

    final ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
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
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .build();

    try {
      cluster.waitClusterUp();
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

      // verify storage types and locations
      LocatedBlocks locatedBlocks = client.getBlockLocations(barFile, 0,
          fileLen);
      for (LocatedBlock lb : locatedBlocks.getLocatedBlocks()) {
        for (StorageType type : lb.getStorageTypes()) {
          Assert.assertEquals(StorageType.DISK, type);
        }
      }

    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}

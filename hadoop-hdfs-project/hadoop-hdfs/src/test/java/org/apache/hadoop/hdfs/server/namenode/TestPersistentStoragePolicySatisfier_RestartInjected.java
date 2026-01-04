/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.hadoop.fs.XAttr;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSCluster.DataNodeProperties;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.server.balancer.NameNodeConnector;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil;
import org.apache.hadoop.hdfs.server.namenode.sps.StoragePolicySatisfier;
import org.apache.hadoop.hdfs.server.sps.ExternalSPSContext;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import java.util.function.Supplier;

import java.io.IOException;
import java.util.List;

import static org.apache.hadoop.hdfs.server.common.HdfsServerConstants.XATTR_SATISFY_STORAGE_POLICY;
import static org.junit.Assert.*;

/**
 * Test persistence of satisfying files/directories.
 */
public class TestPersistentStoragePolicySatisfier_RestartInjected {
  private static Configuration conf;

  private static MiniDFSCluster cluster;
  private static DistributedFileSystem fs;
  private NameNodeConnector nnc;
  private StoragePolicySatisfier sps;
  private ExternalSPSContext ctxt;

  private static Path testFile =
      new Path("/testFile");
  private static String testFileName = testFile.toString();

  private static Path parentDir = new Path("/parentDir");
  private static Path parentFile = new Path(parentDir, "parentFile");
  private static String parentFileName = parentFile.toString();
  private static Path childDir = new Path(parentDir, "childDir");
  private static Path childFile = new Path(childDir, "childFile");
  private static String childFileName = childFile.toString();

  private static final String COLD = "COLD";
  private static final String WARM = "WARM";
  private static final String ONE_SSD = "ONE_SSD";

  private static StorageType[][] storageTypes = new StorageType[][] {
      {StorageType.DISK, StorageType.ARCHIVE, StorageType.SSD},
      {StorageType.DISK, StorageType.ARCHIVE, StorageType.SSD},
      {StorageType.DISK, StorageType.ARCHIVE, StorageType.SSD}
  };

  private final int timeout = 90000;

  /**
   * Setup environment for every test case.
   * @throws IOException
   */
  public void clusterSetUp() throws Exception {
    clusterSetUp(false, new HdfsConfiguration());
  }

  /**
   * Setup environment for every test case.
   * @param hdfsConf hdfs conf.
   * @throws Exception
   */
  public void clusterSetUp(Configuration hdfsConf) throws Exception {
    clusterSetUp(false, hdfsConf);
  }

  /**
   * Setup cluster environment.
   * @param isHAEnabled if true, enable simple HA.
   * @throws IOException
   */
  private void clusterSetUp(boolean isHAEnabled, Configuration newConf)
      throws Exception {
    conf = newConf;
    conf.set(
        DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_RECHECK_TIMEOUT_MILLIS_KEY,
        "3000");
    conf.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.EXTERNAL.toString());
    // Reduced refresh cycle to update latest datanodes.
    conf.setLong(DFSConfigKeys.DFS_SPS_DATANODE_CACHE_REFRESH_INTERVAL_MS,
        1000);
    conf.setInt(
        DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MAX_RETRY_ATTEMPTS_KEY, 20);
    final int dnNumber = storageTypes.length;
    final short replication = 3;
    MiniDFSCluster.Builder clusterBuilder = new MiniDFSCluster.Builder(conf)
        .storageTypes(storageTypes).storagesPerDatanode(3)
        .numDataNodes(dnNumber);
    if (isHAEnabled) {
      clusterBuilder.nnTopology(MiniDFSNNTopology.simpleHATopology());
    }
    cluster = clusterBuilder.build();
    cluster.waitActive();
    if (isHAEnabled) {
      cluster.transitionToActive(0);
      fs = HATestUtil.configureFailoverFs(cluster, conf);
    } else {
      fs = cluster.getFileSystem();
    }
    nnc = DFSTestUtil.getNameNodeConnector(conf,
        HdfsServerConstants.MOVER_ID_PATH, 1, false);

    sps = new StoragePolicySatisfier(conf);
    ctxt = new ExternalSPSContext(sps, nnc);

    sps.init(ctxt);
    sps.start(StoragePolicySatisfierMode.EXTERNAL);

    createTestFiles(fs, replication);
  }

  /**
   * Setup test files for testing.
   * @param dfs
   * @param replication
   * @throws Exception
   */
  private void createTestFiles(DistributedFileSystem dfs,
      short replication) throws Exception {
    DFSTestUtil.createFile(dfs, testFile, 1024L, replication, 0L);
    DFSTestUtil.createFile(dfs, parentFile, 1024L, replication, 0L);
    DFSTestUtil.createFile(dfs, childFile, 1024L, replication, 0L);

    DFSTestUtil.waitReplication(dfs, testFile, replication);
    DFSTestUtil.waitReplication(dfs, parentFile, replication);
    DFSTestUtil.waitReplication(dfs, childFile, replication);
  }

  /**
   * Tear down environment for every test case.
   * @throws IOException
   */
  private void clusterShutdown() throws IOException{
    if(fs != null) {
      fs.close();
      fs = null;
    }
    if(cluster != null) {
      cluster.shutdown(true);
      cluster = null;
    }
    if (sps != null) {
      sps.stopGracefully();
    }
  }

  /**
   * While satisfying file/directory, trigger the cluster's checkpoint to
   * make sure satisfier persistence work as expected. This test case runs
   * as below:
   * 1. use satisfyStoragePolicy and add xAttr to the file.
   * 2. do the checkpoint by secondary NameNode.
   * 3. restart the cluster immediately.
   * 4. make sure all the storage policies are satisfied.
   * @throws Exception
   */
  @Test
  public void testWithCheckpoint() throws Exception {
    SecondaryNameNode secondary = null;
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(testFile, WARM);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("after_satisfy_policy_file")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Start the checkpoint.
      conf.set(
          DFSConfigKeys.DFS_NAMENODE_SECONDARY_HTTP_ADDRESS_KEY, "0.0.0.0:0");
      secondary = new SecondaryNameNode(conf);
      secondary.doCheckpoint();
      RestartFramework.at("after_checkpoint")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      restartCluster();

      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.DISK, 1, timeout, fs);
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.ARCHIVE, 2, timeout, fs);
      RestartFramework.at("after_storage_verification")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      fs.setStoragePolicy(parentDir, COLD);
      fs.satisfyStoragePolicy(parentDir);
      RestartFramework.at("after_satisfy_policy_dir")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      DFSTestUtil.waitExpectedStorageType(
          parentFileName, StorageType.ARCHIVE, 3, timeout, fs);
      DFSTestUtil.waitExpectedStorageType(
          childFileName, StorageType.ARCHIVE, 3, timeout, fs);

    } finally {
      if (secondary != null) {
        secondary.shutdown();
      }
      clusterShutdown();
    }
  }

  /**
   * Tests to verify satisfier persistence working well with multiple
   * restarts operations. This test case runs as below:
   * 1. satisfy the storage policy of file1.
   * 2. restart the cluster.
   * 3. check whether all the blocks are satisfied.
   * 4. satisfy the storage policy of file2.
   * 5. restart the cluster.
   * 6. check whether all the blocks are satisfied.
   * @throws Exception
   */
  @Test
  public void testWithRestarts() throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(testFile, ONE_SSD);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("before_first_restart")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      restartCluster();
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.SSD, 1, timeout, fs);
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.DISK, 2, timeout, fs);
      RestartFramework.at("after_first_verification")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // test directory
      fs.setStoragePolicy(parentDir, COLD);
      fs.satisfyStoragePolicy(parentDir);
      RestartFramework.at("before_second_restart")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      restartCluster();
      DFSTestUtil.waitExpectedStorageType(
          parentFileName, StorageType.ARCHIVE, 3, timeout, fs);
      DFSTestUtil.waitExpectedStorageType(
          childFileName, StorageType.ARCHIVE, 3, timeout, fs);
    } finally {
      clusterShutdown();
    }
  }

  /**
   * Tests to verify SPS xattr will be removed if the satisfy work has
   * been finished, expect that the method satisfyStoragePolicy can be
   * invoked on the same file again after the block movement has been
   * finished:
   * 1. satisfy storage policy of file1.
   * 2. wait until storage policy is satisfied.
   * 3. satisfy storage policy of file1 again
   * 4. make sure step 3 works as expected.
   * @throws Exception
   */
  @Test
  public void testMultipleSatisfyStoragePolicy() throws Exception {
    try {
      // Lower block movement check for testing.
      conf = new HdfsConfiguration();
      final long minCheckTimeout = 500; // minimum value
      conf.setLong(
          DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_RECHECK_TIMEOUT_MILLIS_KEY,
          minCheckTimeout);
      clusterSetUp(conf);
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(testFile, ONE_SSD);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("after_first_satisfy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.SSD, 1, timeout, fs);
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.DISK, 2, timeout, fs);

      // Make sure satisfy xattr has been removed.
      DFSTestUtil.waitForXattrRemoved(testFileName,
          XATTR_SATISFY_STORAGE_POLICY, cluster.getNamesystem(), 30000);
      RestartFramework.at("after_xattr_removed")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      fs.setStoragePolicy(testFile, COLD);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("after_second_satisfy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(
          testFileName, StorageType.ARCHIVE, 3, timeout, fs);
    } finally{
      clusterShutdown();
    }
  }

  /**
   * Tests to verify SPS xattr is removed after SPS is dropped,
   * expect that if the SPS is disabled/dropped, the SPS
   * xattr should be removed accordingly:
   * 1. satisfy storage policy of file1.
   * 2. drop SPS thread in block manager.
   * 3. make sure sps xattr is removed.
   * @throws Exception
   */
  @Test
  public void testDropSPS() throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(testFile, ONE_SSD);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("after_satisfy_policy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      cluster.getNamesystem().getBlockManager().getSPSManager()
          .changeModeEvent(StoragePolicySatisfierMode.NONE);
      RestartFramework.at("after_drop_sps")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Make sure satisfy xattr has been removed.
      DFSTestUtil.waitForXattrRemoved(testFileName,
          XATTR_SATISFY_STORAGE_POLICY, cluster.getNamesystem(), 30000);

    } finally {
      clusterShutdown();
    }
  }

  /**
   * Tests that Xattrs should be cleaned if all blocks already satisfied.
   *
   * @throws Exception
   */
  @Test
  public void testSPSShouldNotLeakXattrIfStorageAlreadySatisfied()
      throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(testFileName, StorageType.DISK, 3,
          timeout, fs);
      fs.satisfyStoragePolicy(testFile);
      RestartFramework.at("after_satisfy_policy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      DFSTestUtil.waitExpectedStorageType(testFileName, StorageType.DISK, 3,
          timeout, fs);

      // Make sure satisfy xattr has been removed.
      DFSTestUtil.waitForXattrRemoved(testFileName,
          XATTR_SATISFY_STORAGE_POLICY, cluster.getNamesystem(), 30000);

    } finally {
      clusterShutdown();
    }
  }

  /**
   * Test loading of SPS xAttrs from the edits log when satisfyStoragePolicy
   * called on child file and parent directory.
   * 1. Create one directory and create one child file.
   * 2. Set storage policy for child file and call
   * satisfyStoragePolicy.
   * 3. wait for SPS to remove xAttr for file child file.
   * 4. Set storage policy for parent directory and call
   * satisfyStoragePolicy.
   * 5. restart the namenode.
   * NameNode should be started successfully.
   */
  @Test
  public void testNameNodeRestartWhenSPSCalledOnChildFileAndParentDir()
      throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(childFile, "COLD");
      fs.satisfyStoragePolicy(childFile);
      RestartFramework.at("after_satisfy_child_file")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(childFile.toUri().getPath(),
          StorageType.ARCHIVE, 3, 30000, cluster.getFileSystem());
      // wait for SPS to remove Xattr from file
      Thread.sleep(30000);
      fs.setStoragePolicy(childDir, "COLD");
      fs.satisfyStoragePolicy(childDir);
      RestartFramework.at("before_namenode_restart")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      try {
        cluster.restartNameNodes();
      } catch (Exception e) {
        assertFalse(e.getMessage().contains(
            "Cannot request to call satisfy storage policy"));
      }
    } finally {
      clusterShutdown();
    }
  }

  /**
   * Test SPS when satisfyStoragePolicy called on child file and
   * parent directory.
   * 1. Create one parent directory and child directory.
   * 2. Create some file in both the directory.
   * 3. Set storage policy for parent directory and call
   * satisfyStoragePolicy.
   * 4. Set storage policy for child directory and call
   * satisfyStoragePolicy.
   * 5. restart the namenode.
   * All the file blocks should satisfy the policy.
   */
  @Test
  public void testSPSOnChildAndParentDirectory() throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      fs.setStoragePolicy(parentDir, "COLD");
      fs.satisfyStoragePolicy(childDir);
      RestartFramework.at("after_satisfy_child_dir")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(childFileName, StorageType.ARCHIVE,
          3, 30000, cluster.getFileSystem());
      fs.satisfyStoragePolicy(parentDir);
      RestartFramework.at("after_satisfy_parent_dir")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      DFSTestUtil.waitExpectedStorageType(parentFileName, StorageType.ARCHIVE,
          3, 30000, cluster.getFileSystem());
    } finally {
      clusterShutdown();
    }
  }

  /**
   * Test SPS xAttr on directory. xAttr should be removed from the directory
   * once all the files blocks moved to specific storage.
   */
  @Test
  public void testSPSxAttrWhenSpsCalledForDir() throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      Path parent = new Path("/parent");
      // create parent dir
      fs.mkdirs(parent);

      // create 10 child files
      for (int i = 0; i < 5; i++) {
        DFSTestUtil.createFile(fs, new Path(parent, "f" + i), 1024, (short) 3,
            0);
      }
      RestartFramework.at("after_files_created")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Set storage policy for parent directory
      fs.setStoragePolicy(parent, "COLD");

      // Stop one DN so we can check the SPS xAttr for directory.
      DataNodeProperties stopDataNode = cluster.stopDataNode(0);

      fs.satisfyStoragePolicy(parent);
      RestartFramework.at("after_satisfy_policy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Check xAttr for parent directory
      FSNamesystem namesystem = cluster.getNamesystem();
      INode inode = namesystem.getFSDirectory().getINode("/parent");
      XAttrFeature f = inode.getXAttrFeature();
      assertTrue("SPS xAttr should be exist",
          f.getXAttr(XATTR_SATISFY_STORAGE_POLICY) != null);

      // check for the child, SPS xAttr should not be there
      for (int i = 0; i < 5; i++) {
        inode = namesystem.getFSDirectory().getINode("/parent/f" + i);
        f = inode.getXAttrFeature();
        assertTrue(f == null);
      }

      cluster.restartDataNode(stopDataNode, false);

      // wait and check all the file block moved in ARCHIVE
      for (int i = 0; i < 5; i++) {
        DFSTestUtil.waitExpectedStorageType("/parent/f" + i,
            StorageType.ARCHIVE, 3, 30000, cluster.getFileSystem());
      }
      DFSTestUtil.waitForXattrRemoved("/parent", XATTR_SATISFY_STORAGE_POLICY,
          namesystem, 10000);
    } finally {
      clusterShutdown();
    }

  }

  /**
   * Test SPS xAttr on file. xAttr should be removed from the file
   * once all the blocks moved to specific storage.
   */
  @Test
  public void testSPSxAttrWhenSpsCalledForFile() throws Exception {
    try {
      clusterSetUp();
      RestartFramework.at("after_cluster_setup")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      Path file = new Path("/file");
      DFSTestUtil.createFile(fs, file, 1024, (short) 3, 0);
      RestartFramework.at("after_file_created")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Set storage policy for file
      fs.setStoragePolicy(file, "COLD");

      // Stop one DN so we can check the SPS xAttr for file.
      DataNodeProperties stopDataNode = cluster.stopDataNode(0);

      fs.satisfyStoragePolicy(file);
      RestartFramework.at("after_satisfy_policy")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      // Check xAttr for parent directory
      FSNamesystem namesystem = cluster.getNamesystem();
      INode inode = namesystem.getFSDirectory().getINode("/file");
      XAttrFeature f = inode.getXAttrFeature();
      assertTrue("SPS xAttr should be exist",
          f.getXAttr(XATTR_SATISFY_STORAGE_POLICY) != null);

      cluster.restartDataNode(stopDataNode, false);

      // wait and check all the file block moved in ARCHIVE
      DFSTestUtil.waitExpectedStorageType("/file", StorageType.ARCHIVE, 3,
          30000, cluster.getFileSystem());
      GenericTestUtils.waitFor(new Supplier<Boolean>() {
        @Override
        public Boolean get() {
          List<XAttr> existingXAttrs = XAttrStorage.readINodeXAttrs(inode);
          return !existingXAttrs.contains(XATTR_SATISFY_STORAGE_POLICY);
        }
      }, 100, 10000);
    } finally {
      clusterShutdown();
    }
  }

  /**
   * Restart the hole env and trigger the DataNode's heart beats.
   * @throws Exception
   */
  private void restartCluster() throws Exception {
    cluster.restartDataNodes();
    cluster.restartNameNodes();
    cluster.waitActive();
    cluster.triggerHeartbeats();
  }

}

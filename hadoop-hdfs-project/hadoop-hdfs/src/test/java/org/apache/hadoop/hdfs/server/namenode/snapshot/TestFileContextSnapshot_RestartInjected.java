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
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.SnapshotException;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileContextSnapshot_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestFileContextSnapshot_RestartInjected.class);
  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;
  private static final long SEED = 0;
  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileContext fileContext;
  private DistributedFileSystem dfs;

  private final String snapshotRoot = "/snapshot";
  private final Path filePath = new Path(snapshotRoot, "file1");
  private Path snapRootPath;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    cluster.waitActive();
    fileContext = FileContext.getFileContext(conf);
    dfs = (DistributedFileSystem) cluster.getFileSystem();
    snapRootPath = new Path(snapshotRoot);
    dfs.mkdirs(snapRootPath);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_NN_Graceful() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_NN_Crash() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_DN_Graceful() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_DN_Crash() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_AllDN_Graceful() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_AllDN_Crash() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_RandomDN_Graceful() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }

  @Test(timeout = 120000)
  public void testCreateAndDeleteSnapshot_AfterCreate_RandomDN_Crash() throws Exception {
    DFSTestUtil.createFile(dfs, filePath, BLOCKSIZE, REPLICATION, SEED);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    dfs.disallowSnapshot(snapRootPath);
    try {
      fileContext.createSnapshot(snapRootPath, "s1");
    } catch (SnapshotException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + snapRootPath, e);
    }
    dfs.allowSnapshot(snapRootPath);
    Path ssPath = fileContext.createSnapshot(snapRootPath, "s1");
    assertTrue("Failed to create snapshot", dfs.exists(ssPath));
    fileContext.deleteSnapshot(snapRootPath, "s1");
    assertFalse("Failed to delete snapshot", dfs.exists(ssPath));
  }
}

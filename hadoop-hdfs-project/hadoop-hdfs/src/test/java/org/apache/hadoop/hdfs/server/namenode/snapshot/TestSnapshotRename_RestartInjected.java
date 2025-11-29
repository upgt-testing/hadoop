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
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.NSQuotaExceededException;
import org.apache.hadoop.hdfs.protocol.SnapshotException;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenode.snapshot.DirectoryWithSnapshotFeature.DirectoryDiff;
import org.apache.hadoop.hdfs.util.ReadOnlyList;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.LambdaTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSnapshotRename_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotRename_RestartInjected.class);

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final long BLOCKSIZE = 1024;

  private final Path dir = new Path("/TestSnapshot");
  private final Path sub1 = new Path(dir, "sub1");
  private final Path file1 = new Path(sub1, "file1");

  Configuration conf;
  MiniDFSCluster cluster;
  FSNamesystem fsn;
  DistributedFileSystem hdfs;
  FSDirectory fsdir;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt(CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    cluster.waitActive();
    fsn = cluster.getNamesystem();
    hdfs = cluster.getFileSystem();
    fsdir = fsn.getFSDirectory();
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private void checkSnapshotList(INodeDirectory srcRoot,
      String[] sortedNames, String[] names) {
    assertTrue(srcRoot.isSnapshottable());
    ReadOnlyList<Snapshot> listByName = srcRoot
        .getDirectorySnapshottableFeature().getSnapshotList();
    assertEquals(sortedNames.length, listByName.size());
    for (int i = 0; i < listByName.size(); i++) {
      assertEquals(sortedNames[i], listByName.get(i).getRoot().getLocalName());
    }
    DiffList<DirectoryDiff> listByTime = srcRoot.getDiffs().asList();
    assertEquals(names.length, listByTime.size());
    for (int i = 0; i < listByTime.size(); i++) {
      Snapshot s = srcRoot.getDirectorySnapshottableFeature().getSnapshotById(
          listByTime.get(i).getSnapshotId());
      assertEquals(names[i], s.getRoot().getLocalName());
    }
  }


  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_NN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_NN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_NN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_NN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_DN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_DN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_DN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_DN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_AllDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_AllDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_AllDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_AllDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_RandomDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_RandomDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_RandomDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_RandomDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_NNAndDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_NNAndDN_Graceful ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterCreateSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterCreateSnapshot_NNAndDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");
    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

  @Test(timeout = 120000)
  public void testSnapshotRename_AfterRenameSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testSnapshotRename_AfterRenameSnapshot_NNAndDN_Crash ===");

    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    Path snapshotRoot = SnapshotTestHelper.createSnapshot(hdfs, sub1, "s1");
    Path ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusBeforeRename = hdfs.getFileStatus(ssPath);
    hdfs.renameSnapshot(sub1, "s1", "s2");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    assertFalse(hdfs.exists(ssPath));
    snapshotRoot = SnapshotTestHelper.getSnapshotRoot(sub1, "s2");
    ssPath = new Path(snapshotRoot, file1.getName());
    assertTrue(hdfs.exists(ssPath));
    FileStatus statusAfterRename = hdfs.getFileStatus(ssPath);
    assertFalse(statusBeforeRename.equals(statusAfterRename));
    statusBeforeRename.setPath(statusAfterRename.getPath());
    assertEquals(statusBeforeRename.toString(), statusAfterRename.toString());
  }

}

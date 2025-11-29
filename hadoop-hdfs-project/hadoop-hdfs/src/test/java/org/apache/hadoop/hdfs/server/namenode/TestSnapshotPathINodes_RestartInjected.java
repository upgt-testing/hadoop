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

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.SnapshotException;
import org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot;
import org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSnapshotPathINodes_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotPathINodes_RestartInjected.class);

  private static final long seed = 0;
  private static final short REPLICATION = 3;

  private final Path dir = new Path("/TestSnapshot");
  private final Path sub1 = new Path(dir, "sub1");
  private final Path file1 = new Path(sub1, "file1");
  private final Path file2 = new Path(sub1, "file2");

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FSDirectory fsdir;
  private DistributedFileSystem hdfs;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt(CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    cluster.waitActive();
    FSNamesystem fsn = cluster.getNamesystem();
    fsdir = fsn.getFSDirectory();
    hdfs = cluster.getFileSystem();
    DFSTestUtil.createFile(hdfs, file1, 1024, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, 1024, REPLICATION, seed);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }


  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_NN_Graceful ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_NN_Crash ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_DN_Graceful ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_DN_Crash ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_AllDN_Graceful ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_AllDN_Crash ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_RandomDN_Graceful ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_RandomDN_Crash ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_NNAndDN_Graceful ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  @Test(timeout = 120000)
  public void testAllowSnapshot_AfterAllowSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testAllowSnapshot_AfterAllowSnapshot_NNAndDN_Crash ===");
    final String pathStr = sub1.toString();
    final INode before = fsdir.getINode(pathStr);
    Assert.assertFalse(before.asDirectory().isSnapshottable());
    final Path path = new Path(pathStr);
    hdfs.allowSnapshot(path);
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertTrue(after.asDirectory().isSnapshottable());
    }
    hdfs.disallowSnapshot(path);
    {
      final INode after = fsdir.getINode(pathStr);
      Assert.assertFalse(after.asDirectory().isSnapshottable());
    }
  }

  static Snapshot getSnapshot(INodesInPath inodesInPath, String name, int index) {
    if (name == null) {
      return null;
    }
    final INode inode = inodesInPath.getINode(index - 1);
    return inode.asDirectory().getSnapshot(DFSUtil.string2Bytes(name));
  }

  static void assertSnapshot(INodesInPath inodesInPath, boolean isSnapshot,
      final Snapshot snapshot, int index) {
    assertEquals(isSnapshot, inodesInPath.isSnapshot());
    assertEquals(Snapshot.getSnapshotId(isSnapshot ? snapshot : null),
        inodesInPath.getPathSnapshotId());
    if (!isSnapshot) {
      assertEquals(Snapshot.getSnapshotId(snapshot),
          inodesInPath.getLatestSnapshotId());
    }
    if (isSnapshot && index >= 0) {
      assertEquals(Snapshot.Root.class, inodesInPath.getINode(index).getClass());
    }
  }

  static void assertINodeFile(INode inode, Path path) {
    assertEquals(path.getName(), inode.getLocalName());
    assertEquals(INodeFile.class, inode.getClass());
  }

  @Test (timeout=15000)
  public void testNonSnapshotPathINodes() throws Exception {
    byte[][] components = INode.getPathComponents(file1.toString());
    INodesInPath nodesInPath = INodesInPath.resolve(fsdir.rootDir, components, false);
    assertEquals(nodesInPath.length(), components.length);
    assertSnapshot(nodesInPath, false, null, -1);
    for (int i=0; i < components.length; i++) {
      assertEquals(components[i], nodesInPath.getPathComponent(i));
    }
    assertTrue("file1=" + file1 + ", nodesInPath=" + nodesInPath,
        nodesInPath.getINode(components.length - 1) != null);
    assertEquals(nodesInPath.getINode(components.length - 1).getFullPathName(), file1.toString());
    assertEquals(nodesInPath.getINode(components.length - 2).getFullPathName(), sub1.toString());
    assertEquals(nodesInPath.getINode(components.length - 3).getFullPathName(), dir.toString());
    assertEquals(Path.SEPARATOR, nodesInPath.getPath(0));
    assertEquals(dir.toString(), nodesInPath.getPath(1));
    assertEquals(sub1.toString(), nodesInPath.getPath(2));
    assertEquals(file1.toString(), nodesInPath.getPath(3));
    assertEquals(file1.getParent().toString(), nodesInPath.getParentINodesInPath().getPath());
    nodesInPath = INodesInPath.resolve(fsdir.rootDir, components, false);
    assertEquals(nodesInPath.length(), components.length);
    assertSnapshot(nodesInPath, false, null, -1);
    assertEquals(nodesInPath.getLastINode().getFullPathName(), file1.toString());
  }

  private int getNumNonNull(INodesInPath iip) {
    for (int i = iip.length() - 1; i >= 0; i--) {
      if (iip.getINode(i) != null) {
        return i+1;
      }
    }
    return 0;
  }

  @Test
  public void testShortCircuitSnapshotSearch() throws SnapshotException {
    FSNamesystem fsn = cluster.getNamesystem();
    SnapshotManager sm = fsn.getSnapshotManager();
    assertEquals(0, sm.getNumSnapshottableDirs());
    INodesInPath iip = Mockito.mock(INodesInPath.class);
    List<INodeDirectory> snapDirs = new ArrayList<>();
    FSDirSnapshotOp.checkSnapshot(fsn.getFSDirectory(), iip, snapDirs);
    Mockito.verifyZeroInteractions(iip);
  }
}

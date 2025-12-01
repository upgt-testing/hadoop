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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.Options.Rename;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.SnapshottableDirectoryStatus;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSnapshottableDirListing_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshottableDirListing_RestartInjected.class);

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final long BLOCKSIZE = 1024;

  private final Path root = new Path("/");
  private final Path dir1 = new Path("/TestSnapshot1");
  private final Path dir2 = new Path("/TestSnapshot2");

  Configuration conf;
  MiniDFSCluster cluster;
  FSNamesystem fsn;
  DistributedFileSystem hdfs;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION)
        .build();
    cluster.waitActive();
    fsn = cluster.getNamesystem();
    hdfs = cluster.getFileSystem();
    hdfs.mkdirs(dir1);
    hdfs.mkdirs(dir2);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_NN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_NN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_DN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_DN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_AllDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_AllDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_RandomDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_RandomDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_NNAndDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListSnapshottableDir_AfterAllowDir1_NNAndDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    final Path root = new Path("/");
    hdfs.allowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals("", dirs[0].getDirStatus().getLocalName());
    assertEquals(root, dirs[0].getFullPath());
    hdfs.disallowSnapshot(root);
    dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(0, dirs[0].getSnapshotNumber());
    hdfs.allowSnapshot(dir2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(0, dirs[1].getSnapshotNumber());
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.rename(dir3, dir2, Rename.OVERWRITE);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    hdfs.allowSnapshot(dir2);
    hdfs.createSnapshot(dir2, "s1");
    hdfs.createSnapshot(dir2, "s2");
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(2, dirs[1].getSnapshotNumber());
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    hdfs.allowSnapshot(sub1);
    hdfs.allowSnapshot(sub2);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub1, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    hdfs.disallowSnapshot(sub1);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(sub2, dirs[2].getFullPath());
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir2.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[0].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_NN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_NN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_DN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_DN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_AllDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_AllDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_RandomDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_RandomDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_NNAndDN_Graceful() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }

  @Test (timeout=120000)
  public void testListWithDifferentUser_AfterAllowInitial_NNAndDN_Crash() throws Exception {
    cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
    hdfs.allowSnapshot(dir1);
    hdfs.allowSnapshot(dir2);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    hdfs.allowSnapshot(dir1_user1);
    hdfs.allowSnapshot(dir2_user1);
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    hdfs.allowSnapshot(dir_user2);
    hdfs.allowSnapshot(subdir_user2);
    String supergroup = conf.get(DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    assertEquals(6, dirs.length);
    dirs = fs1.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    dirs = fs2.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }
}

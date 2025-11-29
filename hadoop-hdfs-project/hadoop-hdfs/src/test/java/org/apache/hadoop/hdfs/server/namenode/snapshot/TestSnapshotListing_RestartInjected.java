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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSnapshotListing_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotListing_RestartInjected.class);

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final long BLOCKSIZE = 1024;

  private final Path dir = new Path("/test.snapshot/dir");

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
    hdfs.mkdirs(dir);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_NN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_NN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_DN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_DN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_AllDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_AllDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_RandomDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_RandomDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_NNAndDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterAllowSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterAllowSnapshot_NNAndDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_NN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_NN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_DN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_DN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_AllDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_AllDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_RandomDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_RandomDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_NNAndDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterCreateSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterCreateSnapshot_NNAndDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    hdfs.createSnapshot(dir, "s_0");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(1, stats.length);
    assertEquals("s_0", stats[0].getPath().getName());

    for (int sNum = 1; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    for (int sNum = snapshotNum - 1; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_NN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_NN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_DN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_DN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_AllDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_AllDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_RandomDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_RandomDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_NNAndDN_Graceful ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }

  @Test(timeout = 120000)
  public void testListSnapshots_AfterDeleteSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testListSnapshots_AfterDeleteSnapshot_NNAndDN_Crash ===");
    final Path snapshotsPath = new Path(dir, ".snapshot");
    FileStatus[] stats = null;

    stats = hdfs.listStatus(new Path("/.snapshot"));
    assertEquals(0, stats.length);

    try {
      stats = hdfs.listStatus(snapshotsPath);
      fail("expect SnapshotException");
    } catch (IOException e) {
      GenericTestUtils.assertExceptionContains(
          "Directory is not a snapshottable directory: " + dir.toString(), e);
    }

    hdfs.allowSnapshot(dir);
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);

    final int snapshotNum = 5;
    for (int sNum = 0; sNum < snapshotNum; sNum++) {
      hdfs.createSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum + 1, stats.length);
      for (int i = 0; i <= sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_4");

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(4, stats.length);
    for (int i = 0; i < 4; i++) {
      assertEquals("s_" + i, stats[i].getPath().getName());
    }

    for (int sNum = snapshotNum - 2; sNum > 0; sNum--) {
      hdfs.deleteSnapshot(dir, "s_" + sNum);
      stats = hdfs.listStatus(snapshotsPath);
      assertEquals(sNum, stats.length);
      for (int i = 0; i < sNum; i++) {
        assertEquals("s_" + i, stats[i].getPath().getName());
      }
    }

    hdfs.deleteSnapshot(dir, "s_0");
    stats = hdfs.listStatus(snapshotsPath);
    assertEquals(0, stats.length);
  }
}

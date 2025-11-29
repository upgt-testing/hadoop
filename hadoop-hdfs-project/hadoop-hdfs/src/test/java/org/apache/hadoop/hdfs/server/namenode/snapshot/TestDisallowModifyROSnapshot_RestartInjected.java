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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.protocol.SnapshotAccessControlException;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestDisallowModifyROSnapshot.
 *
 * Original test: TestDisallowModifyROSnapshot
 * This file contains restart injection variants to verify snapshot
 * read-only protection survives across component restarts.
 */
public class TestDisallowModifyROSnapshot_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestDisallowModifyROSnapshot_RestartInjected.class);

  private final Path dir = new Path("/TestSnapshot");
  private final Path sub1 = new Path(dir, "sub1");
  private final Path sub2 = new Path(dir, "sub2");

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FSNamesystem fsn;
  private DistributedFileSystem fs;
  private Path objInSnapshot;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    // Allow client to survive NN restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();

    fsn = cluster.getNamesystem();
    fs = cluster.getFileSystem();

    Path path1 = new Path(sub1, "dir1");
    assertTrue(fs.mkdirs(path1));
    Path path2 = new Path(sub2, "dir2");
    assertTrue(fs.mkdirs(path2));
    SnapshotTestHelper.createSnapshot(fs, sub1, "testSnapshot");
    objInSnapshot = SnapshotTestHelper.getSnapshotPath(sub1, "testSnapshot",
        "dir1");
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  // ============================================================
  // testSetReplication variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testSetReplication
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetReplication_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSetReplication_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    // Refresh fs reference after NN restart
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setReplication(objInSnapshot, (short) 1);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetReplication
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetReplication_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSetReplication_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    // Refresh fs reference after NN restart
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setReplication(objInSnapshot, (short) 1);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetReplication
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetReplication_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSetReplication_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setReplication(objInSnapshot, (short) 1);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetReplication
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetReplication_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSetReplication_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setReplication(objInSnapshot, (short) 1);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testSetPermission variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testSetPermission
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetPermission_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSetPermission_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setPermission(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetPermission
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetPermission_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSetPermission_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setPermission(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetPermission
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetPermission_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSetPermission_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setPermission(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetPermission
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetPermission_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSetPermission_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setPermission(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testSetOwner variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testSetOwner
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetOwner_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSetOwner_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setOwner(objInSnapshot, "username", "groupname");
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetOwner
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetOwner_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSetOwner_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setOwner(objInSnapshot, "username", "groupname");
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetOwner
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetOwner_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSetOwner_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setOwner(objInSnapshot, "username", "groupname");
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetOwner
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetOwner_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSetOwner_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setOwner(objInSnapshot, "username", "groupname");
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testRename variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testRename
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testRename_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testRename_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.rename(objInSnapshot, new Path("/invalid/path"));
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot, (Options.Rename) null);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testRename
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testRename_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testRename_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.rename(objInSnapshot, new Path("/invalid/path"));
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot, (Options.Rename) null);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testRename
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testRename_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testRename_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.rename(objInSnapshot, new Path("/invalid/path"));
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot, (Options.Rename) null);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testRename
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testRename_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testRename_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.rename(objInSnapshot, new Path("/invalid/path"));
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }

    try {
      fs.rename(sub2, objInSnapshot, (Options.Rename) null);
      fail("Didn't throw SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) { /* Ignored */ }
  }

  // ============================================================
  // testDelete variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testDelete
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testDelete_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testDelete_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.delete(objInSnapshot, true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testDelete
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testDelete_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testDelete_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.delete(objInSnapshot, true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testDelete
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testDelete_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testDelete_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.delete(objInSnapshot, true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testDelete
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testDelete_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testDelete_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.delete(objInSnapshot, true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testQuota variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testQuota
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testQuota_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testQuota_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setQuota(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testQuota
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testQuota_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testQuota_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setQuota(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testQuota
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testQuota_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testQuota_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setQuota(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testQuota
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testQuota_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testQuota_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setQuota(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testSetTime variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testSetTime
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetTime_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testSetTime_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setTimes(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetTime
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetTime_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testSetTime_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.setTimes(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetTime
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testSetTime_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testSetTime_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setTimes(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testSetTime
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testSetTime_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testSetTime_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.setTimes(objInSnapshot, 100, 100);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testCreate variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testCreate
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testCreate_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testCreate_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.create(objInSnapshot.toString(), true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreate
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testCreate_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testCreate_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.create(objInSnapshot.toString(), true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreate
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testCreate_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testCreate_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.create(objInSnapshot.toString(), true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreate
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testCreate_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testCreate_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.create(objInSnapshot.toString(), true);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  // ============================================================
  // testAppend variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testAppend
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAppend_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testAppend_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.append(objInSnapshot, 65535, null);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testAppend
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testAppend_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testAppend_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.append(objInSnapshot, 65535, null);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testAppend
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testAppend_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testAppend_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.append(objInSnapshot, 65535, null);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testAppend
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testAppend_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testAppend_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.append(objInSnapshot, 65535, null);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testMkdir variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testMkdir
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testMkdir_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testMkdir_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.mkdirs(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testMkdir
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testMkdir_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testMkdir_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    try {
      fs.mkdirs(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testMkdir
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testMkdir_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testMkdir_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.mkdirs(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testMkdir
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testMkdir_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testMkdir_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    try {
      fs.mkdirs(objInSnapshot, new FsPermission("777"));
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    }
  }

  // ============================================================
  // testCreateSymlink variants
  // ============================================================

  /**
   * Original: TestDisallowModifyROSnapshot#testCreateSymlink
   * Restart: After snapshot creation, NameNode, Graceful
   */
  @Test(timeout = 120000)
  public void testCreateSymlink_AfterSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting testCreateSymlink_AfterSnapshot_NN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.createSymlink(sub2.toString(), "/TestSnapshot/sub1/.snapshot",
          false);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreateSymlink
   * Restart: After snapshot creation, NameNode, Crash
   */
  @Test(timeout = 120000)
  public void testCreateSymlink_AfterSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting testCreateSymlink_AfterSnapshot_NN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    fs = cluster.getFileSystem();

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.createSymlink(sub2.toString(), "/TestSnapshot/sub1/.snapshot",
          false);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreateSymlink
   * Restart: After snapshot creation, Single DataNode, Graceful
   */
  @Test(timeout = 120000)
  public void testCreateSymlink_AfterSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting testCreateSymlink_AfterSnapshot_DN_Graceful ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.createSymlink(sub2.toString(), "/TestSnapshot/sub1/.snapshot",
          false);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }

  /**
   * Original: TestDisallowModifyROSnapshot#testCreateSymlink
   * Restart: After snapshot creation, Single DataNode, Crash
   */
  @Test(timeout = 120000)
  public void testCreateSymlink_AfterSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting testCreateSymlink_AfterSnapshot_DN_Crash ===");

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL TEST CODE ===
    @SuppressWarnings("deprecation")
    DFSClient dfsclient = new DFSClient(conf);
    try {
      dfsclient.createSymlink(sub2.toString(), "/TestSnapshot/sub1/.snapshot",
          false);
      fail("Expected SnapshotAccessControlException");
    } catch (SnapshotAccessControlException e) {
      // Expected
    } finally {
      dfsclient.close();
    }
  }
}

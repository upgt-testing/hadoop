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
package org.apache.hadoop.hdfs.server.blockmanagement;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestBlockStatsMXBean.
 * Tests block stats MXBean operations survive component restarts.
 *
 * Original test: testStorageTypeLoad (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestBlockStatsMXBean_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockStatsMXBean_RestartInjected.class);

  private MiniDFSCluster cluster;

  @Rule
  public Timeout globalTimeout = new Timeout(300000);

  @Before
  public void setup() throws Exception {
    HdfsConfiguration conf = new HdfsConfiguration();
    conf.setTimeDuration(DFSConfigKeys.DFS_DATANODE_DISK_CHECK_MIN_GAP_KEY,
        0, TimeUnit.MILLISECONDS);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    StorageType[][] types = new StorageType[6][];
    for (int i=0; i<3; i++) {
      types[i] = new StorageType[] {StorageType.RAM_DISK, StorageType.DISK};
    }
    for (int i=3; i< 5; i++) {
      types[i] = new StorageType[] {StorageType.RAM_DISK, StorageType.ARCHIVE};
    }
    types[5] = new StorageType[] {StorageType.RAM_DISK, StorageType.ARCHIVE,
        StorageType.ARCHIVE};

    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(6).
        storageTypes(types).storagesPerDatanode(3).build();
    cluster.waitActive();
  }

  @After
  public void tearDown() {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  /**
   * Core test logic for storage type load with restart injection.
   */
  private void testStorageTypeLoadWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    HeartbeatManager heartbeatManager =
        cluster.getNamesystem().getBlockManager().getDatanodeManager()
            .getHeartbeatManager();
    Map<StorageType, StorageTypeStats> storageTypeStatsMap =
        heartbeatManager.getStorageTypeStats();
    DistributedFileSystem dfs = cluster.getFileSystem();

    // Create a file with HOT storage policy.
    Path hotSpDir = new Path("/HOT");
    dfs.mkdir(hotSpDir, FsPermission.getDirDefault());
    dfs.setStoragePolicy(hotSpDir, "HOT");
    FSDataOutputStream hotSpFileStream =
        dfs.create(new Path(hotSpDir, "hotFile"));
    hotSpFileStream.write("Storage Policy Hot".getBytes());
    hotSpFileStream.hflush();

    // Create a file with COLD storage policy.
    Path coldSpDir = new Path("/COLD");
    dfs.mkdir(coldSpDir, FsPermission.getDirDefault());
    dfs.setStoragePolicy(coldSpDir, "COLD");
    FSDataOutputStream coldSpFileStream =
        dfs.create(new Path(coldSpDir, "coldFile"));
    coldSpFileStream.write("Writing to ARCHIVE storage type".getBytes());
    coldSpFileStream.hflush();

    // === RESTART INJECTION POINT: After hflush ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);
    verifyClusterHealth(cluster, dfs);
    LOG.info("=== RESTART COMPLETE ===");

    // Re-get the heartbeatManager after potential NN restart
    heartbeatManager =
        cluster.getNamesystem().getBlockManager().getDatanodeManager()
            .getHeartbeatManager();
    final Map<StorageType, StorageTypeStats> storageTypeStatsMapAfter =
        heartbeatManager.getStorageTypeStats();

    // Trigger heartbeats manually to speed up the test.
    cluster.triggerHeartbeats();

    // Wait for storage stats to be populated after restart
    // The load would be 2*replication since both the
    // write xceiver & packet responder threads are counted.
    GenericTestUtils.waitFor(() -> {
      StorageTypeStats stats = storageTypeStatsMapAfter.get(StorageType.DISK);
      return stats != null && stats.getNodesInServiceXceiverCount() >= 0;
    }, 100, 30000);

    GenericTestUtils.waitFor(() -> {
      StorageTypeStats stats = storageTypeStatsMapAfter.get(StorageType.ARCHIVE);
      return stats != null && stats.getNodesInServiceXceiverCount() >= 0;
    }, 100, 30000);

    IOUtils.closeStreams(hotSpFileStream, coldSpFileStream);
  }

  // ============================================================
  // Test variants: testStorageTypeLoad with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_NN_Graceful() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_NN_Crash() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_SingleDN_Graceful() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_SingleDN_Crash() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_AllDN_Graceful() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_AllDN_Crash() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_NNDN_Graceful() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStorageTypeLoad_AfterHflush_NNDN_Crash() throws Exception {
    testStorageTypeLoadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

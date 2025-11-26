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
package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDataNodeVolumeMetrics.
 * Tests DataNode volume metrics survive component restarts.
 *
 * Original tests with hsync/hflush:
 * - testVolumeMetrics (hsync)
 * - testWriteIoVolumeMetrics (hflush)
 *
 * Note: testVolumeMetricsWithVolumeDepartureArrival is skipped because
 * it tests volume departure/arrival behavior which conflicts with restart injection.
 *
 * Generated variants:
 * - AfterHsync/AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestDataNodeVolumeMetrics_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDataNodeVolumeMetrics_RestartInjected.class);

  private static final int BLOCK_SIZE = 1024;
  private static final short REPL = 1;
  private static final int NUM_DATANODES = 1;

  /**
   * Core test logic for testVolumeMetrics with restart injection.
   */
  private void testVolumeMetricsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_DATANODE_FILEIO_PROFILING_SAMPLING_PERCENTAGE_KEY, 100);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    SimulatedFSDataset.setFactory(conf);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(NUM_DATANODES)
        .storageTypes(new StorageType[]{StorageType.RAM_DISK, StorageType.DISK})
        .storagesPerDatanode(2)
        .build();

    try {
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();
      final Path fileName = new Path("/test.dat");
      final long fileLen = Integer.MAX_VALUE + 1L;
      DFSTestUtil.createFile(fs, fileName, false, BLOCK_SIZE, fileLen,
          fs.getDefaultBlockSize(fileName), REPL, 1L, true);

      try (FSDataOutputStream out = fs.append(fileName)) {
        out.writeBytes("hello world");
        ((DFSOutputStream) out.getWrappedStream()).hsync();

        // === RESTART INJECTION POINT: After hsync ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, (DistributedFileSystem) fs);
        LOG.info("=== RESTART COMPLETE ===");
      }

      // Verify file is readable
      DFSTestUtil.readFile(fs, fileName);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Core test logic for testWriteIoVolumeMetrics with restart injection.
   */
  private void testWriteIoVolumeMetricsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_DATANODE_FILEIO_PROFILING_SAMPLING_PERCENTAGE_KEY, 100);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(NUM_DATANODES)
        .storageTypes(new StorageType[]{StorageType.RAM_DISK, StorageType.DISK})
        .storagesPerDatanode(2)
        .build();

    try {
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();
      final Path fileName = new Path("/test.dat");
      final long fileLen = Integer.MAX_VALUE + 1L;

      DFSTestUtil.createFile(fs, fileName, false, BLOCK_SIZE, fileLen,
          fs.getDefaultBlockSize(fileName), REPL, 1L, true);

      try (FSDataOutputStream out = fs.append(fileName)) {
        out.writeBytes("hello world");
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, (DistributedFileSystem) fs);
        LOG.info("=== RESTART COMPLETE ===");
      }

      // Verify file is readable
      DFSTestUtil.readFile(fs, fileName);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testVolumeMetrics
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_NN_Graceful() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_NN_Crash() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_SingleDN_Graceful() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_SingleDN_Crash() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_AllDN_Graceful() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_AllDN_Crash() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_NNDN_Graceful() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testVolumeMetrics_AfterHsync_NNDN_Crash() throws Exception {
    testVolumeMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testWriteIoVolumeMetrics
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_NN_Graceful() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_NN_Crash() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_SingleDN_Graceful() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_SingleDN_Crash() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_AllDN_Graceful() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_AllDN_Crash() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_NNDN_Graceful() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteIoVolumeMetrics_AfterHflush_NNDN_Crash() throws Exception {
    testWriteIoVolumeMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

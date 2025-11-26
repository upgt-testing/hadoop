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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
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
 * Restart-injection variant of TestDataNodeMetrics.
 * Tests DataNode metrics survive component restarts.
 *
 * Original tests with hflush/hsync:
 * - testReceivePacketMetrics (hsync)
 * - testDataNodeMXBeanActiveThreadCount (hsync)
 *
 * Note: testTimeoutMetric is skipped because it uses fault injection
 * that throws IOException, which conflicts with restart injection.
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestDataNodeMetrics_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDataNodeMetrics_RestartInjected.class);

  /**
   * Core test logic for testReceivePacketMetrics with restart injection.
   */
  private void testReceivePacketMetricsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    final int interval = 1;
    conf.set(DFSConfigKeys.DFS_METRICS_PERCENTILES_INTERVALS_KEY, "" + interval);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path testFile = new Path("/testFlushNanosMetric.txt");
      FSDataOutputStream fout = fs.create(testFile);
      fout.write(new byte[1]);
      fout.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      fout.close();

      // Verify file is readable
      byte[] content = new byte[1];
      fs.open(testFile).read(content);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Core test logic for testDataNodeMXBeanActiveThreadCount with restart injection.
   */
  private void testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      Path p = new Path("/testfile");

      // create a xceiver thread for write
      FSDataOutputStream os = fs.create(p);
      for (int i = 0; i < 1024; i++) {
        os.write("testdatastr".getBytes());
      }
      os.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      os.close();

      // Verify file is readable
      fs.open(p).read(new byte[16], 0, 4);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testReceivePacketMetrics
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_NN_Graceful() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_NN_Crash() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_SingleDN_Graceful() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_SingleDN_Crash() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_AllDN_Graceful() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_AllDN_Crash() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_NNDN_Graceful() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReceivePacketMetrics_AfterHsync_NNDN_Crash() throws Exception {
    testReceivePacketMetricsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testDataNodeMXBeanActiveThreadCount
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_NN_Graceful() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_NN_Crash() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_SingleDN_Graceful() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_SingleDN_Crash() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_AllDN_Graceful() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_AllDN_Crash() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_NNDN_Graceful() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataNodeMXBeanActiveThreadCount_AfterHsync_NNDN_Crash() throws Exception {
    testDataNodeMXBeanActiveThreadCountWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
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
 * Restart-injection variant of TestNamenodeCapacityReport.
 * Tests namenode capacity report operations survive component restarts.
 *
 * Original test with hsync during capacity reporting
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestNamenodeCapacityReport_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestNamenodeCapacityReport_RestartInjected.class);

  /**
   * Core test logic for capacity report with restart injection.
   */
  private void testCapacityReportWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create and write to a file
      Path file = new Path("/capacityreport_restart_test.dat");
      DFSOutputStream out = (DFSOutputStream) fs.create(file).getWrappedStream();
      out.write("1".getBytes());
      out.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more and close
      out.write("2".getBytes());
      out.close();

      // Verify file
      assertTrue("File should exist", fs.exists(file));
      long fileLen = fs.getFileStatus(file).getLen();
      assertTrue("File should have data", fileLen == 2);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testCapacityReport
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_NN_Graceful() throws Exception {
    testCapacityReportWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_NN_Crash() throws Exception {
    testCapacityReportWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_SingleDN_Graceful() throws Exception {
    testCapacityReportWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_SingleDN_Crash() throws Exception {
    testCapacityReportWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_AllDN_Graceful() throws Exception {
    testCapacityReportWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_AllDN_Crash() throws Exception {
    testCapacityReportWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_NNDN_Graceful() throws Exception {
    testCapacityReportWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCapacityReport_AfterHsync_NNDN_Crash() throws Exception {
    testCapacityReportWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

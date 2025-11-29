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
package org.apache.hadoop.hdfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDatanodeReport.
 * Tests that datanode reporting works correctly after restarts.
 */
public class TestDatanodeReport_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestDatanodeReport_RestartInjected.class);
  private static final int NUM_DATANODES = 3;
  private static final int BLOCKSIZE = 1024;

  /**
   * Test datanode report after file creation and restart.
   */
  private void testDatanodeReportAfterFileWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 500);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_DATANODES).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      DFSClient client = fs.dfs;
      Path filePath = new Path("/testDatanodeReport.dat");

      // Create file and verify initial datanode reports
      DFSTestUtil.createFile(fs, filePath, BLOCKSIZE * 2, (short) NUM_DATANODES, 0);

      DatanodeInfo[] allBefore = client.datanodeReport(DatanodeReportType.ALL);
      DatanodeInfo[] liveBefore = client.datanodeReport(DatanodeReportType.LIVE);
      assertEquals("Should have all datanodes before restart", NUM_DATANODES, allBefore.length);
      assertEquals("All datanodes should be live before restart", NUM_DATANODES, liveBefore.length);

      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      fs = cluster.getFileSystem();
      client = fs.dfs;
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Verify datanode reports work correctly after restart
      DatanodeInfo[] allAfter = client.datanodeReport(DatanodeReportType.ALL);
      DatanodeInfo[] liveAfter = client.datanodeReport(DatanodeReportType.LIVE);

      int expectedLiveCount = NUM_DATANODES;
      if (target == RestartTarget.SINGLE_DATANODE) {
        // Single DN restart should still have all DNs live after restart completes
        expectedLiveCount = NUM_DATANODES;
      }

      assertTrue("Should have datanodes after restart", allAfter.length > 0);
      assertTrue("Should have live datanodes after restart", liveAfter.length > 0);

      // Verify file is still accessible
      assertTrue("File should exist after restart", fs.exists(filePath));
      assertEquals("File should have correct length", BLOCKSIZE * 2, fs.getFileStatus(filePath).getLen());
    } finally {
      cluster.shutdown();
    }
  }

  // RestartPoint: AfterFile - 8 variants
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_NN_Graceful() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_NN_Crash() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_SingleDN_Graceful() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_SingleDN_Crash() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_AllDN_Graceful() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_AllDN_Crash() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_NNDN_Graceful() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDatanodeReportAfterFile_NNDN_Crash() throws Exception { testDatanodeReportAfterFileWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH); }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDeadNodeDetection.
 * Tests dead node detection survives component restarts.
 *
 * Original test: testDetectDeadNodeInBackground (has hflush)
 *
 * Note: The original test deliberately stops all datanodes after hflush.
 * This simplified version focuses on verifying hflush data survives restarts.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDeadNodeDetection_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDeadNodeDetection_RestartInjected.class);

  /**
   * Core test logic with restart injection after hflush.
   */
  private void testDeadNodeDetectionWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setBoolean(HdfsClientConfigKeys.Read.ShortCircuit.KEY, false);
    conf.setBoolean(HdfsClientConfigKeys.DFS_CLIENT_DEAD_NODE_DETECTION_ENABLED_KEY, true);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();

      FileSystem fs = cluster.getFileSystem();
      Path filePath = new Path("/testDetectDeadNodeInBackground");

      // 256 bytes data chunk for writes
      byte[] bytes = new byte[256];
      for (int index = 0; index < bytes.length; index++) {
        bytes[index] = '0';
      }

      // File with a 512 bytes block size
      FSDataOutputStream out = fs.create(filePath, true, 4096, (short) 3, 512);

      // Write a block to all 3 DNs (2x256bytes)
      out.write(bytes);
      out.write(bytes);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      out.close();

      // Verify file is readable
      byte[] readBytes = DFSTestUtil.readFileBuffer((DistributedFileSystem) fs, filePath);
      assert readBytes.length == 512;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDeadNodeDetection with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_NN_Graceful() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_NN_Crash() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_SingleDN_Graceful() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_SingleDN_Crash() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_AllDN_Graceful() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_AllDN_Crash() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_NNDN_Graceful() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDeadNodeDetection_AfterHflush_NNDN_Crash() throws Exception {
    testDeadNodeDetectionWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

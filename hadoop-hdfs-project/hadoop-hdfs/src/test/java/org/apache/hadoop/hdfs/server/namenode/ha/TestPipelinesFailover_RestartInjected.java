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
package org.apache.hadoop.hdfs.server.namenode.ha;

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestPipelinesFailover.
 * Tests pipeline recovery during NN failover survives DataNode restarts.
 *
 * Note: This is an HA test, so we only inject DataNode restarts
 * (NameNode already has failover logic).
 *
 * Original test with hflush during pipeline failover
 *
 * Generated variants:
 * - AfterHflush x 2 DN targets x 2 RestartModes = 4 variants
 */
public class TestPipelinesFailover_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestPipelinesFailover_RestartInjected.class);

  private static final Path TEST_PATH = new Path("/test-file");
  private static final int BLOCK_SIZE = 4096;
  private static final int BLOCK_AND_A_HALF = BLOCK_SIZE * 3 / 2;

  /**
   * Core test logic for pipeline failover with restart injection.
   */
  private void testPipelineFailoverWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    // Don't check low redundancy periodically.
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1000);

    FSDataOutputStream stm = null;
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(5).build();
    try {
      int sizeWritten = 0;

      cluster.waitActive();
      cluster.transitionToActive(0);
      Thread.sleep(500);

      LOG.info("Starting with NN 0 active");
      FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);
      stm = fs.create(TEST_PATH);

      // write a block and a half
      AppendTestUtil.write(stm, 0, BLOCK_AND_A_HALF);
      sizeWritten += BLOCK_AND_A_HALF;

      // Make sure all of the blocks are written out
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data
      AppendTestUtil.write(stm, sizeWritten, BLOCK_AND_A_HALF);
      sizeWritten += BLOCK_AND_A_HALF;

      stm.close();
      stm = null;

      AppendTestUtil.check(fs, TEST_PATH, sizeWritten);
    } finally {
      IOUtils.closeStream(stm);
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testPipelineFailover (HA test - DN restarts only)
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testPipelineFailover_AfterHflush_SingleDN_Graceful() throws Exception {
    testPipelineFailoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineFailover_AfterHflush_SingleDN_Crash() throws Exception {
    testPipelineFailoverWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipelineFailover_AfterHflush_AllDN_Graceful() throws Exception {
    testPipelineFailoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineFailover_AfterHflush_AllDN_Crash() throws Exception {
    testPipelineFailoverWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }
}

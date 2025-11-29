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
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
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
 * Restart-injection variant of TestHASafeMode.
 * Tests HA safemode operations survive DataNode restarts.
 *
 * Note: This is an HA test, so we only inject DataNode restarts
 * (NameNode already has HA failover logic).
 *
 * Original test with hflush during HA safemode operations
 *
 * Generated variants:
 * - AfterHflush x 2 DN targets x 2 RestartModes = 4 variants
 */
public class TestHASafeMode_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestHASafeMode_RestartInjected.class);

  static final int BLOCK_SIZE = 1024;

  /**
   * Core test logic for HA safemode with restart injection.
   */
  private void testHASafeModeWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(3)
        .build();
    try {
      cluster.transitionToActive(0);
      cluster.waitActive();

      FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);

      // Create initial blocks
      DFSTestUtil.createFile(fs, new Path("/other-blocks"), 10 * BLOCK_SIZE, (short) 3, 1L);

      // Create UC files with hflush
      FSDataOutputStream stm = fs.create(new Path("/test-uc-0"));
      stm.write(1);
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      stm.write(2);
      stm.close();

      // Verify files
      assertTrue("File should exist", fs.exists(new Path("/test-uc-0")));
      assertTrue("Other blocks should exist", fs.exists(new Path("/other-blocks")));
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testHASafeMode (HA test - DN restarts only)
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testHASafeMode_AfterHflush_SingleDN_Graceful() throws Exception {
    testHASafeModeWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHASafeMode_AfterHflush_SingleDN_Crash() throws Exception {
    testHASafeModeWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHASafeMode_AfterHflush_AllDN_Graceful() throws Exception {
    testHASafeModeWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHASafeMode_AfterHflush_AllDN_Crash() throws Exception {
    testHASafeModeWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }
}

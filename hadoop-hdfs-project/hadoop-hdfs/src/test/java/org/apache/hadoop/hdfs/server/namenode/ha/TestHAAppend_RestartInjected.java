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

import java.util.concurrent.ThreadLocalRandom;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestHAAppend.
 * Tests HA append operations survive DataNode restarts.
 *
 * Note: This is an HA test, so we only inject DataNode restarts
 * (NameNode already has failover logic).
 *
 * Original test with hflush during HA append catchup tailing
 *
 * Generated variants:
 * - AfterHflush x 2 DN targets x 2 RestartModes = 4 variants
 */
public class TestHAAppend_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestHAAppend_RestartInjected.class);
  static final int COUNT = 5;

  /**
   * Core test logic for HA append with restart injection.
   */
  private void testHAAppendWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();

    // Set a length edits tailing period, and explicit rolling
    conf.set(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, "5000");
    conf.setInt(DFSConfigKeys.DFS_HA_LOGROLL_PERIOD_KEY, -1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(3).build();
    FileSystem fs = null;
    try {
      cluster.transitionToActive(0);
      fs = HATestUtil.configureFailoverFs(cluster, conf);

      Path file = new Path("/haappend_restart_test.dat");

      final byte[] data = new byte[1 << 16];
      ThreadLocalRandom.current().nextBytes(data);
      final int[] appendPos = AppendTestUtil.randomFilePartition(data.length, COUNT);

      // Create file, write some data, and hflush
      FSDataOutputStream out = fs.create(file, false, 4096, (short) 3, 1024);
      out.write(data, 0, appendPos[0]);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Let the StandbyNode catch the creation of the file
      cluster.getNameNode(0).getRpcServer().rollEditLog();
      cluster.getNameNode(1).getNamesystem().getEditLogTailer().doTailEdits();
      out.close();

      // Append more data
      for (int i = 0; i < COUNT; i++) {
        int end = i < COUNT - 1 ? appendPos[i + 1] : data.length;
        out = fs.append(file);
        out.write(data, appendPos[i], end - appendPos[i]);
        out.close();
      }

      // Verify the file
      AppendTestUtil.checkFullFile(fs, file, data.length, data, file.toString());
    } finally {
      if (null != cluster) {
        cluster.shutdown();
      }
      if (null != fs) {
        fs.close();
      }
    }
  }

  // ============================================================
  // Test variants: testHAAppend (HA test - DN restarts only)
  // AfterHflush x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testHAAppend_AfterHflush_SingleDN_Graceful() throws Exception {
    testHAAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHAAppend_AfterHflush_SingleDN_Crash() throws Exception {
    testHAAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHAAppend_AfterHflush_AllDN_Graceful() throws Exception {
    testHAAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHAAppend_AfterHflush_AllDN_Crash() throws Exception {
    testHAAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }
}

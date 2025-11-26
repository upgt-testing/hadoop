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

import static org.junit.Assert.fail;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster.DataNodeProperties;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.util.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDFSClientExcludedNodes.
 * Tests DFSClient excluded nodes handling survives component restarts.
 *
 * Original test: testExcludedNodesForgiveness (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDFSClientExcludedNodes_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSClientExcludedNodes_RestartInjected.class);

  /**
   * Core test logic for excluded nodes forgiveness with restart injection.
   */
  private void testExcludedNodesForgivenessWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Forgive nodes in under 2.5s for this test case.
    conf.setLong(
        HdfsClientConfigKeys.Write.EXCLUDE_NODES_CACHE_EXPIRY_INTERVAL_KEY,
        2500);
    // We'll be using a 512 bytes block size just for tests
    // so making sure the checksum bytes too match it.
    conf.setInt("io.bytes.per.checksum", 512);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
      FileSystem fs = cluster.getFileSystem();
      Path filePath = new Path("/testForgivingExcludedNodes");

      // 256 bytes data chunk for writes
      byte[] bytes = new byte[256];
      for (int index = 0; index < bytes.length; index++) {
        bytes[index] = '0';
      }

      // File with a 512 bytes block size
      FSDataOutputStream out = fs.create(filePath, true, 4096, (short) 3, 512);

      // Write a block to all 3 DNs (2x256bytes).
      out.write(bytes);
      out.write(bytes);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Remove two DNs, to put them into the exclude list.
      DataNodeProperties two = cluster.stopDataNode(2);
      DataNodeProperties one = cluster.stopDataNode(1);

      // Write another block.
      // At this point, we have two nodes already in excluded list.
      out.write(bytes);
      out.write(bytes);
      out.hflush();

      // Bring back the older DNs, since they are gonna be forgiven only
      // afterwards of this previous block write.
      Assert.assertEquals(true, cluster.restartDataNode(one, true));
      Assert.assertEquals(true, cluster.restartDataNode(two, true));
      cluster.waitActive();

      // Sleep for 5s, to let the excluded nodes be expired
      // from the excludes list (i.e. forgiven after the configured wait period).
      ThreadUtil.sleepAtLeastIgnoreInterrupts(5000);

      // Terminate the last good DN, to assert that there's no
      // single-DN-available scenario, caused by not forgiving the other
      // two by now.
      cluster.stopDataNode(0);

      try {
        // Attempt writing another block, which should still pass
        // cause the previous two should have been forgiven by now,
        // while the last good DN added to excludes this time.
        out.write(bytes);
        out.hflush();
        out.close();
      } catch (Exception e) {
        fail("Excluded DataNodes should be forgiven after a while and " +
            "not cause file writing exception of: '" + e.getMessage() + "'");
      }
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testExcludedNodesForgiveness with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_NN_Graceful() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_NN_Crash() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_SingleDN_Graceful() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_SingleDN_Crash() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_AllDN_Graceful() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_AllDN_Crash() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_NNDN_Graceful() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testExcludedNodesForgiveness_AfterHflush_NNDN_Crash() throws Exception {
    testExcludedNodesForgivenessWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

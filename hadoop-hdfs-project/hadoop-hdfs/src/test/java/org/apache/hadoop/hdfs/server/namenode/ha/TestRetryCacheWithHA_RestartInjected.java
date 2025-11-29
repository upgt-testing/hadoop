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

import java.util.EnumSet;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestRetryCacheWithHA.
 * Tests retry cache with HA survives DataNode restarts.
 *
 * Note: This is an HA test, so we only inject DataNode restarts
 * (NameNode already has HA failover logic).
 *
 * Original test with hsync during retry cache operations
 *
 * Generated variants:
 * - AfterHsync x 2 DN targets x 2 RestartModes = 4 variants
 */
public class TestRetryCacheWithHA_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestRetryCacheWithHA_RestartInjected.class);

  static final int BLOCK_SIZE = 1024;
  static final short DATA_NODES = 3;

  /**
   * Core test logic for retry cache with HA with restart injection.
   */
  private void testRetryCacheWithHAWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_ENABLE_RETRY_CACHE_KEY, true);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .nnTopology(MiniDFSNNTopology.simpleHATopology())
        .numDataNodes(DATA_NODES)
        .build();
    try {
      cluster.transitionToActive(0);
      cluster.waitActive();

      FileSystem fs = HATestUtil.configureFailoverFs(cluster, conf);

      // Create file
      Path file = new Path("/retrycache_restart_test.dat");
      DFSTestUtil.createFile(fs, file, BLOCK_SIZE, DATA_NODES, 0);

      // Append with hsync
      FSDataOutputStream out = fs.append(file);
      byte[] appendContent = new byte[100];
      new Random().nextBytes(appendContent);
      out.write(appendContent);
      ((HdfsDataOutputStream) out).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close stream
      out.close();

      // Verify
      assertTrue("File should exist", fs.exists(file));
      long fileLen = fs.getFileStatus(file).getLen();
      assertTrue("File should have all data", fileLen == BLOCK_SIZE + appendContent.length);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testRetryCacheWithHA (HA test - DN restarts only)
  // AfterHsync x 2 DN targets x 2 modes = 4 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testRetryCacheWithHA_AfterHsync_SingleDN_Graceful() throws Exception {
    testRetryCacheWithHAWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRetryCacheWithHA_AfterHsync_SingleDN_Crash() throws Exception {
    testRetryCacheWithHAWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRetryCacheWithHA_AfterHsync_AllDN_Graceful() throws Exception {
    testRetryCacheWithHAWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRetryCacheWithHA_AfterHsync_AllDN_Crash() throws Exception {
    testRetryCacheWithHAWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }
}

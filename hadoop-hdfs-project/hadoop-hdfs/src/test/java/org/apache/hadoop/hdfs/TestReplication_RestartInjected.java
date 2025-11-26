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

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestReplication.
 * Tests replication operations survive component restarts.
 *
 * Original test with hflush before verifying replication
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestReplication_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestReplication_RestartInjected.class);

  /**
   * Core test logic for replication with restart injection.
   */
  private void testReplicationWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create file with replication factor 3
      Path file = new Path("/replication_restart_test.dat");
      FSDataOutputStream stm = fs.create(file, true, 4096, (short) 3, fs.getDefaultBlockSize(file));

      // Write and flush
      byte[] buffer = AppendTestUtil.initBuffer(4096);
      stm.write(buffer);
      stm.hflush(); // make sure blocks are persisted

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more and close
      stm.write(buffer);
      stm.close();

      // Verify file
      long fileLen = fs.getFileStatus(file).getLen();
      assertTrue("File should have all data", fileLen == 8192);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testReplication
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_NN_Graceful() throws Exception {
    testReplicationWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_NN_Crash() throws Exception {
    testReplicationWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_SingleDN_Graceful() throws Exception {
    testReplicationWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_SingleDN_Crash() throws Exception {
    testReplicationWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_AllDN_Graceful() throws Exception {
    testReplicationWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_AllDN_Crash() throws Exception {
    testReplicationWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_NNDN_Graceful() throws Exception {
    testReplicationWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplication_AfterHflush_NNDN_Crash() throws Exception {
    testReplicationWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

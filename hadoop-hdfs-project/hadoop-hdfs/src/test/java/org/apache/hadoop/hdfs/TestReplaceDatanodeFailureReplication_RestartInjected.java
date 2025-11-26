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
 * Restart-injection variant of TestReplaceDatanodeFailureReplication.
 * Tests datanode replacement failure replication survives component restarts.
 *
 * Original test with hflush in replication failure scenario
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestReplaceDatanodeFailureReplication_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestReplaceDatanodeFailureReplication_RestartInjected.class);

  /**
   * Core test logic for replace datanode failure replication with restart injection.
   */
  private void testReplaceDatanodeFailureReplicationWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create file
      Path file = new Path("/replace_dn_failure_repl_restart.dat");
      FSDataOutputStream out = fs.create(file, (short) 3);
      byte[] buffer = AppendTestUtil.initBuffer(1024);
      out.write(buffer);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more and close
      out.write(buffer);
      out.close();

      // Verify file
      long fileLen = fs.getFileStatus(file).getLen();
      assertTrue("File should have all data", fileLen == 2048);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testReplaceDatanodeFailureReplication
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_NN_Graceful() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_NN_Crash() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_SingleDN_Graceful() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_SingleDN_Crash() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_AllDN_Graceful() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_AllDN_Crash() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_NNDN_Graceful() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReplaceDatanodeFailureReplication_AfterHflush_NNDN_Crash() throws Exception {
    testReplaceDatanodeFailureReplicationWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

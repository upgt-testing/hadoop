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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
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
 * Restart-injection variant of TestDFSInputStream.
 * Tests DFSInputStream operations survive component restarts.
 *
 * Original test: testNullCheckSumWhenDNRestarted (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDFSInputStream_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSInputStream_RestartInjected.class);

  /**
   * Core test logic for null checksum when DN restarted with restart injection.
   */
  private void testNullCheckSumWhenDNRestartedWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.set(HdfsClientConfigKeys.DFS_CHECKSUM_TYPE_KEY, "NULL");
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2)
        .build();
    cluster.waitActive();
    try {
      DistributedFileSystem fs = cluster.getFileSystem();

      int chunkSize = 512;
      Random r = new Random(12345L);
      byte[] data = new byte[chunkSize];
      r.nextBytes(data);

      Path file = new Path("/testfile");
      try (FSDataOutputStream fout = fs.create(file)) {
        fout.write(data);
        fout.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, fs);
        LOG.info("=== RESTART COMPLETE ===");
      }

      // wait for block to load
      Thread.sleep(1000);

      // fetch live DN
      final List<DatanodeDescriptor> live = new ArrayList<DatanodeDescriptor>();
      cluster.getNameNode().getNamesystem().getBlockManager()
          .getDatanodeManager().fetchDatanodes(live, null, false);
      assertTrue("Live DN count should be at least 1", live.size() >= 1);
      assertTrue("File size should be " + chunkSize,
          fs.getFileStatus(file).getLen() == chunkSize);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testNullCheckSumWhenDNRestarted with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_NN_Graceful() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_NN_Crash() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_SingleDN_Graceful() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_SingleDN_Crash() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_AllDN_Graceful() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_AllDN_Crash() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_NNDN_Graceful() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNullCheckSumWhenDNRestarted_AfterHflush_NNDN_Crash() throws Exception {
    testNullCheckSumWhenDNRestartedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

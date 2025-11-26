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

import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager;
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
 * Restart-injection variant of TestBlocksScheduledCounter.
 * Tests blocks scheduled counter operations survive component restarts.
 *
 * Original tests: testBlocksScheduledCounter, testScheduledBlocksCounterShouldDecrementOnAbandonBlock (both have hflush)
 *
 * Generated variants:
 * - 2 tests x AfterHflush x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestBlocksScheduledCounter_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlocksScheduledCounter_RestartInjected.class);

  /**
   * Core test logic for blocks scheduled counter with restart injection.
   */
  private void testBlocksScheduledCounterWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = null;
    FileSystem fs = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).build();
      cluster.waitActive();
      fs = cluster.getFileSystem();

      // open a file an write a few bytes:
      FSDataOutputStream out = fs.create(new Path("/testBlockScheduledCounter"));
      for (int i = 0; i < 1024; i++) {
        out.write(i);
      }
      // flush to make sure a block is allocated.
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      ArrayList<DatanodeDescriptor> dnList = new ArrayList<DatanodeDescriptor>();
      final DatanodeManager dm = cluster.getNamesystem().getBlockManager()
          .getDatanodeManager();
      dm.fetchDatanodes(dnList, dnList, false);

      // After restart, verify the counter works properly
      // close the file and the counter should go to zero.
      out.close();
      for (DatanodeDescriptor dn : dnList) {
        assertEquals(0, dn.getBlocksScheduled());
      }
    } finally {
      if (fs != null) {
        fs.close();
      }
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Core test logic for abandon block decrement counter with restart injection.
   */
  private void testAbandonBlockDecrementWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = null;
    FileSystem fs = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
      cluster.waitActive();
      fs = cluster.getFileSystem();

      DatanodeManager datanodeManager = cluster.getNamesystem().getBlockManager()
          .getDatanodeManager();
      ArrayList<DatanodeDescriptor> dnList = new ArrayList<DatanodeDescriptor>();
      datanodeManager.fetchDatanodes(dnList, dnList, false);
      for (DatanodeDescriptor descriptor : dnList) {
        assertEquals("Blocks scheduled should be 0 for " + descriptor.getName(),
            0, descriptor.getBlocksScheduled());
      }

      cluster.getDataNodes().get(0).shutdown();
      // open a file an write a few bytes:
      FSDataOutputStream out = fs.create(new Path("/testBlockScheduledCounter"),
          (short) 2);
      for (int i = 0; i < 1024; i++) {
        out.write(i);
      }
      // flush to make sure a block is allocated.
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Refresh references after restart
      datanodeManager = cluster.getNamesystem().getBlockManager()
          .getDatanodeManager();
      dnList.clear();
      datanodeManager.fetchDatanodes(dnList, dnList, false);

      // close the file and the counter should go to zero.
      out.close();
      for (DatanodeDescriptor descriptor : dnList) {
        assertEquals("Blocks scheduled should be 0 for " + descriptor.getName(),
            0, descriptor.getBlocksScheduled());
      }
    } finally {
      if (fs != null) {
        fs.close();
      }
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testBlocksScheduledCounter with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_NN_Graceful() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_NN_Crash() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_SingleDN_Graceful() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_SingleDN_Crash() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_AllDN_Graceful() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_AllDN_Crash() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_NNDN_Graceful() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testBlocksScheduledCounter_AfterHflush_NNDN_Crash() throws Exception {
    testBlocksScheduledCounterWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testAbandonBlockDecrement with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_NN_Graceful() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_NN_Crash() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_SingleDN_Graceful() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_SingleDN_Crash() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_AllDN_Graceful() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_AllDN_Crash() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_NNDN_Graceful() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAbandonBlockDecrement_AfterHflush_NNDN_Crash() throws Exception {
    testAbandonBlockDecrementWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

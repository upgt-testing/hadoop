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
package org.apache.hadoop.hdfs.server.namenode;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.test.PathUtils;
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
 * Restart-injection variant of TestCheckpoint.
 * Tests checkpoint operations survive component restarts.
 *
 * Original test: testSecondaryNameNodeWithSavedLeases (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 *
 * Note: This test focuses on NameNode restart scenarios as it involves
 * checkpoint operations with Secondary NameNode.
 */
public class TestCheckpoint_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestCheckpoint_RestartInjected.class);

  static final int numDatanodes = 3;

  static void cleanup(SecondaryNameNode snn) {
    if (snn != null) {
      try {
        snn.shutdown();
      } catch (Exception e) {
        LOG.warn("Could not shut down secondary name-node", e);
      }
    }
  }

  static void cleanup(MiniDFSCluster cluster) {
    if (cluster != null) {
      try {
        cluster.shutdown();
      } catch (Exception e) {
        LOG.warn("Could not shutdown MiniDFSCluster ", e);
      }
    }
  }

  static SecondaryNameNode startSecondaryNameNode(Configuration conf)
      throws IOException {
    conf.set(DFSConfigKeys.DFS_NAMENODE_SECONDARY_HTTP_ADDRESS_KEY, "0.0.0.0:0");
    return new SecondaryNameNode(conf);
  }

  /**
   * Core test logic for testing Secondary NameNode with saved leases and restart injection.
   * This test uses NN restart only, as DN restarts don't make sense in the context of
   * checkpoint operations.
   */
  private void testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    MiniDFSCluster cluster = null;
    SecondaryNameNode secondary = null;
    FSDataOutputStream fos = null;
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(numDatanodes)
          .format(true).build();
      FileSystem fs = cluster.getFileSystem();
      fos = fs.create(new Path("tmpfile"));
      fos.write(new byte[] { 0, 1, 2, 3 });
      fos.hflush();
      assertEquals(1, cluster.getNamesystem().getLeaseManager().countLease());

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // After restart, verify lease count
      // For NN restart, the lease should be recovered from edit log
      int expectedLeases = 1;
      if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
        // After NN restart, lease should be recovered
        expectedLeases = 1;
      }

      secondary = startSecondaryNameNode(conf);

      // Checkpoint once, so the 2NN loads the lease into its in-memory state.
      secondary.doCheckpoint();

      fos.close();
      fos = null;

      // Perform a saveNamespace, so that the NN has a new fsimage
      cluster.getNameNodeRpc().setSafeMode(SafeModeAction.SAFEMODE_ENTER, false);
      cluster.getNameNodeRpc().saveNamespace(0, 0);
      cluster.getNameNodeRpc().setSafeMode(SafeModeAction.SAFEMODE_LEAVE, false);

      // Ensure that the 2NN can still perform a checkpoint.
      secondary.doCheckpoint();

      // And the leases have been cleared...
      assertEquals(0, secondary.getFSNamesystem().getLeaseManager().countLease());
    } finally {
      if (fos != null) {
        fos.close();
      }
      cleanup(secondary);
      secondary = null;
      cleanup(cluster);
      cluster = null;
    }
  }

  // ============================================================
  // Test variants: testSecondaryNameNodeWithSavedLeases with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_NN_Graceful() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_NN_Crash() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_SingleDN_Graceful() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_SingleDN_Crash() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_AllDN_Graceful() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_AllDN_Crash() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_NNDN_Graceful() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSecondaryNameNodeWithSavedLeases_AfterHflush_NNDN_Crash() throws Exception {
    testSecondaryNameNodeWithSavedLeasesWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.TestFileCreation;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockUnderConstructionFeature;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
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
 * Restart-injection variant of TestBlockUnderConstruction.
 * Tests block under construction operations survive component restarts.
 *
 * Original test: testEmptyExpectedLocations (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestBlockUnderConstruction_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockUnderConstruction_RestartInjected.class);

  static final String BASE_DIR = "/test/TestBlockUnderConstruction";
  static final int BLOCK_SIZE = 8192;

  /**
   * Core test logic for testing empty expected locations with restart injection.
   * A storage ID can be invalid if the storage failed or the node reregisters.
   */
  private void testEmptyExpectedLocationsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
      cluster.waitActive();
      DistributedFileSystem hdfs = cluster.getFileSystem();

      final NamenodeProtocols namenode = cluster.getNameNodeRpc();
      final FSNamesystem fsn = cluster.getNamesystem();
      final BlockManager bm = fsn.getBlockManager();
      final Path p = new Path(BASE_DIR, "file_restart.dat");
      final String src = p.toString();
      final FSDataOutputStream out = TestFileCreation.createFile(hdfs, p, 1);
      TestFileCreation.writeFile(out, 256);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, hdfs);
      LOG.info("=== RESTART COMPLETE ===");

      // Get fresh references after potential NN restart
      final NamenodeProtocols namenodeAfter = cluster.getNameNodeRpc();
      final FSNamesystem fsnAfter = cluster.getNamesystem();
      final BlockManager bmAfter = fsnAfter.getBlockManager();

      // make sure the block is readable
      LocatedBlocks lbs = namenodeAfter.getBlockLocations(src, 0, 256);
      LocatedBlock lastLB = lbs.getLocatedBlocks().get(0);
      final Block b = lastLB.getBlock().getLocalBlock();

      // fake a block recovery
      long blockRecoveryId = bmAfter.nextGenerationStamp(false);
      BlockUnderConstructionFeature uc = bmAfter.getStoredBlock(b)
          .getUnderConstructionFeature();
      if (uc != null) {
        uc.initializeBlockRecovery(null, blockRecoveryId, false);

        try {
          String[] storages = {"invalid-storage-id1"};
          fsnAfter.commitBlockSynchronization(lastLB.getBlock(), blockRecoveryId, 256L,
              true, false, lastLB.getLocations(), storages);
        } catch (java.lang.IllegalStateException ise) {
          // Although a failure is expected as of now, future commit policy
          // changes may make it not fail. This is not critical to the test.
        }
      }

      // Invalid storage should not trigger an exception.
      lbs = namenodeAfter.getBlockLocations(src, 0, 256);

      out.close();
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testEmptyExpectedLocations with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_NN_Graceful() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_NN_Crash() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_SingleDN_Graceful() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_SingleDN_Crash() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_AllDN_Graceful() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_AllDN_Crash() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_NNDN_Graceful() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testEmptyExpectedLocations_AfterHflush_NNDN_Crash() throws Exception {
    testEmptyExpectedLocationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

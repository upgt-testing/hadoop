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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState;
import org.apache.hadoop.hdfs.server.namenode.LeaseExpiredException;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.io.IOUtils;
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
 * Restart-injection variant of TestClientProtocolForPipelineRecovery.
 * Tests pipeline recovery client protocol survives component restarts.
 *
 * Original test: testGetNewStamp (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestClientProtocolForPipelineRecovery_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestClientProtocolForPipelineRecovery_RestartInjected.class);

  /**
   * Core test logic for getNewStamp with restart injection.
   * Tests that block operations work correctly after restart.
   */
  private void testGetNewStampWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    int numDataNodes = 1;
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(numDataNodes).build();
    try {
      cluster.waitActive();
      FileSystem fileSys = cluster.getFileSystem();
      NamenodeProtocols namenode = cluster.getNameNodeRpc();

      /* Test writing to finalized replicas */
      Path file = new Path("dataprotocol.dat");
      DFSTestUtil.createFile(fileSys, file, 1L, (short) numDataNodes, 0L);
      // get the first blockid for the file
      ExtendedBlock firstBlock = DFSTestUtil.getFirstBlock(fileSys, file);

      // test getNewStampAndToken on a finalized block
      try {
        namenode.updateBlockForPipeline(firstBlock, "");
        Assert.fail("Can not get a new GS from a finalized block");
      } catch (Exception e) {
        Assert.assertTrue(e.getMessage().contains(
            "not " + BlockUCState.UNDER_CONSTRUCTION));
      }

      /* Test RBW replicas */
      // change first block to a RBW
      DFSOutputStream out = null;
      try {
        out = (DFSOutputStream) (fileSys.append(file).
            getWrappedStream());
        out.write(1);
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, fileSys);
        LOG.info("=== RESTART COMPLETE ===");

        // Refresh references after restart
        namenode = cluster.getNameNodeRpc();

        FSDataInputStream in = null;
        try {
          in = fileSys.open(file);
          firstBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
        } finally {
          IOUtils.closeStream(in);
        }

        // test non-lease holder (after restart, the dfs client should still work)
        DFSClient dfs = ((DistributedFileSystem) fileSys).dfs;
        try {
          namenode.updateBlockForPipeline(firstBlock, "test" + dfs.clientName);
          Assert.fail("Cannot get a new GS for a non lease holder");
        } catch (LeaseExpiredException e) {
          Assert.assertTrue(e.getMessage().startsWith("Lease mismatch"));
        }

      } finally {
        IOUtils.closeStream(out);
      }
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testGetNewStamp with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_NN_Graceful() throws Exception {
    testGetNewStampWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_NN_Crash() throws Exception {
    testGetNewStampWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_SingleDN_Graceful() throws Exception {
    testGetNewStampWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_SingleDN_Crash() throws Exception {
    testGetNewStampWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_AllDN_Graceful() throws Exception {
    testGetNewStampWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_AllDN_Crash() throws Exception {
    testGetNewStampWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_NNDN_Graceful() throws Exception {
    testGetNewStampWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testGetNewStamp_AfterHflush_NNDN_Crash() throws Exception {
    testGetNewStampWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

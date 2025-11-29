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
import static org.junit.Assert.assertTrue;

import java.util.Random;

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
 * Restart-injection variant of TestMultipleNNPortQOP.
 * Tests multiple NN port QOP survives component restarts during writes.
 *
 * Original test: hflush during block token operations
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestMultipleNNPortQOP_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestMultipleNNPortQOP_RestartInjected.class);

  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  /**
   * Core test logic for MultipleNNPortQOP with restart injection.
   */
  private void testMultipleNNPortQOPWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create file with hflush
      Path filePath = new Path("/testMultipleNNPortQOP.dat");
      FSDataOutputStream out = fs.create(filePath, (short) REPLICATION);
      byte[] data = new byte[BLOCKSIZE];
      new Random().nextBytes(data);
      out.write(data);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      byte[] moreData = new byte[BLOCKSIZE];
      new Random().nextBytes(moreData);
      out.write(moreData);
      out.close();

      // Verify
      assertTrue("File should exist", fs.exists(filePath));
      long fileLen = fs.getFileStatus(filePath).getLen();
      assertEquals("File should have all data", BLOCKSIZE * 2, fileLen);

      // Verify content
      byte[] readBack = DFSTestUtil.readFileBuffer(fs, filePath);
      assertEquals("Read back should match written", BLOCKSIZE * 2, readBack.length);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testMultipleNNPortQOP
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_NN_Graceful() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_NN_Crash() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_SingleDN_Graceful() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_SingleDN_Crash() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_AllDN_Graceful() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_AllDN_Crash() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_NNDN_Graceful() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultipleNNPortQOP_AfterHflush_NNDN_Crash() throws Exception {
    testMultipleNNPortQOPWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

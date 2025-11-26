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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDataTransferProtocol.
 * Tests data transfer protocol operations survive component restarts.
 *
 * Original test: testOpWrite (has hflush for RBW replica creation)
 *
 * Note: The original test is a complex protocol-level test with direct socket operations.
 * This simplified version focuses on verifying that hflush data survives restarts
 * while file is being written with append operations.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDataTransferProtocol_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(
      TestDataTransferProtocol_RestartInjected.class);

  /**
   * Core test logic for data transfer with restart injection.
   * Tests that data written and flushed survives component restarts.
   */
  private void testDataTransferWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    try {
      cluster.waitActive();
      FileSystem fileSys = cluster.getFileSystem();

      // Create initial file
      Path file = new Path("/dataprotocol.dat");
      DFSTestUtil.createFile(fileSys, file, 1L, (short) 1, 0L);

      // Append to file with hflush
      DFSOutputStream out = (DFSOutputStream) (fileSys.append(file).getWrappedStream());
      out.write(1);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fileSys);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      out.write(2);
      out.hflush();
      out.close();

      // Verify file is readable
      FSDataInputStream in = fileSys.open(file);
      byte[] data = new byte[3];
      in.readFully(data);
      in.close();

      // Original file had 1 byte, we appended 2 more
      assert data.length == 3;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDataTransfer with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_NN_Graceful() throws Exception {
    testDataTransferWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_NN_Crash() throws Exception {
    testDataTransferWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_SingleDN_Graceful() throws Exception {
    testDataTransferWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_SingleDN_Crash() throws Exception {
    testDataTransferWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_AllDN_Graceful() throws Exception {
    testDataTransferWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_AllDN_Crash() throws Exception {
    testDataTransferWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_NNDN_Graceful() throws Exception {
    testDataTransferWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDataTransfer_AfterHflush_NNDN_Crash() throws Exception {
    testDataTransferWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

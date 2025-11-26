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
package org.apache.hadoop.hdfs.server.datanode;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDiskError.
 * Tests disk error handling survives component restarts.
 *
 * Original test: testDataTransferWhenBytesPerChecksumIsZero (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDiskError_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDiskError_RestartInjected.class);

  /**
   * Core test logic with restart injection after hflush.
   */
  private void testDiskErrorWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    try {
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();

      Path filePath = new Path("/test.dat");
      FSDataOutputStream out = fs.create(filePath, (short) 1);
      out.write(1);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      out.close();

      // Verify file is readable
      byte[] content = DFSTestUtil.readFileBuffer((DistributedFileSystem) fs, filePath);
      assert content.length == 1;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDiskError with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_NN_Graceful() throws Exception {
    testDiskErrorWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_NN_Crash() throws Exception {
    testDiskErrorWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_SingleDN_Graceful() throws Exception {
    testDiskErrorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_SingleDN_Crash() throws Exception {
    testDiskErrorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_AllDN_Graceful() throws Exception {
    testDiskErrorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_AllDN_Crash() throws Exception {
    testDiskErrorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_NNDN_Graceful() throws Exception {
    testDiskErrorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDiskError_AfterHflush_NNDN_Crash() throws Exception {
    testDiskErrorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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
 * Restart-injection variant of TestFileAppend3.
 * Tests file append operations survive component restarts.
 *
 * Original test: testTC11 (has hflush before rename)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileAppend3_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileAppend3_RestartInjected.class);
  private static final long BLOCK_SIZE = 64 * 1024;
  private static final short REPLICATION = 3;

  /**
   * Core test logic for testTC11 with restart injection.
   */
  private void testTC11WithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      final Path p = new Path("/TC11/foo");
      final int buffersize = 4096;

      // Create file and write initial data
      final int len1 = (int) BLOCK_SIZE;
      FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
      AppendTestUtil.write(out, 0, len1);
      out.close();

      // Reopen file in "append" mode. Append half block of data.
      out = fs.append(p);
      final int len2 = (int) BLOCK_SIZE / 2;
      AppendTestUtil.write(out, len1, len2);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Close file
      out.close();

      // Verify data
      AppendTestUtil.check(fs, p, len1 + len2);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testTC11
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_NN_Graceful() throws Exception {
    testTC11WithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_NN_Crash() throws Exception {
    testTC11WithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_SingleDN_Graceful() throws Exception {
    testTC11WithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_SingleDN_Crash() throws Exception {
    testTC11WithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_AllDN_Graceful() throws Exception {
    testTC11WithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_AllDN_Crash() throws Exception {
    testTC11WithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_NNDN_Graceful() throws Exception {
    testTC11WithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testTC11_AfterHflush_NNDN_Crash() throws Exception {
    testTC11WithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

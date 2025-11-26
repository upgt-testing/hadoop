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

import org.apache.hadoop.fs.FSDataInputStream;
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
 * Restart-injection variant of TestFileAppend2.
 * Tests file append operations survive component restarts.
 *
 * Original test: testAppendLessThanChecksumChunk (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileAppend2_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileAppend2_RestartInjected.class);

  /**
   * Core test logic for testAppendLessThanChecksumChunk with restart injection.
   */
  private void testAppendLessThanChecksumChunkWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    final byte[] buf = new byte[1024];
    HdfsConfiguration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    final MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();

    try {
      DistributedFileSystem fs = cluster.getFileSystem();
      final int len1 = 200;
      final int len2 = 300;
      final Path p = new Path("/foo");

      FSDataOutputStream out = fs.create(p);
      out.write(buf, 0, len1);
      out.close();

      out = fs.append(p);
      out.write(buf, 0, len2);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // read data to verify the replica's content and checksum are correct
      FSDataInputStream in = fs.open(p);
      final int length = in.read(0, buf, 0, len1 + len2);
      assertTrue(length > 0);
      in.close();
      out.close();
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testAppendLessThanChecksumChunk
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_NN_Graceful() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_NN_Crash() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_SingleDN_Graceful() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_SingleDN_Crash() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_AllDN_Graceful() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_AllDN_Crash() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_NNDN_Graceful() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendLessThanChecksumChunk_AfterHflush_NNDN_Crash() throws Exception {
    testAppendLessThanChecksumChunkWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

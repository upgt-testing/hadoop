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

import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDataStream.
 * Tests DataStream operations survive component restarts.
 *
 * Original test: testDfsClient (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDataStream_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDataStream_RestartInjected.class);
  private static final int PACKET_SIZE = 1024;

  /**
   * Core test logic for testDfsClient with restart injection.
   */
  private void testDfsClientWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_WRITE_PACKET_SIZE_KEY, PACKET_SIZE);
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SLOW_IO_WARNING_THRESHOLD_KEY, 10000);
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SOCKET_TIMEOUT_KEY, 60000);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      byte[] toWrite = new byte[PACKET_SIZE];
      new Random(1).nextBytes(toWrite);
      final Path path = new Path("/file1");
      final DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream out = dfs.create(path, false);

      out.write(toWrite);
      out.write(toWrite);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, dfs);
      LOG.info("=== RESTART COMPLETE ===");

      out.write(toWrite);
      out.write(toWrite);
      out.hflush();

      out.close();

      // Verify file is readable
      byte[] content = DFSTestUtil.readFileBuffer(dfs, path);
      assert content.length == PACKET_SIZE * 4;
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testDfsClient with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_NN_Graceful() throws Exception {
    testDfsClientWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_NN_Crash() throws Exception {
    testDfsClientWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_SingleDN_Graceful() throws Exception {
    testDfsClientWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_SingleDN_Crash() throws Exception {
    testDfsClientWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_AllDN_Graceful() throws Exception {
    testDfsClientWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_AllDN_Crash() throws Exception {
    testDfsClientWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_NNDN_Graceful() throws Exception {
    testDfsClientWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDfsClient_AfterHflush_NNDN_Crash() throws Exception {
    testDfsClientWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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
package org.apache.hadoop.hdfs.server.datanode.fsdataset.impl;

import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDatanodeRestart.
 * Tests data persistence across component restarts.
 *
 * Original test: testRbwReplicas (has hflush)
 *
 * Note: The original test already tests DN restarts. This version adds
 * additional restart injection points (including NN restarts) after hflush.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestDatanodeRestart_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDatanodeRestart_RestartInjected.class);

  /**
   * Core test logic for RBW replicas with additional restart injection.
   */
  private void testRbwReplicasWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024L);
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_WRITE_PACKET_SIZE_KEY, 512);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();

    FSDataOutputStream out = null;
    FileSystem fs = cluster.getFileSystem();
    final Path src = new Path("/test.txt");

    try {
      final int fileLen = 515;
      byte[] writeBuf = new byte[fileLen];
      new Random().nextBytes(writeBuf);
      out = fs.create(src);
      out.write(writeBuf);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      out.write(writeBuf);
      out.close();
      out = null;

      // Verify file is readable
      byte[] readBuf = new byte[fileLen * 2];
      fs.open(src).readFully(readBuf);
    } finally {
      IOUtils.closeStream(out);
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testRbwReplicas with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_NN_Graceful() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_NN_Crash() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_SingleDN_Graceful() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_SingleDN_Crash() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_AllDN_Graceful() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_AllDN_Crash() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_NNDN_Graceful() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRbwReplicas_AfterHflush_NNDN_Crash() throws Exception {
    testRbwReplicasWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

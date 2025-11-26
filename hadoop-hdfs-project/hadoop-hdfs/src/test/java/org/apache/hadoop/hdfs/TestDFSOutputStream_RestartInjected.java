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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StreamCapabilities.StreamCapability;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDFSOutputStream.
 * Tests DFSOutputStream operations survive component restarts.
 *
 * Original test: testStreamFlush (has hflush and hsync)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 * Total: 16 variants
 */
public class TestDFSOutputStream_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSOutputStream_RestartInjected.class);

  /**
   * Core test logic for stream flush with restart injection after hflush.
   */
  private void testStreamFlushWithRestartAfterHflush(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    cluster.waitActive();
    try {
      FileSystem fs = cluster.getFileSystem();
      FSDataOutputStream os = fs.create(new Path("/normal-file"));
      // Verify output stream supports hsync() and hflush().
      assertTrue("DFSOutputStream should support hflush()!",
          os.hasCapability(StreamCapability.HFLUSH.getValue()));
      assertTrue("DFSOutputStream should support hsync()!",
          os.hasCapability(StreamCapability.HSYNC.getValue()));
      byte[] bytes = new byte[1024];
      InputStream is = new ByteArrayInputStream(bytes);
      IOUtils.copyBytes(is, os, bytes.length);
      os.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      is = new ByteArrayInputStream(bytes);
      IOUtils.copyBytes(is, os, bytes.length);
      os.hsync();
      os.close();
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for stream flush with restart injection after hsync.
   */
  private void testStreamFlushWithRestartAfterHsync(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    cluster.waitActive();
    try {
      FileSystem fs = cluster.getFileSystem();
      FSDataOutputStream os = fs.create(new Path("/normal-file"));
      // Verify output stream supports hsync() and hflush().
      assertTrue("DFSOutputStream should support hflush()!",
          os.hasCapability(StreamCapability.HFLUSH.getValue()));
      assertTrue("DFSOutputStream should support hsync()!",
          os.hasCapability(StreamCapability.HSYNC.getValue()));
      byte[] bytes = new byte[1024];
      InputStream is = new ByteArrayInputStream(bytes);
      IOUtils.copyBytes(is, os, bytes.length);
      os.hflush();
      is = new ByteArrayInputStream(bytes);
      IOUtils.copyBytes(is, os, bytes.length);
      os.hsync();

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      os.close();
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testStreamFlush with restart injection after hflush
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_NN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_NN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_SingleDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_SingleDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_AllDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_AllDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_NNDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHflush_NNDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHflush(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testStreamFlush with restart injection after hsync
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_NN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_NN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_SingleDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_SingleDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_AllDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_AllDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_NNDN_Graceful() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testStreamFlush_AfterHsync_NNDN_Crash() throws Exception {
    testStreamFlushWithRestartAfterHsync(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

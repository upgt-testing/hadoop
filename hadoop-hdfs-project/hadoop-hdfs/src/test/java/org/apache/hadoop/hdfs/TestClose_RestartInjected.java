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

import java.io.OutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injected version of TestClose.
 *
 * Tests that file close operations are properly persisted across restarts.
 * The original TestClose tests write-after-close behavior and double close.
 * This restart-injected version tests that:
 * 1. Closed files persist correctly across restarts
 * 2. File content is preserved after restart
 *
 * Original tests transformed:
 * - testWriteAfterClose: Write, close, restart, verify file persisted
 *
 * Restart points:
 * - AfterClose: After file is written and closed
 *
 * Generated variants:
 * - AfterClose x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestClose_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestClose_RestartInjected.class);

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).build();
    cluster.waitActive();
    fs = FileSystem.get(conf);
  }

  @After
  public void teardown() throws Exception {
    if (fs != null) {
      fs.close();
    }
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ============================================================
  // Test: testWriteAfterClose with restart injection
  // AfterClose x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Write data, close file, restart, verify file persisted.
   * Based on TestClose#testWriteAfterClose
   */
  private void testWriteAfterCloseWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    final byte[] data = "foo".getBytes();
    Path testPath = new Path("/test");

    // === ORIGINAL CODE: Write and close file ===
    OutputStream out = fs.create(testPath);
    out.write(data);
    out.close();

    // Verify file was created with correct data
    assertTrue("File should exist after close", fs.exists(testPath));
    assertEquals("File should have correct length", data.length,
        fs.getFileStatus(testPath).getLen());

    // === RESTART INJECTION POINT: After close ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = FileSystem.get(conf);
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file persisted after restart ===
    assertTrue("File should exist after restart", fs.exists(testPath));
    assertEquals("File length should be preserved after restart", data.length,
        fs.getFileStatus(testPath).getLen());

    // Verify file content
    byte[] readData = new byte[data.length];
    fs.open(testPath).read(readData);
    for (int i = 0; i < data.length; i++) {
      assertEquals("Data should match at byte " + i, data[i], readData[i]);
    }
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_NN_Graceful() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_NN_Crash() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_SingleDN_Graceful() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_SingleDN_Crash() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_AllDN_Graceful() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_AllDN_Crash() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_NNDN_Graceful() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWriteAfterClose_AfterClose_NNDN_Crash() throws Exception {
    testWriteAfterCloseWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

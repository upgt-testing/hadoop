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
 * Restart-injected version of TestDFSMkdirs.
 *
 * Tests that directories created with mkdirs persist correctly across restarts.
 *
 * Original tests transformed:
 * - testDFSMkdirs: Create directory with mkdirs, create file in it, restart,
 *   verify both directory and file persist
 *
 * Restart points:
 * - AfterMkdirs: After creating directories
 * - AfterFileInDir: After creating file in the directory
 *
 * Generated variants:
 * - 2 tests x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestDFSMkdirs_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSMkdirs_RestartInjected.class);

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
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
  // Test: Directory creation with mkdirs persists after restart
  // AfterMkdirs x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create directory with mkdirs, restart, verify it persists.
   * Based on TestDFSMkdirs#testDFSMkdirs
   */
  private void testMkdirsWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create directory with mkdirs ===
    Path myPath = new Path("/test/mkdirs");
    assertTrue("mkdirs should succeed", fs.mkdirs(myPath));
    assertTrue("Directory should exist", fs.exists(myPath));
    assertTrue("Second mkdirs on same path should succeed", fs.mkdirs(myPath));

    // === RESTART INJECTION POINT: After mkdirs ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify directory persisted ===
    assertTrue("Directory should exist after restart", fs.exists(myPath));
    assertTrue("Directory should still be a directory",
        fs.getFileStatus(myPath).isDirectory());

    // Verify we can still create subdirectories after restart
    Path subDir = new Path("/test/mkdirs/subdir");
    assertTrue("Should be able to create subdirectory after restart",
        fs.mkdirs(subDir));
    assertTrue("Subdirectory should exist", fs.exists(subDir));
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_NN_Graceful() throws Exception {
    testMkdirsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_NN_Crash() throws Exception {
    testMkdirsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_SingleDN_Graceful() throws Exception {
    testMkdirsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_SingleDN_Crash() throws Exception {
    testMkdirsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_AllDN_Graceful() throws Exception {
    testMkdirsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_AllDN_Crash() throws Exception {
    testMkdirsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_NNDN_Graceful() throws Exception {
    testMkdirsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterMkdirs_NNDN_Crash() throws Exception {
    testMkdirsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: Directory and file in it persist after restart
  // AfterFileInDir x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create directory, create file in it, restart, verify both persist.
   * Based on TestDFSMkdirs#testDFSMkdirs
   */
  private void testFileInMkdirsWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create directory and file in it ===
    Path myPath = new Path("/test/mkdirs");
    assertTrue("mkdirs should succeed", fs.mkdirs(myPath));
    assertTrue("Directory should exist", fs.exists(myPath));

    // Create a file in that directory
    Path myFile = new Path("/test/mkdirs/myFile");
    DFSTestUtil.writeFile(fs, myFile, "hello world");
    assertTrue("File should exist", fs.exists(myFile));

    // === RESTART INJECTION POINT: After creating file in directory ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify directory and file persisted ===
    assertTrue("Directory should exist after restart", fs.exists(myPath));
    assertTrue("File should exist after restart", fs.exists(myFile));
    assertEquals("File content length should match",
        "hello world".length(), fs.getFileStatus(myFile).getLen());

    // Verify file content
    byte[] content = new byte["hello world".length()];
    fs.open(myFile).read(content);
    assertEquals("File content should match", "hello world", new String(content));
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_NN_Graceful() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_NN_Crash() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_SingleDN_Graceful() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_SingleDN_Crash() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_AllDN_Graceful() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_AllDN_Crash() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_NNDN_Graceful() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMkdirs_AfterFileInDir_NNDN_Crash() throws Exception {
    testFileInMkdirsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

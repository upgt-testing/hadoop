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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.DataOutputStream;
import java.io.IOException;

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
 * Restart-injected version of TestDFSRename.
 *
 * Tests that file rename operations persist correctly across restarts.
 *
 * Original tests transformed:
 * - testRename: Create file, rename it, restart, verify rename persisted
 *
 * Restart points:
 * - AfterCreate: After creating file (before rename)
 * - AfterRename: After renaming file
 *
 * Generated variants:
 * - 2 tests x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestDFSRename_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSRename_RestartInjected.class);

  private static final Path DIR = new Path("/test/rename/");

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
  }

  @After
  public void teardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  private static void createFile(FileSystem fs, Path f) throws IOException {
    DataOutputStream out = fs.create(f);
    out.writeBytes("something");
    out.close();
  }

  // ============================================================
  // Test: File created persists after restart (before rename)
  // AfterCreate x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file, restart, verify file persists, then rename.
   * Based on TestDFSRename#testRename
   */
  private void testFileCreatedWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create directory and file ===
    assertTrue("Directory should be created", fs.mkdirs(DIR));

    Path src = new Path(DIR, "src");
    createFile(fs, src);
    assertTrue("Source file should exist", fs.exists(src));

    // === RESTART INJECTION POINT: After creating file ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file persisted and can be renamed ===
    assertTrue("Source file should exist after restart", fs.exists(src));

    // Rename should work after restart
    Path dst = new Path(DIR, "dst");
    assertTrue("Rename should succeed", fs.rename(src, dst));
    assertFalse("Source should not exist after rename", fs.exists(src));
    assertTrue("Destination should exist after rename", fs.exists(dst));

    // Cleanup
    fs.delete(DIR, true);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_NN_Graceful() throws Exception {
    testFileCreatedWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_NN_Crash() throws Exception {
    testFileCreatedWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_SingleDN_Graceful() throws Exception {
    testFileCreatedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_SingleDN_Crash() throws Exception {
    testFileCreatedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_AllDN_Graceful() throws Exception {
    testFileCreatedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_AllDN_Crash() throws Exception {
    testFileCreatedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_NNDN_Graceful() throws Exception {
    testFileCreatedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterCreate_NNDN_Crash() throws Exception {
    testFileCreatedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: Renamed file persists after restart
  // AfterRename x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file, rename it, restart, verify rename persisted.
   * Based on TestDFSRename#testRename
   */
  private void testFileRenamedWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create and rename file ===
    assertTrue("Directory should be created", fs.mkdirs(DIR));

    Path src = new Path(DIR, "src");
    Path dst = new Path(DIR, "dst");

    createFile(fs, src);
    assertTrue("Source file should exist", fs.exists(src));

    // Rename file
    assertTrue("Rename should succeed", fs.rename(src, dst));
    assertFalse("Source should not exist after rename", fs.exists(src));
    assertTrue("Destination should exist after rename", fs.exists(dst));
    assertEquals("Destination should have correct length",
        "something".length(), fs.getFileStatus(dst).getLen());

    // === RESTART INJECTION POINT: After renaming file ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify rename persisted ===
    assertFalse("Source should remain non-existent after restart", fs.exists(src));
    assertTrue("Destination should exist after restart", fs.exists(dst));
    assertEquals("Destination length should be preserved",
        "something".length(), fs.getFileStatus(dst).getLen());

    // Verify file content
    byte[] content = new byte["something".length()];
    fs.open(dst).read(content);
    assertEquals("File content should match", "something", new String(content));

    // Cleanup
    fs.delete(DIR, true);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_NN_Graceful() throws Exception {
    testFileRenamedWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_NN_Crash() throws Exception {
    testFileRenamedWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_SingleDN_Graceful() throws Exception {
    testFileRenamedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_SingleDN_Crash() throws Exception {
    testFileRenamedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_AllDN_Graceful() throws Exception {
    testFileRenamedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_AllDN_Crash() throws Exception {
    testFileRenamedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_NNDN_Graceful() throws Exception {
    testFileRenamedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRename_AfterRename_NNDN_Crash() throws Exception {
    testFileRenamedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

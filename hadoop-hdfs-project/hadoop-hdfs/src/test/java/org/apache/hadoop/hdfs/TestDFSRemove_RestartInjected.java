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
 * Restart-injected version of TestDFSRemove.
 *
 * Tests that file removal operations persist correctly across restarts.
 *
 * Original tests transformed:
 * - testRemove: Create files, delete them, restart, verify deletion persisted
 *
 * Restart points:
 * - AfterCreate: After creating files (before delete)
 * - AfterDelete: After deleting files
 *
 * Generated variants:
 * - 2 tests x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestDFSRemove_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSRemove_RestartInjected.class);

  private static final Path DIR = new Path("/test/remove/");
  private static final int FILE_COUNT = 10;

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
  // Test: Files persist after restart (before delete)
  // AfterCreate x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create files, restart, verify files persist.
   * Based on TestDFSRemove#testRemove
   */
  private void testFilesCreatedWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create directory and files ===
    assertTrue("Directory should be created", fs.mkdirs(DIR));

    // Create files
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      createFile(fs, a);
    }

    // Verify all files exist
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      assertTrue("File " + a + " should exist", fs.exists(a));
    }

    // === RESTART INJECTION POINT: After creating files ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify files persisted ===
    assertTrue("Directory should exist after restart", fs.exists(DIR));
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      assertTrue("File " + a + " should exist after restart", fs.exists(a));
    }

    // Cleanup
    fs.delete(DIR, true);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_NN_Graceful() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_NN_Crash() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_SingleDN_Graceful() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_SingleDN_Crash() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_AllDN_Graceful() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_AllDN_Crash() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_NNDN_Graceful() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterCreate_NNDN_Crash() throws Exception {
    testFilesCreatedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: Deleted files stay deleted after restart
  // AfterDelete x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create files, delete them, restart, verify deleted files stay deleted.
   * Based on TestDFSRemove#testRemove
   */
  private void testFilesDeletedWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create and delete files ===
    assertTrue("Directory should be created", fs.mkdirs(DIR));

    // Create files
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      createFile(fs, a);
    }

    // Delete files
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      assertTrue("File " + a + " should be deleted", fs.delete(a, false));
    }

    // Verify all files are deleted
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      assertFalse("File " + a + " should not exist after delete", fs.exists(a));
    }

    // Directory should still exist
    assertTrue("Directory should still exist", fs.exists(DIR));
    assertEquals("Directory should be empty", 0, fs.listStatus(DIR).length);

    // === RESTART INJECTION POINT: After deleting files ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    fs = cluster.getFileSystem();
    verifyClusterHealth(cluster, fs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify deleted files stay deleted ===
    assertTrue("Directory should exist after restart", fs.exists(DIR));
    for (int i = 0; i < FILE_COUNT; i++) {
      Path a = new Path(DIR, "a" + i);
      assertFalse("File " + a + " should remain deleted after restart", fs.exists(a));
    }
    assertEquals("Directory should still be empty after restart",
        0, fs.listStatus(DIR).length);

    // Cleanup
    fs.delete(DIR, true);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_NN_Graceful() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_NN_Crash() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_SingleDN_Graceful() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_SingleDN_Crash() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_AllDN_Graceful() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_AllDN_Crash() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_NNDN_Graceful() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testRemove_AfterDelete_NNDN_Crash() throws Exception {
    testFilesDeletedWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

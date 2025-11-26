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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileCreationDelete.
 * Tests file creation and delete operations survive component restarts.
 *
 * Original test: testFileCreationDeleteParent (has hflush before delete and restart)
 *
 * Note: Original test already has restart logic. This variant adds restart
 * injection at the hflush point before delete to test different restart scenarios.
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileCreationDelete_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileCreationDelete_RestartInjected.class);

  /**
   * Core test logic for testFileCreationDeleteParent with restart injection.
   */
  private void testFileCreationDeleteParentWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    FileSystem fs = null;
    try {
      cluster.waitActive();
      fs = cluster.getFileSystem();

      // create file1
      Path dir = new Path("/foo_restart");
      Path file1 = new Path(dir, "file1");
      FSDataOutputStream stm1 = TestFileCreation.createFile(fs, file1, 1);
      TestFileCreation.writeFile(stm1, 1000);
      stm1.hflush();

      // create file2
      Path file2 = new Path("/file2_restart");
      FSDataOutputStream stm2 = TestFileCreation.createFile(fs, file2, 1);
      TestFileCreation.writeFile(stm2, 1000);
      stm2.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fs);
      LOG.info("=== RESTART COMPLETE ===");

      // rm dir (file1's parent)
      fs.delete(dir, true);

      // Close streams
      stm1.close();
      stm2.close();

      // Verify file2 still exists
      assertTrue("file2 should exist after restart and delete", fs.exists(file2));
    } finally {
      if (fs != null) {
        fs.close();
      }
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testFileCreationDeleteParent
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_NN_Graceful() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_NN_Crash() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_SingleDN_Graceful() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_SingleDN_Crash() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_AllDN_Graceful() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_AllDN_Crash() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_NNDN_Graceful() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFileCreationDeleteParent_AfterHflush_NNDN_Crash() throws Exception {
    testFileCreationDeleteParentWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

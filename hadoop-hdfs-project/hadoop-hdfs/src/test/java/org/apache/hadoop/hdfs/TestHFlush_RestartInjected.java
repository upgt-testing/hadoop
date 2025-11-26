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

import java.util.EnumSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestHFlush.
 * Tests hflush/hsync operations survive component restarts.
 *
 * Original tests:
 * - hFlush_01, hFlush_02, hFlush_03 (basic hflush tests)
 * - hSyncUpdateLength_00 (hsync with update length)
 * - testPipelineHeartbeat (slow writer with hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 24 variants (3 tests x 8 variants)
 */
public class TestHFlush_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestHFlush_RestartInjected.class);
  private static final String fName = "hflushtest_restart.dat";

  /**
   * Core test logic for hflush tests with restart injection.
   * Uses doTheJob helper method pattern from original test.
   */
  private void doTheJobWithRestart(Configuration conf, final String fileName,
      long block_size, short replicas, boolean isSync,
      EnumSet<SyncFlag> syncFlags, RestartTarget target, RestartMode mode) throws Exception {

    byte[] fileContent;
    final int SECTIONS = 10;
    fileContent = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(replicas).build();
    DistributedFileSystem fileSystem = cluster.getFileSystem();

    try {
      Path path = new Path(fileName);
      FSDataOutputStream stm = fileSystem.create(path, false, 4096, replicas, block_size);

      int tenth = AppendTestUtil.FILE_SIZE / SECTIONS;
      int rounding = AppendTestUtil.FILE_SIZE - tenth * SECTIONS;

      for (int i = 0; i < SECTIONS; i++) {
        // write to the file
        stm.write(fileContent, tenth * i, tenth);

        // Wait while hflush/hsync pushes all packets through built pipeline
        if (isSync) {
          ((DFSOutputStream) stm.getWrappedStream()).hsync(syncFlags);
        } else {
          ((DFSOutputStream) stm.getWrappedStream()).hflush();
        }

        // Inject restart at halfway point
        if (i == SECTIONS / 2) {
          // === RESTART INJECTION POINT: After hflush/hsync ===
          LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
          executeRestart(cluster, target, mode, true);
          verifyClusterHealth(cluster, fileSystem);
          LOG.info("=== RESTART COMPLETE ===");
        }

        byte[] toRead = new byte[tenth];
        byte[] expected = new byte[tenth];
        System.arraycopy(fileContent, tenth * i, expected, 0, tenth);
        // Open the same file for read
        FSDataInputStream is = fileSystem.open(path);
        is.seek(tenth * i);
        int readBytes = is.read(toRead, 0, tenth);
        assertTrue("Should've get more bytes", (readBytes > 0) && (readBytes <= tenth));
        is.close();
        TestHFlush.checkData(toRead, 0, readBytes, expected, "Partial verification");
      }

      stm.write(fileContent, tenth * SECTIONS, rounding);
      stm.close();

      assertEquals("File size doesn't match", AppendTestUtil.FILE_SIZE,
          fileSystem.getFileStatus(path).getLen());
      AppendTestUtil.checkFullFile(fileSystem, path, fileContent.length, fileContent, "hflush()");
    } finally {
      fileSystem.close();
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for hSyncUpdateLength_00 with restart injection.
   */
  private void testHSyncUpdateLength00WithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    DistributedFileSystem fileSystem = cluster.getFileSystem();

    try {
      Path path = new Path(fName);
      FSDataOutputStream stm = fileSystem.create(path, true, 4096, (short) 2,
          AppendTestUtil.BLOCK_SIZE);

      // Write some data
      stm.write(new byte[1024]);
      ((DFSOutputStream) stm.getWrappedStream()).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fileSystem);
      LOG.info("=== RESTART COMPLETE ===");

      long currentFileLength = fileSystem.getFileStatus(path).getLen();
      assertEquals(1024L, currentFileLength);
      stm.close();
    } finally {
      fileSystem.close();
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testPipelineHeartbeat with restart injection.
   */
  private void testPipelineHeartbeatWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    final int DATANODE_NUM = 2;
    final int fileLen = 6;
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    final int timeout = 2000;
    conf.setInt("dfs.client.socket-timeout", timeout);

    final Path p = new Path("/pipelineHeartbeat_restart/foo");

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(DATANODE_NUM).build();
    try {
      DistributedFileSystem fs = cluster.getFileSystem();
      byte[] fileContents = AppendTestUtil.initBuffer(fileLen);

      FSDataOutputStream stm = AppendTestUtil.createFile(fs, p, DATANODE_NUM);

      stm.write(fileContents, 0, 1);
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data
      stm.write(fileContents, 1, 1);
      stm.hflush();

      stm.write(fileContents, 2, 1);
      stm.hflush();

      stm.write(fileContents, 3, 3);
      stm.close();

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, p, fileLen, fileContents, "Failed to write file with restart");
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: hFlush (basic test)
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_NN_Graceful() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_NN_Crash() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_SingleDN_Graceful() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_SingleDN_Crash() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_AllDN_Graceful() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_AllDN_Crash() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_NNDN_Graceful() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHFlush_AfterHflush_NNDN_Crash() throws Exception {
    doTheJobWithRestart(new HdfsConfiguration(), fName, AppendTestUtil.BLOCK_SIZE,
        (short) 2, false, EnumSet.noneOf(SyncFlag.class),
        RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: hSyncUpdateLength_00
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_NN_Graceful() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_NN_Crash() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_SingleDN_Graceful() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_SingleDN_Crash() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_AllDN_Graceful() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_AllDN_Crash() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_NNDN_Graceful() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testHSyncUpdateLength_AfterHsync_NNDN_Crash() throws Exception {
    testHSyncUpdateLength00WithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testPipelineHeartbeat
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_NN_Graceful() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_NN_Crash() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_SingleDN_Graceful() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_SingleDN_Crash() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_AllDN_Graceful() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_AllDN_Crash() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_NNDN_Graceful() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipelineHeartbeat_AfterHflush_NNDN_Crash() throws Exception {
    testPipelineHeartbeatWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

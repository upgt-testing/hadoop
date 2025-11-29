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
package org.apache.hadoop.hdfs.server.namenode;

import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFSImage.
 * Tests FSImage persistence operations survive component restarts.
 *
 * Original test with hsync during file creation before saveNamespace
 *
 * Generated variants:
 * - AfterHsync x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFSImage_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFSImage_RestartInjected.class);

  /**
   * Core test logic for FSImage persist with restart injection.
   */
  private void testPersistWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      final Path dir = new Path("/abc/def");
      final Path file1 = new Path(dir, "f1");
      final Path file2 = new Path(dir, "f2");

      // create an empty file f1
      fs.create(file1).close();

      // create an under-construction file f2
      FSDataOutputStream out = fs.create(file2);
      out.writeBytes("hello");
      ((DFSOutputStream) out.getWrappedStream()).hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));

      // === RESTART INJECTION POINT: After hsync ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Write more data and close
      out.writeBytes(" world");
      out.close();

      // Verify files exist
      assertTrue("Directory should exist", fs.isDirectory(dir));
      assertTrue("File1 should exist", fs.exists(file1));
      assertTrue("File2 should exist", fs.exists(file2));

      // Verify file2 contents
      long fileLen = fs.getFileStatus(file2).getLen();
      assertTrue("File2 should have all data", fileLen == "hello world".length());
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testPersist
  // AfterHsync x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_NN_Graceful() throws Exception {
    testPersistWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_NN_Crash() throws Exception {
    testPersistWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_SingleDN_Graceful() throws Exception {
    testPersistWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_SingleDN_Crash() throws Exception {
    testPersistWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_AllDN_Graceful() throws Exception {
    testPersistWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_AllDN_Crash() throws Exception {
    testPersistWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_NNDN_Graceful() throws Exception {
    testPersistWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPersist_AfterHsync_NNDN_Crash() throws Exception {
    testPersistWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

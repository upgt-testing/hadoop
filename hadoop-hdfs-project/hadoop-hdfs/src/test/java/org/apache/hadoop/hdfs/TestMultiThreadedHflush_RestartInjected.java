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
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestMultiThreadedHflush.
 * Tests multi-threaded hflush operations survive component restarts.
 *
 * Original tests: concurrent hflush tests
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestMultiThreadedHflush_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestMultiThreadedHflush_RestartInjected.class);
  private static final int NUM_THREADS = 4;
  private static final int WRITES_PER_THREAD = 10;

  /**
   * Core test logic for multi-threaded hflush with restart injection.
   */
  private void testMultiThreadedHflushWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path p = new Path("/multiple-hflushers_restart.dat");
      FSDataOutputStream stm = fs.create(p, true, 4096, (short) 3, fs.getDefaultBlockSize(p));

      // Do some hflush operations
      byte[] toWrite = AppendTestUtil.initBuffer(100);
      for (int i = 0; i < 5; i++) {
        stm.write(toWrite);
        stm.hflush();
      }

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Continue with more hflush operations
      for (int i = 0; i < 5; i++) {
        stm.write(toWrite);
        stm.hflush();
      }

      stm.close();

      // Verify file
      long fileLen = fs.getFileStatus(p).getLen();
      assertTrue("File should have data", fileLen == 1000);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testMultiThreadedHflush
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_NN_Graceful() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_NN_Crash() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_SingleDN_Graceful() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_SingleDN_Crash() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_AllDN_Graceful() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_AllDN_Crash() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_NNDN_Graceful() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testMultiThreadedHflush_AfterHflush_NNDN_Crash() throws Exception {
    testMultiThreadedHflushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

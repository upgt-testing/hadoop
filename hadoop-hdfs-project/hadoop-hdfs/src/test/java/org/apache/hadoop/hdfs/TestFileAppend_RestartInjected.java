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
 * Restart-injection variant of TestFileAppend.
 * Tests file append operations survive component restarts.
 *
 * Original tests with hflush:
 * - testSimpleFlush
 * - testComplexFlush
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestFileAppend_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileAppend_RestartInjected.class);
  private static final int FILE_SIZE = 8192;

  private byte[] initBuffer(int size) {
    byte[] buf = new byte[size];
    for (int i = 0; i < size; i++) {
      buf[i] = (byte) (i % 127);
    }
    return buf;
  }

  /**
   * Core test logic for testSimpleFlush with restart injection.
   */
  private void testSimpleFlushWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    byte[] fileContents = initBuffer(FILE_SIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // create a new file
      Path file1 = new Path("/simpleFlush.dat");
      FSDataOutputStream stm = fs.create(file1, true, 4096, (short) 1, fs.getDefaultBlockSize(file1));

      // write to file
      int mid = FILE_SIZE / 2;
      stm.write(fileContents, 0, mid);
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // write the remainder of the file
      stm.write(fileContents, mid, FILE_SIZE - mid);
      stm.hflush();
      stm.close();

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, file1, FILE_SIZE, fileContents, "Read");
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testComplexFlush with restart injection.
   */
  private void testComplexFlushWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    byte[] fileContents = initBuffer(FILE_SIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // create a new file
      Path file1 = new Path("/complexFlush.dat");
      FSDataOutputStream stm = fs.create(file1, true, 4096, (short) 1, fs.getDefaultBlockSize(file1));

      int start = 0;
      for (start = 0; (start + 29) < FILE_SIZE; ) {
        stm.write(fileContents, start, 29);
        stm.hflush();
        start += 29;

        // Inject restart after first few flushes
        if (start == 29 * 3) {
          // === RESTART INJECTION POINT: After hflush ===
          LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
          executeRestart(cluster, target, mode, true);
          verifyClusterHealth(cluster, fs);
          LOG.info("=== RESTART COMPLETE ===");
        }
      }
      stm.write(fileContents, start, FILE_SIZE - start);
      stm.flush();
      stm.close();

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, file1, FILE_SIZE, fileContents, "Read");
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testSimpleFlush
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_NN_Graceful() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_NN_Crash() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_SingleDN_Graceful() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_SingleDN_Crash() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_AllDN_Graceful() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_AllDN_Crash() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_NNDN_Graceful() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testSimpleFlush_AfterHflush_NNDN_Crash() throws Exception {
    testSimpleFlushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testComplexFlush
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_NN_Graceful() throws Exception {
    testComplexFlushWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_NN_Crash() throws Exception {
    testComplexFlushWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_SingleDN_Graceful() throws Exception {
    testComplexFlushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_SingleDN_Crash() throws Exception {
    testComplexFlushWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_AllDN_Graceful() throws Exception {
    testComplexFlushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_AllDN_Crash() throws Exception {
    testComplexFlushWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_NNDN_Graceful() throws Exception {
    testComplexFlushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testComplexFlush_AfterHflush_NNDN_Crash() throws Exception {
    testComplexFlushWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

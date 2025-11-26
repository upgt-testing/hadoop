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
import org.apache.hadoop.fs.FSDataInputStream;
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
 * Restart-injection variant of TestReadWhileWriting.
 * Tests read while writing operations survive component restarts.
 *
 * Original test: pipeline_02_03 (read while writing with hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestReadWhileWriting_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestReadWhileWriting_RestartInjected.class);
  private static final int WRITE_SIZE = 1024;

  /**
   * Core test logic for read while writing with restart injection.
   */
  private void testReadWhileWritingWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path p = new Path("/readWhileWriting_restart.dat");
      FSDataOutputStream out = fs.create(p, true, 4096, (short) 3, fs.getDefaultBlockSize(p));

      // Write some data and hflush
      byte[] buffer = AppendTestUtil.initBuffer(WRITE_SIZE);
      out.write(buffer);
      ((DFSOutputStream) out.getWrappedStream()).hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Read while still writing
      FSDataInputStream in = fs.open(p);
      byte[] readBuffer = new byte[WRITE_SIZE];
      int readLen = in.read(readBuffer, 0, WRITE_SIZE);
      assertTrue("Should read data that was hflushed", readLen > 0);
      in.close();

      // Write more and close
      out.write(buffer);
      out.close();

      // Verify file
      long fileLen = fs.getFileStatus(p).getLen();
      assertTrue("File should have all data", fileLen == WRITE_SIZE * 2);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testReadWhileWriting
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_NN_Graceful() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_NN_Crash() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_SingleDN_Graceful() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_SingleDN_Crash() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_AllDN_Graceful() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_AllDN_Crash() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_NNDN_Graceful() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadWhileWriting_AfterHflush_NNDN_Crash() throws Exception {
    testReadWhileWritingWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

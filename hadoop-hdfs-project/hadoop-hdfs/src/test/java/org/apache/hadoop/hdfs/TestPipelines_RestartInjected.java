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
 * Restart-injection variant of TestPipelines.
 * Tests pipeline operations survive component restarts.
 *
 * Original test: pipeline_01 (append/write/hflush sequence)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestPipelines_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestPipelines_RestartInjected.class);
  private static final short REPL_FACTOR = 3;
  private static final int FILE_SIZE = 512;

  /**
   * Core test logic for pipeline_01 with restart injection.
   */
  private void testPipelineWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(REPL_FACTOR).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      Path filePath = new Path("/pipeline_restart.dat");
      DFSTestUtil.createFile(fs, filePath, FILE_SIZE, REPL_FACTOR, System.currentTimeMillis());

      FSDataOutputStream ofs = fs.append(filePath);
      ofs.writeBytes("Some more stuff to write");
      ((DFSOutputStream) ofs.getWrappedStream()).hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      ofs.close();

      // Verify file is readable
      byte[] content = DFSTestUtil.readFileBuffer(fs, filePath);
      LOG.info("File size after close: {}", content.length);
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: pipeline_01
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_NN_Graceful() throws Exception {
    testPipelineWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_NN_Crash() throws Exception {
    testPipelineWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_SingleDN_Graceful() throws Exception {
    testPipelineWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_SingleDN_Crash() throws Exception {
    testPipelineWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_AllDN_Graceful() throws Exception {
    testPipelineWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_AllDN_Crash() throws Exception {
    testPipelineWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_NNDN_Graceful() throws Exception {
    testPipelineWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPipeline_AfterHflush_NNDN_Crash() throws Exception {
    testPipelineWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

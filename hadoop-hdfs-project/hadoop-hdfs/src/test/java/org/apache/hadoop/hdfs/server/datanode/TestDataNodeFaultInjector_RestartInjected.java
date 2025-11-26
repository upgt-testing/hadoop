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
package org.apache.hadoop.hdfs.server.datanode;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.PathUtils;
import org.junit.Test;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDataNodeFaultInjector.
 * Tests DataNode fault injection survives component restarts.
 *
 * Original tests: testDelaySendingAckToUpstream, testDelaySendingPacketDownstream
 * Both use verifyFaultInjectionDelayPipeline which has hflush and hsync.
 *
 * Generated variants per test:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestDataNodeFaultInjector_RestartInjected {
  private static final Logger LOG = LoggerFactory
      .getLogger(TestDataNodeFaultInjector_RestartInjected.class);

  private static class MetricsDataNodeFaultInjector
      extends DataNodeFaultInjector {

    public static final long DELAY = 2000;
    private long delayMs = 0;
    private final String err = "Interrupted while sleeping. Bailing out.";
    private long delayTries = 1;

    void delayOnce() throws IOException {
      if (delayTries > 0) {
        delayTries--;
        try {
          Thread.sleep(DELAY);
        } catch (InterruptedException ie) {
          throw new IOException(err);
        }
      }
    }

    long getDelayMs() {
      return delayMs;
    }

    void logDelay(final long duration) {
      if (duration >= DELAY) {
        this.delayMs = duration;
      }
    }
  }

  /**
   * Core test logic for delaySendingAckToUpstream with restart injection.
   */
  private void testDelaySendingAckToUpstreamWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    final MetricsDataNodeFaultInjector mdnFaultInjector =
        new MetricsDataNodeFaultInjector() {
          @Override
          public void delaySendingAckToUpstream(final String upstreamAddr)
              throws IOException {
            delayOnce();
          }

          @Override
          public void logDelaySendingAckToUpstream(final String upstreamAddr,
              final long delayMs) throws IOException {
            logDelay(delayMs);
          }
        };
    verifyFaultInjectionDelayPipelineWithRestart(mdnFaultInjector, target, mode);
  }

  /**
   * Core test logic for delaySendingPacketDownstream with restart injection.
   */
  private void testDelaySendingPacketDownstreamWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    final MetricsDataNodeFaultInjector mdnFaultInjector =
        new MetricsDataNodeFaultInjector() {
          @Override
          public void stopSendingPacketDownstream(final String mirrAddr)
              throws IOException {
            delayOnce();
          }

          @Override
          public void logDelaySendingPacketDownstream(final String mirrAddr,
              final long delayMs) throws IOException {
            logDelay(delayMs);
          }
        };
    verifyFaultInjectionDelayPipelineWithRestart(mdnFaultInjector, target, mode);
  }

  private void verifyFaultInjectionDelayPipelineWithRestart(
      final MetricsDataNodeFaultInjector mdnFaultInjector,
      RestartTarget target, RestartMode mode) throws Exception {

    final Path baseDir = new Path(
        PathUtils.getTestDir(getClass()).getPath(),
        GenericTestUtils.getMethodName());
    final DataNodeFaultInjector oldDnInjector = DataNodeFaultInjector.get();
    DataNodeFaultInjector.set(mdnFaultInjector);

    final Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    final long datanodeSlowLogThresholdMs = MetricsDataNodeFaultInjector.DELAY / 2;
    conf.setLong(DFSConfigKeys.DFS_DATANODE_SLOW_IO_WARNING_THRESHOLD_KEY,
        datanodeSlowLogThresholdMs);
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.toString());

    conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_SOCKET_TIMEOUT_KEY,
        MetricsDataNodeFaultInjector.DELAY * 2);
    conf.setBoolean(
        HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.ENABLE_KEY,
        true);
    conf.set(
        HdfsClientConfigKeys.BlockWrite.ReplaceDatanodeOnFailure.POLICY_KEY,
        "ALWAYS");

    MiniDFSCluster cluster = null;
    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
      cluster.waitActive();

      final FileSystem fs = cluster.getFileSystem();
      try (FSDataOutputStream out = fs
          .create(new Path(baseDir, "test.data"), (short) 2)) {
        out.write(0x31);
        out.hflush();

        // === RESTART INJECTION POINT: After hflush ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, (DistributedFileSystem) fs);
        LOG.info("=== RESTART COMPLETE ===");

        out.hsync();
      }
      LOG.info("delay info: " + mdnFaultInjector.getDelayMs() + ":"
          + datanodeSlowLogThresholdMs);
      assertTrue("Injected delay should be longer than the configured one",
          mdnFaultInjector.getDelayMs() > datanodeSlowLogThresholdMs);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
      DataNodeFaultInjector.set(oldDnInjector);
    }
  }

  // ============================================================
  // Test variants: testDelaySendingAckToUpstream
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_NN_Graceful() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_NN_Crash() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_SingleDN_Graceful() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_SingleDN_Crash() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_AllDN_Graceful() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_AllDN_Crash() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_NNDN_Graceful() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingAckToUpstream_AfterHflush_NNDN_Crash() throws Exception {
    testDelaySendingAckToUpstreamWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testDelaySendingPacketDownstream
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_NN_Graceful() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_NN_Crash() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_SingleDN_Graceful() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_SingleDN_Crash() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_AllDN_Graceful() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_AllDN_Crash() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_NNDN_Graceful() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDelaySendingPacketDownstream_AfterHflush_NNDN_Crash() throws Exception {
    testDelaySendingPacketDownstreamWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

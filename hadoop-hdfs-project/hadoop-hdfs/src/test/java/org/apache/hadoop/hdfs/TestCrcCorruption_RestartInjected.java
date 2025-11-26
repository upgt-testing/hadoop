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
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestCrcCorruption.
 * Tests CRC corruption handling survives component restarts.
 *
 * Original test: testCorruptionDuringWrt (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestCrcCorruption_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestCrcCorruption_RestartInjected.class);

  private DFSClientFaultInjector faultInjector;

  @Before
  public void setUp() {
    faultInjector = Mockito.mock(DFSClientFaultInjector.class);
    DFSClientFaultInjector.set(faultInjector);
  }

  /**
   * Core test logic for corruption during write with restart injection.
   */
  private void testCorruptionDuringWrtWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Set short retry timeouts so this test runs faster
    conf.setInt(HdfsClientConfigKeys.Retry.WINDOW_BASE_KEY, 10);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = null;

    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(10).build();
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();
      Path file = new Path("/test_corruption_file");
      FSDataOutputStream out = fs.create(file, true, 8192, (short) 3, (long) (128 * 1024 * 1024));
      byte[] data = new byte[65536];
      for (int i = 0; i < 65536; i++) {
        data[i] = (byte) (i % 256);
      }

      for (int i = 0; i < 5; i++) {
        out.write(data, 0, 65535);
      }
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // corrupt the packet once
      Mockito.when(faultInjector.corruptPacket()).thenReturn(true, false);
      Mockito.when(faultInjector.uncorruptPacket()).thenReturn(true, false);

      for (int i = 0; i < 5; i++) {
        out.write(data, 0, 65535);
      }
      out.close();
      // read should succeed
      FSDataInputStream in = fs.open(file);
      for (int c; (c = in.read()) != -1; ) ;
      in.close();
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
      Mockito.when(faultInjector.corruptPacket()).thenReturn(false);
      Mockito.when(faultInjector.uncorruptPacket()).thenReturn(false);
    }
  }

  // ============================================================
  // Test variants: testCorruptionDuringWrt with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_NN_Graceful() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_NN_Crash() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_SingleDN_Graceful() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_SingleDN_Crash() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_AllDN_Graceful() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_AllDN_Crash() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_NNDN_Graceful() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testCorruptionDuringWrt_AfterHflush_NNDN_Crash() throws Exception {
    testCorruptionDuringWrtWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

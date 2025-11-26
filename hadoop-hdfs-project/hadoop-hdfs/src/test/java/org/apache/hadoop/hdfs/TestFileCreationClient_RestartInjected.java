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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileCreationClient.
 * Tests client-triggered lease recovery survives component restarts.
 *
 * Original test: testClientTriggeredLeaseRecovery (has hflush in SlowWriter thread)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestFileCreationClient_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileCreationClient_RestartInjected.class);
  private static final int REPLICATION = 3;

  static class SlowWriter extends Thread {
    final FileSystem fs;
    final Path filepath;
    volatile boolean running = true;
    volatile int writeCount = 0;

    SlowWriter(FileSystem fs, Path filepath) {
      super(SlowWriter.class.getSimpleName() + ":" + filepath);
      this.fs = fs;
      this.filepath = filepath;
    }

    @Override
    public void run() {
      FSDataOutputStream out = null;
      int i = 0;
      try {
        out = fs.create(filepath);
        for (; running; i++) {
          out.write(i);
          out.hflush();
          writeCount = i;
          sleep(100);
        }
      } catch (Exception e) {
        // Expected when thread is interrupted
      } finally {
        IOUtils.closeStream(out);
      }
    }
  }

  /**
   * Core test logic for testClientTriggeredLeaseRecovery with restart injection.
   */
  private void testClientTriggeredLeaseRecoveryWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setInt(DFSConfigKeys.DFS_DATANODE_HANDLER_COUNT_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, REPLICATION);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path dir = new Path("/wrwelkj_restart");

      SlowWriter[] slowwriters = new SlowWriter[3]; // Reduced for faster test
      for (int i = 0; i < slowwriters.length; i++) {
        slowwriters[i] = new SlowWriter(fs, new Path(dir, "file" + i));
      }

      try {
        for (SlowWriter sw : slowwriters) {
          sw.start();
        }

        // Wait for writers to do some hflush
        Thread.sleep(500);

        // === RESTART INJECTION POINT: After hflush (in writers) ===
        LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
        executeRestart(cluster, target, mode, true);
        verifyClusterHealth(cluster, fs);
        LOG.info("=== RESTART COMPLETE ===");

        // Let the slow writers continue for a bit
        Thread.sleep(1000);
      } finally {
        for (SlowWriter sw : slowwriters) {
          if (sw != null) {
            sw.running = false;
            sw.interrupt();
          }
        }
        for (SlowWriter sw : slowwriters) {
          if (sw != null) {
            sw.join();
          }
        }
      }

      // Verify the files
      for (SlowWriter sw : slowwriters) {
        FSDataInputStream in = null;
        try {
          in = fs.open(sw.filepath);
          for (int j = 0, x; (x = in.read()) != -1; j++) {
            assertEquals(j, x);
          }
        } finally {
          IOUtils.closeStream(in);
        }
      }
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testClientTriggeredLeaseRecovery
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_NN_Graceful() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_NN_Crash() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_SingleDN_Graceful() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_SingleDN_Crash() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_AllDN_Graceful() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_AllDN_Crash() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_NNDN_Graceful() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testClientTriggeredLeaseRecovery_AfterHflush_NNDN_Crash() throws Exception {
    testClientTriggeredLeaseRecoveryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

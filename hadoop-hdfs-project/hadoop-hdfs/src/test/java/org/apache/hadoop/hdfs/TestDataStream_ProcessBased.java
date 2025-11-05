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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.GenericTestUtils.LogCapturer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDataStream}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDataStream Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDataStream_ProcessBased extends ProcessBasedUpgradeTestBase {

  static int PACKET_SIZE = 1024;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_FIRST_WRITE,
      UpgradeCheckpoints.AFTER_FIRST_FLUSH,
      "AFTER_FIRST_CLOSE",
      "AFTER_REOPEN",
      UpgradeCheckpoints.AFTER_SECOND_WRITE,
      "AFTER_SECOND_FLUSH",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test(timeout = 60000)
  public void testDfsClient() throws Exception {
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_WRITE_PACKET_SIZE_KEY,
        PACKET_SIZE);
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SLOW_IO_WARNING_THRESHOLD_KEY,
        10000);
    conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SOCKET_TIMEOUT_KEY, 60000);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    LogCapturer logs = GenericTestUtils.LogCapturer.captureLogs(LoggerFactory
        .getLogger(DataStreamer.class));
    byte[] toWrite = new byte[PACKET_SIZE];
    new Random(1).nextBytes(toWrite);
    final Path path = new Path("/file1");
    fs = cluster.getFileSystem();
    FSDataOutputStream out = null;
    out = fs.create(path, false);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    out.write(toWrite);
    out.write(toWrite);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.hflush();
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_FLUSH);

    // Close before potential upgrade
    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    // Wait to cross slow IO warning threshold
    Thread.sleep(15 * 1000);

    // Reopen in append mode
    out = fs.append(path);
    checkpoint("AFTER_REOPEN");

    out.write(toWrite);
    out.write(toWrite);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.hflush();
    checkpoint("AFTER_SECOND_FLUSH");

    // Wait for capturing logs in busy cluster
    Thread.sleep(5 * 1000);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    out.close();
    logs.stopCapturing();
    GenericTestUtils.assertDoesNotMatch(logs.getOutput(),
        "Slow ReadProcessor read fields for block");
  }
}

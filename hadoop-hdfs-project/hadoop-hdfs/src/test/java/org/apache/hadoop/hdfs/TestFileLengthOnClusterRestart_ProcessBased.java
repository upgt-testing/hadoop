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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataInputStream;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileLengthOnClusterRestart}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestFileLengthOnClusterRestart Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFileLengthOnClusterRestart_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_FIRST_WRITE",
      "AFTER_HSYNC",
      "AFTER_FIRST_NN_RESTART",
      "AFTER_DN_SHUTDOWN",
      "AFTER_SECOND_NN_RESTART"
    );
  }

  /**
   * Tests the fileLength when we sync the file and restart the cluster and
   * Datanodes not report to Namenode yet.
   */
  @Test(timeout = 60000)
  public void testFileLengthWithHSyncAndClusterRestartWithOutDNsRegister()
      throws Exception {
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 512);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    HdfsDataInputStream in = null;
    try {
      Path path = new Path("/tmp/TestFileLengthOnClusterRestart", "test");
      DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();

      FSDataOutputStream out = dfs.create(path);
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      int fileLength = 1030;
      out.write(new byte[fileLength]);
      checkpoint("AFTER_FIRST_WRITE");

      out.hsync();
      out.close();  // Close before checkpoint/restart
      checkpoint("AFTER_HSYNC");

      // Reopen to get fresh file handle after potential upgrade
      dfs = (DistributedFileSystem) cluster.getFileSystem();

      cluster.restartNameNode(0);
      cluster.waitClusterUp();
      checkpoint("AFTER_FIRST_NN_RESTART");

      // Get fresh filesystem after restart
      dfs = (DistributedFileSystem) cluster.getFileSystem();

      in = (HdfsDataInputStream) dfs.open(path, 1024);
      // Verify the length when we just restart NN. DNs will register
      // immediately.
      Assert.assertEquals(fileLength, in.getVisibleLength());
      in.close();
      in = null;

      // Shutdown all DataNodes
      int numDataNodes = cluster.getNumDataNodes();
      for (int i = 0; i < numDataNodes; i++) {
        cluster.shutdownDataNode(i);
      }
      checkpoint("AFTER_DN_SHUTDOWN");

      cluster.restartNameNode(0, false);
      checkpoint("AFTER_SECOND_NN_RESTART");

      // Get fresh filesystem after restart
      dfs = (DistributedFileSystem) cluster.getFileSystem();

      // This is just for ensuring NN started.
      verifyNNIsInSafeMode(dfs);

      try {
        in = (HdfsDataInputStream) dfs.open(path);
        Assert.fail("Expected IOException");
      } catch (IOException e) {
        Assert.assertTrue(e.getLocalizedMessage().indexOf(
            "Name node is in safe mode") >= 0);
      }
    } finally {
      if (null != in) {
        in.close();
      }
      // Cleanup handled by ProcessBasedUpgradeTestBase @After
    }
  }

  private void verifyNNIsInSafeMode(DistributedFileSystem dfs)
      throws IOException {
    while (true) {
      try {
        if (dfs.isInSafeMode()) {
          return;
        } else {
          throw new IOException("Expected to be in SafeMode");
        }
      } catch (IOException e) {
        // NN might not started completely Ignore
      }
    }
  }
}

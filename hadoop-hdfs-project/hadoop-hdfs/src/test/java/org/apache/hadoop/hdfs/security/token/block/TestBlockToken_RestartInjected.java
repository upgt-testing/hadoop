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

package org.apache.hadoop.hdfs.security.token.block;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.Test;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestBlockToken.
 * Tests block token operations survive component restarts.
 *
 * Original test: testLastLocatedBlockTokenExpiry (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestBlockToken_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestBlockToken_RestartInjected.class);

  @Before
  public void disableKerberos() {
    Configuration conf = new Configuration();
    conf.set(HADOOP_SECURITY_AUTHENTICATION, "simple");
    UserGroupInformation.setConfiguration(conf);
  }

  /**
   * Core test logic for last in-progress block token expiry with restart injection.
   * 1. Write file with one block which is in-progress.
   * 2. hflush the data.
   * 3. Inject restart.
   * 4. Open input stream and close the output stream.
   * 5. Wait for block token expiration and read the data.
   * 6. Read should be success.
   */
  private void testLastLocatedBlockTokenExpiryWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    Configuration conf = new Configuration();
    conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(1).build()) {
      cluster.waitClusterUp();
      final NameNode nn = cluster.getNameNode();
      final BlockManager bm = nn.getNamesystem().getBlockManager();
      final BlockTokenSecretManager sm = bm.getBlockTokenSecretManager();

      // set a short token lifetime (1 second)
      SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);

      DistributedFileSystem fs = cluster.getFileSystem();
      Path p = new Path("/tmp/abc.log");
      FSDataOutputStream out = fs.create(p);
      byte[] data = "hello\n".getBytes(StandardCharsets.UTF_8);
      out.write(data);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Re-set block token lifetime after potential NN restart
      if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
        final NameNode nnAfter = cluster.getNameNode();
        final BlockManager bmAfter = nnAfter.getNamesystem().getBlockManager();
        final BlockTokenSecretManager smAfter = bmAfter.getBlockTokenSecretManager();
        SecurityTestUtil.setBlockTokenLifetime(smAfter, 1000L);
      }

      FSDataInputStream in = fs.open(p);
      out.close();

      // wait for last block token to expire
      Thread.sleep(2000L);

      byte[] readData = new byte[data.length];
      long startTime = System.currentTimeMillis();
      in.read(readData);
      // DFSInputStream#refetchLocations() minimum wait for 1sec to refetch
      // complete located blocks.
      assertTrue("Should not wait for refetch complete located blocks",
          1000L > (System.currentTimeMillis() - startTime));
    }
  }

  // ============================================================
  // Test variants: testLastLocatedBlockTokenExpiry with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

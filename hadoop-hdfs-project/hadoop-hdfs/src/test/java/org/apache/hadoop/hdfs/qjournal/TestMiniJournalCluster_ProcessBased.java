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
package org.apache.hadoop.hdfs.qjournal;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.qjournal.server.JournalNode;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.test.LambdaTestUtils;
import org.apache.hadoop.test.PathUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestMiniJournalCluster}.
 *
 * Note: This test doesn't use ProcessBasedMiniDFSCluster for cluster creation.
 * It creates a MiniJournalCluster (multiple JournalNodes for testing QJM).
 * The original only used MiniDFSCluster.getBaseDirectory() for test paths.
 *
 * Since MiniJournalCluster doesn't support upgrade() method, this test uses
 * parameterization with only NO_UPGRADE checkpoint for consistency with other tests.
 *
 * @see TestMiniJournalCluster Original test
 */
@RunWith(Parameterized.class)
public class TestMiniJournalCluster_ProcessBased {

  private static final Logger LOG = LoggerFactory.getLogger(TestMiniJournalCluster_ProcessBased.class);
  private static final File TEST_BUILD_DATA = PathUtils.getTestDir(TestMiniJournalCluster_ProcessBased.class);

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    // MiniJournalCluster doesn't have upgrade() method, so only NO_UPGRADE checkpoint
    return Arrays.asList(UpgradeCheckpoints.NO_UPGRADE);
  }

  @Test
  public void testStartStop() throws IOException {
    Configuration conf = new Configuration();
    MiniJournalCluster c = new MiniJournalCluster.Builder(conf)
      .build();
    c.waitActive();
    try {
      URI uri = c.getQuorumJournalURI("myjournal");
      String[] addrs = uri.getAuthority().split(";");
      assertEquals(3, addrs.length);
      
      JournalNode node = c.getJournalNode(0);
      String dir = node.getConf().get(DFSConfigKeys.DFS_JOURNALNODE_EDITS_DIR_KEY);
      assertEquals(
          new File(TEST_BUILD_DATA + File.separator + "journalnode-0")
            .getAbsolutePath(),
          dir);
    } finally {
      c.shutdown();
    }
  }

  @Test
  public void testStartStopWithPorts() throws Exception {
    Configuration conf = new Configuration();

    LambdaTestUtils.intercept(
        IllegalArgumentException.class,
        "Num of http ports (1) should match num of JournalNodes (3)",
        "MiniJournalCluster port validation failed",
        () -> {
          new MiniJournalCluster.Builder(conf).setHttpPorts(8481).build();
        });

    LambdaTestUtils.intercept(
        IllegalArgumentException.class,
        "Num of rpc ports (2) should match num of JournalNodes (3)",
        "MiniJournalCluster port validation failed",
        () -> {
          new MiniJournalCluster.Builder(conf).setRpcPorts(8481, 8482).build();
        });

    LambdaTestUtils.intercept(
        IllegalArgumentException.class,
        "Num of rpc ports (1) should match num of JournalNodes (3)",
        "MiniJournalCluster port validation failed",
        () -> {
          new MiniJournalCluster.Builder(conf).setHttpPorts(800, 9000, 10000).setRpcPorts(8481)
              .build();
        });

    LambdaTestUtils.intercept(
        IllegalArgumentException.class,
        "Num of http ports (4) should match num of JournalNodes (3)",
        "MiniJournalCluster port validation failed",
        () -> {
          new MiniJournalCluster.Builder(conf).setHttpPorts(800, 9000, 1000, 2000)
              .setRpcPorts(8481, 8482, 8483).build();
        });

    final Set<Integer> httpAndRpcPorts = NetUtils.getFreeSocketPorts(6);
    LOG.info("Free socket ports: {}", httpAndRpcPorts);

    for (Integer httpAndRpcPort : httpAndRpcPorts) {
      assertNotEquals("None of the acquired socket port should not be zero", 0,
          httpAndRpcPort.intValue());
    }

    final int[] httpPorts = new int[3];
    final int[] rpcPorts = new int[3];
    int httpPortIdx = 0;
    int rpcPortIdx = 0;
    for (Integer httpAndRpcPort : httpAndRpcPorts) {
      if (httpPortIdx < 3) {
        httpPorts[httpPortIdx++] = httpAndRpcPort;
      } else {
        rpcPorts[rpcPortIdx++] = httpAndRpcPort;
      }
    }

    LOG.info("Http ports selected: {}", httpPorts);
    LOG.info("Rpc ports selected: {}", rpcPorts);

    try (MiniJournalCluster miniJournalCluster = new MiniJournalCluster.Builder(conf)
        .setHttpPorts(httpPorts)
        .setRpcPorts(rpcPorts).build()) {
      miniJournalCluster.waitActive();
      URI uri = miniJournalCluster.getQuorumJournalURI("myjournal");
      String[] addrs = uri.getAuthority().split(";");
      assertEquals(3, addrs.length);

      assertEquals(httpPorts[0], miniJournalCluster.getJournalNode(0).getHttpAddress().getPort());
      assertEquals(httpPorts[1], miniJournalCluster.getJournalNode(1).getHttpAddress().getPort());
      assertEquals(httpPorts[2], miniJournalCluster.getJournalNode(2).getHttpAddress().getPort());

      assertEquals(rpcPorts[0],
          miniJournalCluster.getJournalNode(0).getRpcServer().getAddress().getPort());
      assertEquals(rpcPorts[1],
          miniJournalCluster.getJournalNode(1).getRpcServer().getAddress().getPort());
      assertEquals(rpcPorts[2],
          miniJournalCluster.getJournalNode(2).getRpcServer().getAddress().getPort());

      JournalNode node = miniJournalCluster.getJournalNode(0);
      String dir = node.getConf().get(DFSConfigKeys.DFS_JOURNALNODE_EDITS_DIR_KEY);
      assertEquals(new File(TEST_BUILD_DATA + File.separator + "journalnode-0").getAbsolutePath(),
          dir);
    }
  }

}

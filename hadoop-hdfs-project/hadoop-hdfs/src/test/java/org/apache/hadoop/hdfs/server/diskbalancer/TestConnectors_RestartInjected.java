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
package org.apache.hadoop.hdfs.server.diskbalancer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.diskbalancer.connectors.ClusterConnector;
import org.apache.hadoop.hdfs.server.diskbalancer.connectors.ConnectorFactory;
import org.apache.hadoop.hdfs.server.diskbalancer.datamodel.DiskBalancerCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injected version of TestConnectors.
 *
 * Tests that diskbalancer connectors function correctly across component
 * restarts. The original tests verify NameNode connector and JSON
 * serialization of cluster info.
 *
 * Original tests transformed:
 * - testNameNodeConnector: Connect to cluster, read cluster info, restart,
 *   verify connector still works
 * - testJsonConnector: Serialize cluster info to JSON, restart, verify
 *   serialization still works
 *
 * Restart points:
 * - AfterClusterRead: After reading cluster info via connector
 * - AfterJsonSerialization: After serializing cluster info to JSON
 *
 * Generated variants:
 * - 2 tests x AfterOperation x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestConnectors_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestConnectors_RestartInjected.class);

  private static final int NUM_DATANODES = 3;
  private static final int VOLUME_COUNT = 2; // default volumes in MiniDFSCluster

  private Configuration conf;
  private MiniDFSCluster cluster;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(NUM_DATANODES).build();
    cluster.waitActive();
  }

  @After
  public void teardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  // ============================================================
  // Test: NameNode connector functionality after restart
  // Tests that the connector can read cluster info after restart
  // AfterClusterRead x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Read cluster info via connector, restart, verify we can still
   * read cluster info. Based on TestConnectors#testNameNodeConnector
   */
  private void testNameNodeConnectorWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Read cluster info ===
    ClusterConnector nameNodeConnector =
        ConnectorFactory.getCluster(cluster.getFileSystem(0).getUri(), conf);
    DiskBalancerCluster diskBalancerCluster =
        new DiskBalancerCluster(nameNodeConnector);
    diskBalancerCluster.readClusterInfo();

    // Verify cluster info was read correctly
    assertEquals("Expected number of Datanodes not found.",
        NUM_DATANODES, diskBalancerCluster.getNodes().size());
    assertEquals("Expected number of volumes not found.",
        VOLUME_COUNT, diskBalancerCluster.getNodes().get(0).getVolumeCount());

    // === RESTART INJECTION POINT: After reading cluster info ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh file system reference after restart
    verifyClusterHealth(cluster, cluster.getFileSystem());
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify connector still works after restart ===
    ClusterConnector connectorAfterRestart =
        ConnectorFactory.getCluster(cluster.getFileSystem(0).getUri(), conf);
    DiskBalancerCluster clusterAfterRestart =
        new DiskBalancerCluster(connectorAfterRestart);
    clusterAfterRestart.readClusterInfo();

    assertEquals("Datanode count should be preserved after restart",
        NUM_DATANODES, clusterAfterRestart.getNodes().size());
    assertEquals("Volume count should be preserved after restart",
        VOLUME_COUNT, clusterAfterRestart.getNodes().get(0).getVolumeCount());
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_NN_Graceful() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_NN_Crash() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_SingleDN_Graceful() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_SingleDN_Crash() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_AllDN_Graceful() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_AllDN_Crash() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_NNDN_Graceful() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNameNodeConnector_AfterRead_NNDN_Crash() throws Exception {
    testNameNodeConnectorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: JSON serialization functionality after restart
  // Tests that cluster info JSON serialization works after restart
  // AfterJsonSerialization x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Serialize cluster info to JSON, restart, verify we can still
   * serialize and deserialize. Based on TestConnectors#testJsonConnector
   */
  private void testJsonConnectorWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Serialize cluster info to JSON ===
    ClusterConnector nameNodeConnector =
        ConnectorFactory.getCluster(cluster.getFileSystem(0).getUri(), conf);
    DiskBalancerCluster diskBalancerCluster =
        new DiskBalancerCluster(nameNodeConnector);
    diskBalancerCluster.readClusterInfo();
    String diskBalancerJson = diskBalancerCluster.toJson();

    // Verify serialization works
    assertNotNull("JSON should not be null", diskBalancerJson);
    assertTrue("JSON should not be empty", diskBalancerJson.length() > 0);

    DiskBalancerCluster serializedCluster =
        DiskBalancerCluster.parseJson(diskBalancerJson);
    assertEquals("Parsed cluster is not equal to persisted info.",
        diskBalancerCluster.getNodes().size(),
        serializedCluster.getNodes().size());

    // === RESTART INJECTION POINT: After JSON serialization ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh file system reference after restart
    verifyClusterHealth(cluster, cluster.getFileSystem());
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify JSON serialization still works ===
    ClusterConnector connectorAfterRestart =
        ConnectorFactory.getCluster(cluster.getFileSystem(0).getUri(), conf);
    DiskBalancerCluster clusterAfterRestart =
        new DiskBalancerCluster(connectorAfterRestart);
    clusterAfterRestart.readClusterInfo();

    String jsonAfterRestart = clusterAfterRestart.toJson();
    assertNotNull("JSON after restart should not be null", jsonAfterRestart);
    assertTrue("JSON after restart should not be empty", jsonAfterRestart.length() > 0);

    DiskBalancerCluster parsedAfterRestart =
        DiskBalancerCluster.parseJson(jsonAfterRestart);
    assertEquals("Cluster nodes should match after restart parsing",
        clusterAfterRestart.getNodes().size(),
        parsedAfterRestart.getNodes().size());
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_NN_Graceful() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_NN_Crash() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_SingleDN_Graceful() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_SingleDN_Crash() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_AllDN_Graceful() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_AllDN_Crash() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_NNDN_Graceful() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testJsonConnector_AfterSerialization_NNDN_Crash() throws Exception {
    testJsonConnectorWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

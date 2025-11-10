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

package org.apache.hadoop.yarn.server.process.integration;

import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.Assert;
import org.junit.Test;

/**
 * Basic integration tests for ProcessBasedMiniYARNCluster.
 * Tests fundamental cluster operations: startup, node registration,
 * application submission, node restart, and shutdown.
 */
public class TestProcessBasedMiniYARNClusterBasics extends IntegrationTestBase {

  @Test(timeout = 90000)  // 90 second timeout
  public void testClusterStartupAndShutdown() throws Exception {
    LOG.info("Testing basic cluster startup and shutdown");

    // Create cluster with 1 RM + 2 NMs
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    // Start cluster
    LOG.info("Starting cluster...");
    cluster.start();
    LOG.info("Cluster started successfully");

    // Create and start YarnClient
    yarnClient = YarnClient.createYarnClient();
    YarnConfiguration clientConf = cluster.getConfiguration();
    String rmAddr = clientConf.get(YarnConfiguration.RM_ADDRESS);
    String rmHost = clientConf.get(YarnConfiguration.RM_HOSTNAME);
    System.out.println("DEBUG: RM_ADDRESS from cluster.getConfiguration() = " + rmAddr);
    System.out.println("DEBUG: RM_HOSTNAME from cluster.getConfiguration() = " + rmHost);
    LOG.info("DEBUG: RM_ADDRESS = {}, RM_HOSTNAME = {}", rmAddr, rmHost);
    yarnClient.init(clientConf);
    yarnClient.start();

    // Wait for NodeManagers to register
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Verify cluster metrics
    YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
    Assert.assertEquals("Should have 2 NodeManagers",
        2, metrics.getNumNodeManagers());

    LOG.info("Cluster startup test passed: {}", getClusterMetricsSummary());

    // Shutdown cluster (handled by @After)
  }

  @Test
  public void testNodeManagerRegistration() throws Exception {
    LOG.info("Testing NodeManager registration");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for all NMs to register
    waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

    // Get node reports
    List<NodeReport> nodes = yarnClient.getNodeReports(NodeState.RUNNING);
    Assert.assertEquals("Should have 3 RUNNING nodes", 3, nodes.size());

    // Verify each node has resources
    for (NodeReport node : nodes) {
      LOG.info("Node {}: Memory={}MB, VCores={}",
          node.getNodeId(),
          node.getCapability().getMemory(),
          node.getCapability().getVirtualCores());

      Assert.assertTrue("Node should have positive memory",
          node.getCapability().getMemory() > 0);
      Assert.assertTrue("Node should have positive vcores",
          node.getCapability().getVirtualCores() > 0);
    }

    LOG.info("NodeManager registration test passed");
  }

  @Test
  public void testApplicationSubmissionAndCompletion() throws Exception {
    LOG.info("Testing application submission and completion");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Submit application
    ApplicationId appId = submitSleepApp(5000);  // 5 second sleep
    LOG.info("Application submitted: {}", appId);

    // Wait for application to complete
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Application {} completed successfully", appId);
  }

  @Test
  public void testNodeManagerRestart() throws Exception {
    LOG.info("Testing NodeManager restart");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    YarnClusterMetrics metricsBefore = yarnClient.getYarnClusterMetrics();
    LOG.info("Before restart: {}", getClusterMetricsSummary());

    // Restart NodeManager 0
    LOG.info("Restarting NodeManager 0...");
    cluster.restartNodeManager(0);

    // Wait for NM to re-register
    Thread.sleep(10000);  // Give NM time to restart and reconnect

    // Verify cluster still has 2 NMs
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    YarnClusterMetrics metricsAfter = yarnClient.getYarnClusterMetrics();
    LOG.info("After restart: {}", getClusterMetricsSummary());

    Assert.assertEquals("Should still have 2 NodeManagers after restart",
        2, metricsAfter.getNumNodeManagers());

    LOG.info("NodeManager restart test passed");
  }

  @Test
  public void testResourceManagerRestart() throws Exception {
    LOG.info("Testing ResourceManager restart");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    LOG.info("Before RM restart: {}", getClusterMetricsSummary());

    // Stop YarnClient before RM restart
    yarnClient.stop();

    // Restart ResourceManager
    LOG.info("Restarting ResourceManager...");
    cluster.restartResourceManager(0);

    // Recreate YarnClient
    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for NMs to reconnect
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    LOG.info("After RM restart: {}", getClusterMetricsSummary());

    // Verify we can still submit applications
    ApplicationId appId = submitSleepApp(3000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("ResourceManager restart test passed");
  }

  @Test
  public void testMultipleApplications() throws Exception {
    LOG.info("Testing multiple concurrent applications");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

    // Submit multiple applications
    int numApps = 3;
    ApplicationId[] appIds = new ApplicationId[numApps];

    for (int i = 0; i < numApps; i++) {
      appIds[i] = submitSleepApp(5000);
      LOG.info("Submitted application {}: {}", i, appIds[i]);
      Thread.sleep(1000);  // Stagger submissions
    }

    // Wait for all applications to complete
    for (int i = 0; i < numApps; i++) {
      waitForAppCompletion(appIds[i], APP_TIMEOUT_MS);
      LOG.info("Application {} completed: {}", i, appIds[i]);
    }

    LOG.info("Multiple applications test passed");
  }

  @Test
  public void testClusterWithMinimalConfiguration() throws Exception {
    LOG.info("Testing cluster with minimal configuration (1 RM + 1 NM)");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(1, STARTUP_TIMEOUT_MS);

    // Submit and complete application
    ApplicationId appId = submitSleepApp(3000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Minimal configuration test passed");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetResourceManagerThrowsException() throws Exception {
    LOG.info("Testing that getResourceManager() throws UnsupportedOperationException");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    // This should throw UnsupportedOperationException
    cluster.getResourceManager();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetNodeManagerThrowsException() throws Exception {
    LOG.info("Testing that getNodeManager() throws UnsupportedOperationException");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    // This should throw UnsupportedOperationException
    cluster.getNodeManager(0);
  }

  @Test
  public void testClusterIsUpCheck() throws Exception {
    LOG.info("Testing cluster isClusterUp() check");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    // Before start
    Assert.assertFalse("Cluster should not be up before start",
        cluster.isClusterUp());

    // Start cluster
    cluster.start();

    // After start
    Assert.assertTrue("Cluster should be up after start",
        cluster.isClusterUp());

    // Shutdown cluster
    cluster.shutdown();

    // After shutdown
    Assert.assertFalse("Cluster should not be up after shutdown",
        cluster.isClusterUp());

    // Don't call shutdown again in @After
    cluster = null;

    LOG.info("Cluster isClusterUp() test passed");
  }
}

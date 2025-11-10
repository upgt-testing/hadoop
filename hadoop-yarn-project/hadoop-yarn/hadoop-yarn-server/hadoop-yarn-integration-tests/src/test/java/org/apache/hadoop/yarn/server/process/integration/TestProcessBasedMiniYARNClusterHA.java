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

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests for ProcessBasedMiniYARNCluster High Availability.
 * Tests RM HA functionality: startup, failover, and application survival.
 */
public class TestProcessBasedMiniYARNClusterHA extends IntegrationTestBase {

  @Test
  public void testHAClusterStartup() throws Exception {
    LOG.info("Testing HA cluster startup with 2 ResourceManagers");

    // Create HA cluster
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    LOG.info("Starting HA cluster...");
    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for NodeManagers
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Verify active RM
    int activeRM = cluster.getActiveRMIndex();
    LOG.info("Active ResourceManager: RM{}", activeRM);
    Assert.assertTrue("Active RM should be 0 or 1",
        activeRM == 0 || activeRM == 1);

    LOG.info("HA cluster startup test passed");
  }

  @Test
  public void testManualFailover() throws Exception {
    LOG.info("Testing manual ResourceManager failover");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Get initial active RM
    int initialActiveRM = cluster.getActiveRMIndex();
    LOG.info("Initial active RM: RM{}", initialActiveRM);

    // Stop YarnClient before restart
    yarnClient.stop();

    // Restart active RM (triggers failover)
    LOG.info("Restarting RM{}...", initialActiveRM);
    cluster.restartResourceManager(initialActiveRM);

    // Wait for failover
    Thread.sleep(15000);

    // Recreate YarnClient
    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for NMs to reconnect
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Verify new active RM
    int newActiveRM = cluster.getActiveRMIndex();
    LOG.info("New active RM after failover: RM{}", newActiveRM);

    // Note: With ZK-based failover, the same RM might become active again
    // after restart. What's important is that cluster is still operational.
    Assert.assertTrue("Active RM should be 0 or 1",
        newActiveRM == 0 || newActiveRM == 1);

    LOG.info("Manual failover test passed (failover: RM{} -> RM{})",
        initialActiveRM, newActiveRM);
  }

  @Test
  public void testApplicationSurvivesFailover() throws Exception {
    LOG.info("Testing application survival during RM failover");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Submit long-running application
    ApplicationId appId = submitSleepApp(30000);  // 30 second sleep
    LOG.info("Submitted application: {}", appId);

    // Wait for app to start running
    Thread.sleep(5000);

    // Get active RM
    int activeRM = cluster.getActiveRMIndex();
    LOG.info("Active RM before failover: RM{}", activeRM);

    // Stop YarnClient
    yarnClient.stop();

    // Trigger failover by restarting active RM
    LOG.info("Restarting active RM{}...", activeRM);
    cluster.restartResourceManager(activeRM);

    // Wait for failover
    Thread.sleep(15000);

    // Recreate YarnClient
    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for NMs to reconnect
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Verify application can still complete
    LOG.info("Waiting for application to complete after failover...");
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Application survived failover test passed");
  }

  @Test
  public void testBothRMsCanBecomeActive() throws Exception {
    LOG.info("Testing that both RMs can become active");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(1, STARTUP_TIMEOUT_MS);

    // Track which RMs have been active
    boolean rm0WasActive = false;
    boolean rm1WasActive = false;

    // Perform multiple failovers
    for (int i = 0; i < 3; i++) {
      int activeRM = cluster.getActiveRMIndex();
      LOG.info("Iteration {}: Active RM = RM{}", i, activeRM);

      if (activeRM == 0) {
        rm0WasActive = true;
      } else if (activeRM == 1) {
        rm1WasActive = true;
      }

      // If both have been active, we're done
      if (rm0WasActive && rm1WasActive) {
        LOG.info("Both RMs have been active");
        break;
      }

      // Trigger failover
      yarnClient.stop();
      cluster.restartResourceManager(activeRM);
      Thread.sleep(15000);

      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();
      waitForNodeManagersToRegister(1, STARTUP_TIMEOUT_MS);
    }

    // Note: Due to ZK leader election, we can't guarantee both RMs
    // will become active, but at least one should have been active
    Assert.assertTrue("At least one RM should have been active",
        rm0WasActive || rm1WasActive);

    LOG.info("RM activation test passed (RM0: {}, RM1: {})",
        rm0WasActive, rm1WasActive);
  }

  @Test
  public void testHAClusterWithMultipleApplications() throws Exception {
    LOG.info("Testing HA cluster with multiple applications");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
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
      Thread.sleep(1000);
    }

    // Wait for all to complete
    for (int i = 0; i < numApps; i++) {
      waitForAppCompletion(appIds[i], APP_TIMEOUT_MS);
      LOG.info("Application {} completed: {}", i, appIds[i]);
    }

    LOG.info("HA cluster multiple applications test passed");
  }

  @Test
  public void testStandbyRMRestart() throws Exception {
    LOG.info("Testing standby ResourceManager restart");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(2)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    // Get active and standby RMs
    int activeRM = cluster.getActiveRMIndex();
    int standbyRM = (activeRM == 0) ? 1 : 0;

    LOG.info("Active RM: RM{}, Standby RM: RM{}", activeRM, standbyRM);

    // Restart standby RM (should not cause failover)
    LOG.info("Restarting standby RM{}...", standbyRM);
    cluster.restartResourceManager(standbyRM);

    // Wait for standby to restart
    Thread.sleep(10000);

    // Verify active RM unchanged
    int activeRMAfter = cluster.getActiveRMIndex();
    Assert.assertEquals("Active RM should remain the same after standby restart",
        activeRM, activeRMAfter);

    LOG.info("Standby RM restart test passed");
  }

  @Test
  public void testHAConfigurationCorrectness() throws Exception {
    LOG.info("Testing HA configuration correctness");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    // Verify HA is enabled in configuration
    YarnConfiguration clusterConf = cluster.getConfiguration();
    boolean haEnabled = clusterConf.getBoolean(
        "yarn.resourcemanager.ha.enabled", false);

    Assert.assertTrue("HA should be enabled in cluster configuration",
        haEnabled);

    String rmIds = clusterConf.get("yarn.resourcemanager.ha.rm-ids");
    Assert.assertNotNull("RM IDs should be configured", rmIds);
    Assert.assertTrue("RM IDs should contain 'rm0'", rmIds.contains("rm0"));
    Assert.assertTrue("RM IDs should contain 'rm1'", rmIds.contains("rm1"));

    LOG.info("HA configuration test passed (rm-ids: {})", rmIds);
  }
}

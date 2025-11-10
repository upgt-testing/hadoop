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
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.UpgradeTestHelper;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Integration tests for YARN rolling upgrades using ProcessBasedMiniYARNCluster.
 * Tests various upgrade scenarios: rolling NM upgrades, RM upgrades, and
 * application survival during upgrades.
 *
 * Note: These tests simulate upgrades by restarting nodes. Full version
 * switching would require cluster API enhancements to change Hadoop
 * distributions per node at runtime.
 */
public class TestProcessBasedMiniYARNClusterUpgrade extends IntegrationTestBase {

  @Test
  public void testRollingNodeManagerUpgrade() throws Exception {
    LOG.info("Testing rolling NodeManager upgrade simulation");

    // Start cluster (all nodes on 3.3.5)
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

    LOG.info("Initial cluster state: {}", getClusterMetricsSummary());

    // Simulate rolling upgrade by restarting NMs one-by-one
    for (int i = 0; i < 3; i++) {
      LOG.info("Upgrading NodeManager {}...", i);

      // Stop YarnClient (connection will break during NM restart)
      yarnClient.stop();

      // Restart NM
      cluster.restartNodeManager(i);

      // Wait for restart
      Thread.sleep(10000);

      // Recreate YarnClient
      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();

      // Wait for all NMs to reconnect
      waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

      LOG.info("After upgrading NM{}: {}", i, getClusterMetricsSummary());
    }

    // Verify cluster still functional
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Rolling NM upgrade test passed");
  }

  @Test
  public void testApplicationSurvivesNodeManagerUpgrade() throws Exception {
    LOG.info("Testing application survival during NM upgrades");

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

    // Submit long-running application
    ApplicationId appId = submitSleepApp(30000);  // 30 seconds
    LOG.info("Submitted long-running application: {}", appId);

    // Wait for app to start
    Thread.sleep(5000);

    // Upgrade NMs one by one while app is running
    for (int i = 0; i < 3; i++) {
      LOG.info("Upgrading NM{} while application is running...", i);

      yarnClient.stop();
      cluster.restartNodeManager(i);
      Thread.sleep(10000);

      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();

      waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);
    }

    // Application should still complete
    LOG.info("Waiting for application to complete after upgrades...");
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Application survival test passed");
  }

  @Test
  public void testBatchNodeManagerUpgrade() throws Exception {
    LOG.info("Testing batch NodeManager upgrade");

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(4)
        .allNodesHadoopDistribution(HADOOP_3_3_5)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(4, STARTUP_TIMEOUT_MS);

    LOG.info("Before batch upgrade: {}", getClusterMetricsSummary());

    // Upgrade 2 NMs in batch
    LOG.info("Upgrading NMs 0 and 1 in batch...");

    yarnClient.stop();

    cluster.restartNodeManager(0);
    cluster.restartNodeManager(1);

    Thread.sleep(15000);

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(4, STARTUP_TIMEOUT_MS);

    LOG.info("After first batch: {}", getClusterMetricsSummary());

    // Upgrade remaining 2 NMs
    LOG.info("Upgrading NMs 2 and 3 in batch...");

    yarnClient.stop();

    cluster.restartNodeManager(2);
    cluster.restartNodeManager(3);

    Thread.sleep(15000);

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(4, STARTUP_TIMEOUT_MS);

    LOG.info("After second batch: {}", getClusterMetricsSummary());

    // Verify cluster functional
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Batch upgrade test passed");
  }

  @Test
  public void testHAResourceManagerUpgrade() throws Exception {
    LOG.info("Testing RM upgrade in HA configuration");

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

    // Submit application before upgrade
    ApplicationId preUpgradeApp = submitSleepApp(10000);
    LOG.info("Submitted pre-upgrade application: {}", preUpgradeApp);

    // Get active RM
    int activeRM = cluster.getActiveRMIndex();
    int standbyRM = (activeRM == 0) ? 1 : 0;

    LOG.info("Before upgrade - Active: RM{}, Standby: RM{}",
        activeRM, standbyRM);

    // Step 1: Upgrade standby RM
    LOG.info("Step 1: Upgrading standby RM{}...", standbyRM);
    cluster.restartResourceManager(standbyRM);
    Thread.sleep(15000);

    // Step 2: Trigger failover by upgrading active RM
    LOG.info("Step 2: Upgrading active RM{} (triggers failover)...", activeRM);

    yarnClient.stop();
    cluster.restartResourceManager(activeRM);
    Thread.sleep(20000);

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    int newActiveRM = cluster.getActiveRMIndex();
    LOG.info("After upgrade - New active: RM{}", newActiveRM);

    // Verify pre-upgrade application completed
    waitForAppCompletion(preUpgradeApp, APP_TIMEOUT_MS);

    // Submit post-upgrade application
    ApplicationId postUpgradeApp = submitSleepApp(5000);
    waitForAppCompletion(postUpgradeApp, APP_TIMEOUT_MS);

    LOG.info("HA RM upgrade test passed");
  }

  @Test
  public void testUpgradeTestHelper() throws Exception {
    LOG.info("Testing UpgradeTestHelper utility");

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

    // Create upgrade helper
    UpgradeTestHelper helper = new UpgradeTestHelper(cluster);

    // Verify cluster healthy
    boolean healthy = helper.verifyClusterHealthy();
    Assert.assertTrue("Cluster should be healthy", healthy);

    LOG.info("UpgradeTestHelper test passed");
  }

  @Test
  public void testVersionCompatibilityCheck() throws Exception {
    LOG.info("Testing version compatibility verification");

    // Test compatible versions
    boolean compatible335_340 = UpgradeTestHelper.areVersionsCompatible(
        "3.3.5", "3.4.0");
    LOG.info("3.3.5 <-> 3.4.0 compatible: {}", compatible335_340);

    // Test same version
    boolean compatible335_335 = UpgradeTestHelper.areVersionsCompatible(
        "3.3.5", "3.3.5");
    Assert.assertTrue("Same version should be compatible", compatible335_335);

    // Get upgrade path
    String path = UpgradeTestHelper.getUpgradePath("3.3.5", "3.4.0");
    LOG.info("Upgrade path 3.3.5 -> 3.4.0: {}", path);
    Assert.assertNotNull("Upgrade path should not be null", path);

    LOG.info("Version compatibility check test passed");
  }

  @Test
  public void testGradualNodeManagerUpgradeWithTraffic() throws Exception {
    LOG.info("Testing gradual NM upgrade with continuous application traffic");

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

    // Submit multiple long-running applications
    ApplicationId[] apps = new ApplicationId[2];
    for (int i = 0; i < 2; i++) {
      apps[i] = submitSleepApp(40000);  // 40 seconds
      LOG.info("Submitted long-running app {}: {}", i, apps[i]);
      Thread.sleep(2000);
    }

    // Gradually upgrade NMs while apps are running
    for (int i = 0; i < 3; i++) {
      LOG.info("Upgrading NM{} with {} apps running...", i, apps.length);

      yarnClient.stop();

      cluster.restartNodeManager(i);

      Thread.sleep(12000);

      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();

      waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

      LOG.info("NM{} upgraded, cluster: {}", i, getClusterMetricsSummary());
    }

    // Wait for all applications to complete
    for (int i = 0; i < apps.length; i++) {
      waitForAppCompletion(apps[i], APP_TIMEOUT_MS);
      LOG.info("App {} completed successfully", i);
    }

    LOG.info("Gradual upgrade with traffic test passed");
  }

  @Test
  public void testMultiPhaseUpgrade() throws Exception {
    LOG.info("Testing multi-phase upgrade (NMs first, then RM)");

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

    // Phase 1: Upgrade all NMs
    LOG.info("Phase 1: Upgrading all NodeManagers...");
    for (int i = 0; i < 2; i++) {
      yarnClient.stop();
      cluster.restartNodeManager(i);
      Thread.sleep(10000);

      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();
      waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);
    }

    LOG.info("Phase 1 complete: All NMs upgraded");

    // Phase 2: Upgrade RM
    LOG.info("Phase 2: Upgrading ResourceManager...");
    yarnClient.stop();
    cluster.restartResourceManager(0);
    Thread.sleep(15000);

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    LOG.info("Phase 2 complete: RM upgraded");

    // Verify cluster operational
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Multi-phase upgrade test passed");
  }
}

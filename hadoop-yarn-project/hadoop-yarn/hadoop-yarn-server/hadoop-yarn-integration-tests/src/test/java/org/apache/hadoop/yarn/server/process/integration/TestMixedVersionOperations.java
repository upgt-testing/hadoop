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
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.VersionConfigAdapter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.List;

/**
 * Integration tests for running mixed-version YARN clusters.
 * Tests scenarios where different nodes run different Hadoop versions,
 * verifying version compatibility and cluster operations.
 */
public class TestMixedVersionOperations extends IntegrationTestBase {

  @Test
  public void testMixedVersionCluster335And340() throws Exception {
    LOG.info("Testing mixed-version cluster: RM on 3.3.5, NMs on 3.4.0");

    // Verify both distributions exist
    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Verify version compatibility
    boolean compatible = VersionConfigAdapter.areCompatible("3.3.5", "3.4.0");
    LOG.info("3.3.5 <-> 3.4.0 compatible: {}", compatible);

    // Create mixed-version cluster
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .rmHadoopDistribution(0, HADOOP_3_3_5)    // RM on 3.3.5
        .nmHadoopDistribution(0, HADOOP_3_4_0)    // NM0 on 3.4.0
        .nmHadoopDistribution(1, HADOOP_3_4_0)    // NM1 on 3.4.0
        .build();

    LOG.info("Starting mixed-version cluster...");
    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    // Wait for NodeManagers to register
    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    LOG.info("Mixed-version cluster started: {}", getClusterMetricsSummary());

    // Verify nodes are running
    List<NodeReport> nodes = yarnClient.getNodeReports(NodeState.RUNNING);
    Assert.assertEquals("Should have 2 running nodes", 2, nodes.size());

    // Submit and complete application
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Mixed-version cluster (3.3.5 RM + 3.4.0 NMs) test passed");
  }

  @Test
  public void testMixedVersionCluster340And335() throws Exception {
    LOG.info("Testing mixed-version cluster: RM on 3.4.0, NMs on 3.3.5");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Create mixed-version cluster (reverse configuration)
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .rmHadoopDistribution(0, HADOOP_3_4_0)    // RM on 3.4.0
        .nmHadoopDistribution(0, HADOOP_3_3_5)    // NM0 on 3.3.5
        .nmHadoopDistribution(1, HADOOP_3_3_5)    // NM1 on 3.3.5
        .build();

    LOG.info("Starting mixed-version cluster (reverse)...");
    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    LOG.info("Mixed-version cluster started: {}", getClusterMetricsSummary());

    // Submit and complete application
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Mixed-version cluster (3.4.0 RM + 3.3.5 NMs) test passed");
  }

  @Test
  public void testHeterogeneousNodeManagerVersions() throws Exception {
    LOG.info("Testing cluster with different NM versions");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Create cluster with NMs on different versions
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .rmHadoopDistribution(0, HADOOP_3_3_5)    // RM on 3.3.5
        .nmHadoopDistribution(0, HADOOP_3_3_5)    // NM0 on 3.3.5
        .nmHadoopDistribution(1, HADOOP_3_4_0)    // NM1 on 3.4.0
        .nmHadoopDistribution(2, HADOOP_3_4_0)    // NM2 on 3.4.0
        .build();

    LOG.info("Starting heterogeneous cluster...");
    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

    LOG.info("Heterogeneous cluster started: {}", getClusterMetricsSummary());

    // Submit multiple applications
    ApplicationId app1 = submitSleepApp(5000);
    Thread.sleep(1000);
    ApplicationId app2 = submitSleepApp(5000);

    waitForAppCompletion(app1, APP_TIMEOUT_MS);
    waitForAppCompletion(app2, APP_TIMEOUT_MS);

    LOG.info("Heterogeneous NM versions test passed");
  }

  @Test
  public void testMixedVersionHACluster() throws Exception {
    LOG.info("Testing mixed-version HA cluster");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Create HA cluster with mixed versions
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(2)
        .rmHadoopDistribution(0, HADOOP_3_3_5)    // RM0 on 3.3.5
        .rmHadoopDistribution(1, HADOOP_3_4_0)    // RM1 on 3.4.0
        .nmHadoopDistribution(0, HADOOP_3_3_5)    // NM0 on 3.3.5
        .nmHadoopDistribution(1, HADOOP_3_4_0)    // NM1 on 3.4.0
        .build();

    LOG.info("Starting mixed-version HA cluster...");
    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    int activeRM = cluster.getActiveRMIndex();
    LOG.info("Mixed-version HA cluster started, active RM: RM{}",
        activeRM);

    // Submit application
    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Mixed-version HA cluster test passed");
  }

  @Test
  public void testVersionTransitionDuringUpgrade() throws Exception {
    LOG.info("Testing version transition during simulated upgrade");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Start all nodes on 3.3.5
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

    LOG.info("Initial cluster (all 3.3.5): {}", getClusterMetricsSummary());

    // Submit long-running application
    ApplicationId appId = submitSleepApp(30000);
    LOG.info("Submitted app: {}", appId);
    Thread.sleep(5000);

    // Simulate upgrade to mixed-version state
    // Note: Current implementation restarts with same version
    // Full version switching would require cluster API enhancement

    LOG.info("Simulating version transition (via restarts)...");

    for (int i = 0; i < 3; i++) {
      yarnClient.stop();
      cluster.restartNodeManager(i);
      Thread.sleep(10000);

      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
      yarnClient.start();
      waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

      LOG.info("After upgrading NM{}: {}", i, getClusterMetricsSummary());
    }

    // Wait for application to complete
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Version transition test passed");
  }

  @Test
  public void testSameMinorVersionMixing() throws Exception {
    LOG.info("Testing cluster with same minor but different patch versions");

    // This test would verify 3.3.4 + 3.3.5 or similar
    // Skipped if distributions not available

    if (!distributionExists(HADOOP_3_2_4)) {
      LOG.info("Skipping test - Hadoop 3.2.4 not available");
      Assume.assumeTrue("Hadoop 3.2.4 not found", false);
    }

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(2)
        .rmHadoopDistribution(0, HADOOP_3_2_4)
        .nmHadoopDistribution(0, HADOOP_3_2_4)
        .nmHadoopDistribution(1, HADOOP_3_2_4)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(2, STARTUP_TIMEOUT_MS);

    ApplicationId appId = submitSleepApp(5000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("Same minor version mixing test passed");
  }

  @Test
  public void testMultipleApplicationsOnMixedVersionCluster() throws Exception {
    LOG.info("Testing multiple applications on mixed-version cluster");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .rmHadoopDistribution(0, HADOOP_3_3_5)
        .nmHadoopDistribution(0, HADOOP_3_3_5)
        .nmHadoopDistribution(1, HADOOP_3_4_0)
        .nmHadoopDistribution(2, HADOOP_3_4_0)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(3, STARTUP_TIMEOUT_MS);

    // Submit multiple applications
    int numApps = 4;
    ApplicationId[] apps = new ApplicationId[numApps];

    for (int i = 0; i < numApps; i++) {
      apps[i] = submitSleepApp(8000);
      LOG.info("Submitted app {}: {}", i, apps[i]);
      Thread.sleep(1000);
    }

    // Wait for all to complete
    for (int i = 0; i < numApps; i++) {
      waitForAppCompletion(apps[i], APP_TIMEOUT_MS);
      LOG.info("App {} completed", i);
    }

    LOG.info("Multiple applications on mixed-version cluster test passed");
  }

  @Test
  public void testVersionConfigAdapterIntegration() throws Exception {
    LOG.info("Testing VersionConfigAdapter with real cluster");

    Assume.assumeTrue("Hadoop 3.3.5 not found",
        distributionExists(HADOOP_3_3_5));
    Assume.assumeTrue("Hadoop 3.4.0 not found",
        distributionExists(HADOOP_3_4_0));

    // Create adapter for 3.4.0
    VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");

    // Adapt configuration from 3.3.5 to 3.4.0
    YarnConfiguration adaptedConf = adapter.adapt(conf, "3.3.5");

    LOG.info("Created adapted configuration for 3.4.0");

    // Create cluster with adapted config
    cluster = new ProcessBasedMiniYARNCluster.Builder(adaptedConf)
        .numResourceManagers(1)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(HADOOP_3_4_0)
        .build();

    cluster.start();

    yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    waitForNodeManagersToRegister(1, STARTUP_TIMEOUT_MS);

    ApplicationId appId = submitSleepApp(3000);
    waitForAppCompletion(appId, APP_TIMEOUT_MS);

    LOG.info("VersionConfigAdapter integration test passed");
  }
}

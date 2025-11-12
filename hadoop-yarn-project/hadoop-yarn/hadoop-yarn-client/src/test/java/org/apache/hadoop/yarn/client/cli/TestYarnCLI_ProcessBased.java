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
package org.apache.hadoop.yarn.client.cli;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.resourcemanager.reservation.ReservationSystemTestUtil;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacitySchedulerConfiguration;
import static org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacitySchedulerConfiguration.PREFIX;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBased version of {@link TestYarnCLI} integration tests.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * This file contains only the integration tests from TestYarnCLI that use
 * real MiniYARNCluster (3 tests). The unit tests with mocked YarnClient
 * remain in the original TestYarnCLI.java.
 *
 * @see TestYarnCLI Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class TestYarnCLI_ProcessBased extends YarnUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_YARNCLIENT_START",
      "AFTER_GET_QUEUE_INFO"
    );
  }

  ByteArrayOutputStream sysOutStream;
  private PrintStream sysOut;

  @Before
  public void setupStreams() {
    sysOutStream = new ByteArrayOutputStream();
    sysOut = new PrintStream(sysOutStream);
    System.setOut(sysOut);
  }

  private QueueCLI createAndGetQueueCLI(YarnClient yarnClient) {
    QueueCLI cli = new QueueCLI();
    cli.setClient(yarnClient);
    cli.setSysOutPrintStream(sysOut);
    return cli;
  }

  @Test(timeout = 120000)
  public void testGetQueueInfoOverrideIntraQueuePreemption() throws Exception {
    CapacitySchedulerConfiguration localConf = new CapacitySchedulerConfiguration();
    ReservationSystemTestUtil.setupQueueConfiguration(localConf);
    localConf.setClass(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class,
        ResourceScheduler.class);
    localConf.setBoolean(YarnConfiguration.RM_SCHEDULER_ENABLE_MONITORS, true);
    localConf.set(YarnConfiguration.RM_SCHEDULER_MONITOR_POLICIES,
        "org.apache.hadoop.yarn.server.resourcemanager.monitor.capacity."
        + "ProportionalCapacityPreemptionPolicy");
    // Turn on cluster-wide intra-queue preemption
    localConf.setBoolean(
        CapacitySchedulerConfiguration.INTRAQUEUE_PREEMPTION_ENABLED, true);
    // Disable intra-queue preemption for all queues
    localConf.setBoolean(CapacitySchedulerConfiguration.PREFIX
        + "root.intra-queue-preemption.disable_preemption", true);
    // Enable intra-queue preemption for the a1 queue
    localConf.setBoolean(CapacitySchedulerConfiguration.PREFIX
        + "root.a.a1.intra-queue-preemption.disable_preemption", false);

    cluster = new ProcessBasedMiniYARNCluster.Builder(localConf)
        .numNodeManagers(1)
        .numResourceManagers(2)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    checkpoint("AFTER_YARNCLIENT_START");

    try {
      QueueCLI cli = createAndGetQueueCLI(yarnClient);
      sysOutStream.reset();
      // Get status for the root.a queue
      int result = cli.run(new String[] { "-status", "a" });
      assertEquals(0, result);
      String queueStatusOut = sysOutStream.toString();
      Assert.assertTrue(queueStatusOut
          .contains("\tPreemption : enabled"));
      // In-queue preemption is disabled at the "root.a" queue level
      Assert.assertTrue(queueStatusOut
          .contains("Intra-queue Preemption : disabled"));

      checkpoint("AFTER_GET_QUEUE_INFO");

      cli = createAndGetQueueCLI(yarnClient);
      sysOutStream.reset();
      // Get status for the root.a.a1 queue
      result = cli.run(new String[] { "-status", "a1" });
      assertEquals(0, result);
      queueStatusOut = sysOutStream.toString();
      Assert.assertTrue(queueStatusOut
          .contains("\tPreemption : enabled"));
      // In-queue preemption is enabled at the "root.a.a1" queue level
      Assert.assertTrue(queueStatusOut
          .contains("Intra-queue Preemption : enabled"));
    } finally {
      if (yarnClient != null) {
        yarnClient.stop();
      }
      // Cluster cleanup handled by YarnUpgradeTestBase @After
    }
  }

  @Test(timeout = 120000)
  public void testGetQueueInfoPreemptionEnabled() throws Exception {
    CapacitySchedulerConfiguration localConf = new CapacitySchedulerConfiguration();
    ReservationSystemTestUtil.setupQueueConfiguration(localConf);
    localConf.setClass(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class,
        ResourceScheduler.class);
    localConf.setBoolean(YarnConfiguration.RM_SCHEDULER_ENABLE_MONITORS, true);
    localConf.set(YarnConfiguration.RM_SCHEDULER_MONITOR_POLICIES,
        "org.apache.hadoop.yarn.server.resourcemanager.monitor.capacity."
        + "ProportionalCapacityPreemptionPolicy");
    localConf.setBoolean(
        CapacitySchedulerConfiguration.INTRAQUEUE_PREEMPTION_ENABLED, true);

    cluster = new ProcessBasedMiniYARNCluster.Builder(localConf)
        .numNodeManagers(1)
        .numResourceManagers(2)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    checkpoint("AFTER_YARNCLIENT_START");

    try {
      QueueCLI cli = createAndGetQueueCLI(yarnClient);
      sysOutStream.reset();
      int result = cli.run(new String[] { "-status", "a1" });
      assertEquals(0, result);
      String queueStatusOut = sysOutStream.toString();
      Assert.assertTrue(queueStatusOut
          .contains("\tPreemption : enabled"));
      Assert.assertTrue(queueStatusOut
          .contains("Intra-queue Preemption : enabled"));

      checkpoint("AFTER_GET_QUEUE_INFO");
    } finally {
      if (yarnClient != null) {
        yarnClient.stop();
      }
      // Cluster cleanup handled by YarnUpgradeTestBase @After
    }
  }

  @Test(timeout = 120000)
  public void testGetQueueInfoPreemptionDisabled() throws Exception {
    CapacitySchedulerConfiguration localConf = new CapacitySchedulerConfiguration();
    ReservationSystemTestUtil.setupQueueConfiguration(localConf);
    localConf.setClass(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class,
        ResourceScheduler.class);
    localConf.setBoolean(YarnConfiguration.RM_SCHEDULER_ENABLE_MONITORS, true);
    localConf.set(YarnConfiguration.RM_SCHEDULER_MONITOR_POLICIES,
        "org.apache.hadoop.yarn.server.resourcemanager.monitor.capacity."
        + "ProportionalCapacityPreemptionPolicy");
    localConf.setBoolean(YarnConfiguration.RM_SCHEDULER_ENABLE_MONITORS, true);
    localConf.setBoolean(PREFIX + "root.a.a1.disable_preemption", true);

    cluster = new ProcessBasedMiniYARNCluster.Builder(localConf)
        .numNodeManagers(1)
        .numResourceManagers(2)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(cluster.getConfiguration());
    yarnClient.start();

    checkpoint("AFTER_YARNCLIENT_START");

    try {
      QueueCLI cli = createAndGetQueueCLI(yarnClient);
      sysOutStream.reset();
      int result = cli.run(new String[] { "-status", "a1" });
      assertEquals(0, result);
      String queueStatusOut = sysOutStream.toString();
      Assert.assertTrue(queueStatusOut
          .contains("\tPreemption : disabled"));
      Assert.assertTrue(queueStatusOut
          .contains("Intra-queue Preemption : disabled"));

      checkpoint("AFTER_GET_QUEUE_INFO");
    } finally {
      if (yarnClient != null) {
        yarnClient.stop();
      }
      // Cluster cleanup handled by YarnUpgradeTestBase @After
    }
  }
}

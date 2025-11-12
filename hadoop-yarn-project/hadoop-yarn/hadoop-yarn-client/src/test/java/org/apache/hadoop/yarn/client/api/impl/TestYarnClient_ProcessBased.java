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

package org.apache.hadoop.yarn.client.api.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.hadoop.yarn.util.Records;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBased version of {@link TestYarnClient} integration tests.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * Note: This file contains only the integration tests from TestYarnClient that
 * use MiniYARNCluster. The unit tests using MockRM remain in the original file
 * as they don't require process-based transformation.
 *
 * @see TestYarnClient Original test with all YarnClient tests
 */
@RunWith(Parameterized.class)
public class TestYarnClient_ProcessBased extends YarnUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,           // Baseline - no upgrade
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,  // After cluster initialization
      "AFTER_CLIENT_START",                        // After YarnClient started
      "AFTER_APP_CREATE",                          // After application created
      "AFTER_APP_SUBMIT",                          // After application submitted
      "BEFORE_VALIDATION"                          // Before final validation
    );
  }

  @Test(timeout = 120000)
  public void testSubmitIncorrectQueueToCapacityScheduler() throws Exception {
    YarnConfiguration testConf = new YarnConfiguration();
    testConf.set(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class.getName());

    cluster = new ProcessBasedMiniYARNCluster.Builder(testConf)
        .numNodeManagers(1)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    YarnClient rmClient = YarnClient.createYarnClient();
    rmClient.init(cluster.getConfiguration());
    rmClient.start();

    checkpoint("AFTER_CLIENT_START");

    try {
      YarnClientApplication newApp = rmClient.createApplication();
      ApplicationId appId = newApp.getNewApplicationResponse().getApplicationId();

      checkpoint("AFTER_APP_CREATE");

      // Create launch context for app master
      ApplicationSubmissionContext appContext = Records.newRecord(ApplicationSubmissionContext.class);
      appContext.setApplicationId(appId);
      appContext.setApplicationName("test");

      // Set the queue to a non-existent queue - should fail
      appContext.setQueue("nonexist");

      // Set up the container launch context for the application master
      ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
      appContext.setAMContainerSpec(amContainer);
      appContext.setResource(Resource.newInstance(1024, 1));

      checkpoint("BEFORE_VALIDATION");

      // Submit the application - should throw exception
      try {
        rmClient.submitApplication(appContext);
        Assert.fail("Job submission should have thrown an exception");
      } catch (YarnException e) {
        Assert.assertTrue(e.getMessage().contains("Failed to submit"));
      }
    } finally {
      if (rmClient != null) {
        rmClient.stop();
      }
    }

    // Cluster cleanup handled by YarnUpgradeTestBase @After
  }

  @Test(timeout = 120000)
  public void testAMMRTokens() throws Exception {
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    YarnClient rmClient = YarnClient.createYarnClient();
    rmClient.init(cluster.getConfiguration());
    rmClient.start();

    checkpoint("AFTER_CLIENT_START");

    try {
      // Test 1: Managed AMs don't return AMRM token
      ApplicationId appId = createApp(rmClient, false);
      waitTillAccepted(rmClient, appId, false);
      Assert.assertNull("Managed AM should not have AMRM token", rmClient.getAMRMToken(appId));

      checkpoint("AFTER_APP_CREATE");

      // Test 2: Unmanaged AMs do return AMRM token
      appId = createApp(rmClient, true);
      waitTillAccepted(rmClient, appId, true);
      long start = System.currentTimeMillis();
      while (rmClient.getAMRMToken(appId) == null) {
        if (System.currentTimeMillis() - start > 20 * 1000) {
          Assert.fail("AMRM token is null");
        }
        Thread.sleep(100);
      }
      Assert.assertNotNull("Unmanaged AM should have AMRM token", rmClient.getAMRMToken(appId));

      checkpoint("AFTER_APP_SUBMIT");

      // Test 3: Other users don't get AMRM token
      final Configuration yarnConf = cluster.getConfiguration();
      UserGroupInformation other = UserGroupInformation.createUserForTesting("foo", new String[]{});
      appId = other.doAs(new PrivilegedExceptionAction<ApplicationId>() {
        @Override
        public ApplicationId run() throws Exception {
          YarnClient otherClient = YarnClient.createYarnClient();
          otherClient.init(yarnConf);
          otherClient.start();
          ApplicationId otherAppId = createApp(otherClient, true);
          waitTillAccepted(otherClient, otherAppId, true);
          long start = System.currentTimeMillis();
          while (otherClient.getAMRMToken(otherAppId) == null) {
            if (System.currentTimeMillis() - start > 20 * 1000) {
              Assert.fail("AMRM token is null");
            }
            Thread.sleep(100);
          }
          Assert.assertNotNull("Unmanaged AM should have AMRM token", otherClient.getAMRMToken(otherAppId));
          otherClient.stop();
          return otherAppId;
        }
      });

      checkpoint("BEFORE_VALIDATION");

      // Original user should not get AMRM token for other user's app
      Assert.assertNull("Other user's AMRM token should not be accessible", rmClient.getAMRMToken(appId));
    } finally {
      if (rmClient != null) {
        rmClient.stop();
      }
    }

    // Cluster cleanup handled by YarnUpgradeTestBase @After
  }

  private ApplicationId createApp(YarnClient rmClient, boolean unmanaged) throws Exception {
    YarnClientApplication newApp = rmClient.createApplication();
    ApplicationId appId = newApp.getNewApplicationResponse().getApplicationId();

    // Create launch context for app master
    ApplicationSubmissionContext appContext = Records.newRecord(ApplicationSubmissionContext.class);
    appContext.setApplicationId(appId);
    appContext.setApplicationName("test");

    // Set the priority for the application master
    Priority pri = Records.newRecord(Priority.class);
    pri.setPriority(1);
    appContext.setPriority(pri);

    // Set the queue to which this application is to be submitted in the RM
    appContext.setQueue("default");

    // Set up the container launch context for the application master
    ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
    appContext.setAMContainerSpec(amContainer);
    appContext.setResource(Resource.newInstance(1024, 1));
    appContext.setUnmanagedAM(unmanaged);

    // Submit the application to the applications manager
    rmClient.submitApplication(appContext);

    return appId;
  }

  private void waitTillAccepted(YarnClient rmClient, ApplicationId appId, boolean unmanagedApplication)
      throws Exception {
    long start = System.currentTimeMillis();
    while (true) {
      ApplicationReport report = rmClient.getApplicationReport(appId);
      YarnApplicationState state = report.getYarnApplicationState();
      if (state == YarnApplicationState.ACCEPTED || state == YarnApplicationState.RUNNING ||
          state == YarnApplicationState.FINISHED || state == YarnApplicationState.FAILED ||
          state == YarnApplicationState.KILLED) {
        return;
      }
      if (System.currentTimeMillis() - start > 20 * 1000) {
        Assert.fail("Application " + appId + " is still in " + state);
      }
      Thread.sleep(100);
    }
  }
}

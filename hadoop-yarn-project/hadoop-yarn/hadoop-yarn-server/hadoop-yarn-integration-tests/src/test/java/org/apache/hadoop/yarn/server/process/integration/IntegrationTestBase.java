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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.DirectoryManager;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.util.Records;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for ProcessBasedMiniYARNCluster integration tests.
 * Provides common setup, teardown, and helper methods.
 */
public abstract class IntegrationTestBase {

  protected static final Logger LOG =
      LoggerFactory.getLogger(IntegrationTestBase.class);

  /** Base directory for Hadoop test distributions */
  protected static final String HADOOP_DIST_BASE =
      "/Users/allenwang/xlab/hadoop-test-distributions";

  /** Hadoop 3.3.5 distribution path */
  protected static final String HADOOP_3_3_5 =
      HADOOP_DIST_BASE + "/hadoop-3.3.5";

  /** Hadoop 3.4.0 distribution path */
  protected static final String HADOOP_3_4_0 =
      HADOOP_DIST_BASE + "/hadoop-3.4.0";

  /** Hadoop 3.2.4 distribution path */
  protected static final String HADOOP_3_2_4 =
      HADOOP_DIST_BASE + "/hadoop-3.2.4";

  /** Hadoop 2.10.2 distribution path */
  protected static final String HADOOP_2_10_2 =
      HADOOP_DIST_BASE + "/hadoop-2.10.2";

  /** Default startup timeout (ms) */
  protected static final long STARTUP_TIMEOUT_MS = 60000;

  /** Default application timeout (ms) */
  protected static final long APP_TIMEOUT_MS = 120000;

  protected ProcessBasedMiniYARNCluster cluster;
  protected YarnClient yarnClient;
  protected YarnConfiguration conf;

  @Rule
  public TestName testName = new TestName();

  @Rule
  public TestWatcher watcher = new TestWatcher() {
    @Override
    protected void failed(Throwable e, Description description) {
      LOG.error("Test failed: " + description.getMethodName(), e);

      // Keep cluster logs on failure
      if (cluster != null) {
        try {
          DirectoryManager dirManager = cluster.getDirectoryManager();
          dirManager.setDeleteOnCleanup(false);
          LOG.error("Test failed. Cluster logs at: " +
              dirManager.getClusterRoot());
        } catch (Exception ex) {
          LOG.error("Failed to preserve cluster logs", ex);
        }
      }
    }
  };

  @Before
  public void baseSetUp() throws Exception {
    LOG.info("=== Starting test: {} ===", testName.getMethodName());

    // Kill any leftover launcher processes from previous runs
    killLeftoverProcesses();

    // Verify Hadoop distributions exist
    verifyDistributionsExist();

    // Create base configuration with defaults for normal operation
    // The cluster will extract and write only explicit properties to config files
    conf = new YarnConfiguration();
    configureYarn(conf);
  }

  /**
   * Kill any leftover launcher processes from previous test runs.
   */
  protected void killLeftoverProcesses() {
    try {
      LOG.info("Checking for leftover launcher processes...");

      // Use jps to find launcher processes
      ProcessBuilder pb = new ProcessBuilder("jps", "-l");
      Process p = pb.start();

      java.io.BufferedReader reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(p.getInputStream()));

      String line;
      java.util.List<String> pidsToKill = new java.util.ArrayList<>();

      while ((line = reader.readLine()) != null) {
        if (line.contains("ResourceManagerProcessLauncher") ||
            line.contains("NodeManagerProcessLauncher")) {
          String[] parts = line.split("\\s+");
          if (parts.length > 0) {
            pidsToKill.add(parts[0]);
            LOG.warn("Found leftover launcher process: {}", line);
          }
        }
      }

      p.waitFor();

      // Kill each leftover process
      for (String pid : pidsToKill) {
        try {
          LOG.info("Killing leftover process {}", pid);
          ProcessBuilder killPb = new ProcessBuilder("kill", "-9", pid);
          Process killProc = killPb.start();
          killProc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
          LOG.warn("Failed to kill process {}: {}", pid, e.getMessage());
        }
      }

      if (!pidsToKill.isEmpty()) {
        LOG.info("Killed {} leftover processes, waiting for cleanup...", pidsToKill.size());
        Thread.sleep(2000); // Give OS time to clean up
      }
    } catch (Exception e) {
      LOG.warn("Error checking for leftover processes: {}", e.getMessage());
    }
  }

  @After
  public void baseTearDown() throws Exception {
    LOG.info("=== Cleaning up test: {} ===", testName.getMethodName());

    // Stop YarnClient
    if (yarnClient != null) {
      try {
        yarnClient.stop();
      } catch (Exception e) {
        LOG.warn("Error stopping YarnClient", e);
      }
      yarnClient = null;
    }

    // Preserve logs for debugging (comment this out for cleanup)
    if (cluster != null) {
      try {
        DirectoryManager dirManager = cluster.getDirectoryManager();
        dirManager.setDeleteOnCleanup(false);
        LOG.info("Cluster logs preserved at: {}", dirManager.getClusterRoot());
      } catch (Exception e) {
        LOG.warn("Error preserving logs", e);
      }
    }

    // Shutdown cluster
    if (cluster != null) {
      try {
        cluster.shutdown();
      } catch (Exception e) {
        LOG.warn("Error shutting down cluster", e);
      }
      cluster = null;
    }

    // Kill any leftover launcher processes after shutdown
    killLeftoverProcesses();

    LOG.info("=== Test cleanup complete: {} ===", testName.getMethodName());
  }

  /**
   * Verify required Hadoop distributions exist.
   * Tests will be skipped if distributions are missing.
   */
  protected void verifyDistributionsExist() {
    File hadoop335 = new File(HADOOP_3_3_5);
    File hadoop340 = new File(HADOOP_3_4_0);

    if (!hadoop335.exists() || !hadoop340.exists()) {
      String message = String.format(
          "Required Hadoop distributions not found:\n" +
          "  - %s: %s\n" +
          "  - %s: %s\n" +
          "Please ensure distributions are installed at: %s",
          HADOOP_3_3_5, hadoop335.exists() ? "OK" : "MISSING",
          HADOOP_3_4_0, hadoop340.exists() ? "OK" : "MISSING",
          HADOOP_DIST_BASE);

      LOG.warn(message);
      Assume.assumeTrue(message, hadoop335.exists() && hadoop340.exists());
    }
  }

  /**
   * Configure YARN for test environment.
   */
  protected void configureYarn(YarnConfiguration config) {
    // Small memory limits for testing
    config.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    config.setInt(YarnConfiguration.RM_SCHEDULER_MAXIMUM_ALLOCATION_MB, 2048);
    config.setInt(YarnConfiguration.NM_PMEM_MB, 4096);
    config.setInt(YarnConfiguration.NM_VMEM_PMEM_RATIO, 3);

    // Fast heartbeats for quicker tests
    config.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 500);
    config.setLong(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 10000);

    // Disable ACLs for testing
    config.setBoolean(YarnConfiguration.YARN_ACL_ENABLE, false);

    // Fast application completion
    config.setInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS, 2);
  }

  /**
   * Wait for NodeManagers to register with ResourceManager.
   *
   * @param expectedCount Expected number of NodeManagers
   * @param timeoutMs Timeout in milliseconds
   * @throws Exception if timeout or error
   */
  protected void waitForNodeManagersToRegister(int expectedCount, long timeoutMs)
      throws Exception {
    LOG.info("Waiting for {} NodeManagers to register (timeout: {}ms)",
        expectedCount, timeoutMs);

    long startTime = System.currentTimeMillis();
    int registeredCount = 0;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        registeredCount = yarnClient.getYarnClusterMetrics().getNumNodeManagers();
        LOG.debug("Currently registered NodeManagers: {}", registeredCount);

        if (registeredCount >= expectedCount) {
          LOG.info("All {} NodeManagers registered successfully", expectedCount);
          return;
        }
      } catch (Exception e) {
        LOG.debug("Error checking NM count (will retry)", e);
      }

      Thread.sleep(1000);
    }

    throw new Exception(String.format(
        "Timeout waiting for NodeManagers to register. " +
        "Expected: %d, Registered: %d, Timeout: %dms",
        expectedCount, registeredCount, timeoutMs));
  }

  /**
   * Submit a simple sleep application for testing.
   *
   * @param sleepTimeMs Sleep time in milliseconds
   * @return ApplicationId
   * @throws Exception if submission fails
   */
  protected ApplicationId submitSleepApp(long sleepTimeMs) throws Exception {
    LOG.info("Submitting sleep application (sleep: {}ms)", sleepTimeMs);

    YarnClientApplication app = yarnClient.createApplication();
    ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
    ApplicationId appId = appContext.getApplicationId();

    // Set application name
    appContext.setApplicationName("SleepApp-" + System.currentTimeMillis());

    // Create container launch context
    ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
    amContainer.setCommands(Collections.singletonList(
        "sleep " + (sleepTimeMs / 1000) + " && echo 'Sleep complete'"));
    amContainer.setLocalResources(Collections.<String, LocalResource>emptyMap());
    amContainer.setEnvironment(Collections.<String, String>emptyMap());

    appContext.setAMContainerSpec(amContainer);

    // Set resource requirements
    Resource capability = Records.newRecord(Resource.class);
    capability.setMemory(128);
    capability.setVirtualCores(1);
    appContext.setResource(capability);

    // Set priority
    Priority priority = Records.newRecord(Priority.class);
    priority.setPriority(0);
    appContext.setPriority(priority);

    // Set queue
    appContext.setQueue("default");

    // Submit application
    yarnClient.submitApplication(appContext);
    LOG.info("Application submitted: {}", appId);

    return appId;
  }

  /**
   * Wait for application to reach a specific state.
   *
   * @param appId Application ID
   * @param expectedState Expected application state
   * @param timeoutMs Timeout in milliseconds
   * @throws Exception if timeout or error
   */
  protected void waitForAppState(ApplicationId appId,
                                 YarnApplicationState expectedState,
                                 long timeoutMs) throws Exception {
    LOG.info("Waiting for app {} to reach state {} (timeout: {}ms)",
        appId, expectedState, timeoutMs);

    long startTime = System.currentTimeMillis();
    YarnApplicationState currentState = null;

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      ApplicationReport report = yarnClient.getApplicationReport(appId);
      currentState = report.getYarnApplicationState();

      LOG.debug("App {} current state: {}", appId, currentState);

      if (currentState == expectedState) {
        LOG.info("App {} reached expected state: {}", appId, expectedState);
        return;
      }

      // Check for terminal failure states
      if (currentState == YarnApplicationState.FAILED ||
          currentState == YarnApplicationState.KILLED) {
        throw new Exception(String.format(
            "Application %s entered terminal failure state: %s (expected: %s)",
            appId, currentState, expectedState));
      }

      Thread.sleep(1000);
    }

    throw new Exception(String.format(
        "Timeout waiting for app %s to reach state %s. " +
        "Current state: %s, Timeout: %dms",
        appId, expectedState, currentState, timeoutMs));
  }

  /**
   * Wait for application to complete (reach FINISHED state).
   *
   * @param appId Application ID
   * @param timeoutMs Timeout in milliseconds
   * @throws Exception if timeout or error
   */
  protected void waitForAppCompletion(ApplicationId appId, long timeoutMs)
      throws Exception {
    waitForAppState(appId, YarnApplicationState.FINISHED, timeoutMs);
  }

  /**
   * Get cluster metrics summary for logging.
   */
  protected String getClusterMetricsSummary() {
    try {
      return String.format(
          "NMs: %d, Active: %d",
          yarnClient.getYarnClusterMetrics().getNumNodeManagers(),
          yarnClient.getYarnClusterMetrics().getNumActiveNodeManagers());
    } catch (Exception e) {
      return "Unable to get metrics: " + e.getMessage();
    }
  }

  /**
   * Check if a distribution exists.
   */
  protected boolean distributionExists(String path) {
    File dist = new File(path);
    return dist.exists() && new File(dist, "share/hadoop/yarn").exists();
  }
}

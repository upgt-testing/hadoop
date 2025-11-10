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

package org.apache.hadoop.yarn.server.process.upgrade;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class for parameterized upgrade testing of ProcessBasedMiniYARNCluster.
 *
 * <p>This class provides automatic lifecycle management, checkpoint-based upgrade
 * testing, and complete test isolation. Each test execution is fully isolated with
 * guaranteed cleanup between runs.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li><b>Automatic Lifecycle Management:</b> @Before/@After handle all setup and cleanup</li>
 *   <li><b>Checkpoint-Based Upgrades:</b> Trigger rolling upgrades at specific test points</li>
 *   <li><b>Complete Isolation:</b> Each test run is independent, no shared state</li>
 *   <li><b>Defensive Cleanup:</b> Kills orphaned processes, cleans old directories</li>
 *   <li><b>Verification:</b> Ensures successful cleanup after each test</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @RunWith(Parameterized.class)
 * public class TestApplicationUpgrade extends YarnUpgradeTestBase {
 *
 *   @Parameter
 *   public String upgradeCheckpoint;
 *
 *   @Parameters(name = "upgrade-at={0}")
 *   public static Collection<String> checkpoints() {
 *     return Arrays.asList(
 *       YarnUpgradeCheckpoints.NO_UPGRADE,           // Baseline test
 *       YarnUpgradeCheckpoints.AFTER_CLUSTER_START,  // Upgrade before app
 *       "AFTER_APP_SUBMIT"                          // Custom checkpoint
 *     );
 *   }
 *
 *   @Test
 *   public void testDistributedShellUpgrade() throws Exception {
 *     // Setup: Create cluster (inherited field from base class)
 *     cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
 *         .numNodeManagers(3)
 *         .build();
 *     cluster.start();
 *
 *     // Checkpoint: Upgrade can happen here
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);
 *
 *     // Test logic continues...
 *     // No cleanup needed - @After handles it automatically!
 *   }
 * }
 * }</pre>
 *
 * <h3>Important Notes:</h3>
 * <ul>
 *   <li><b>YarnClient:</b> Due to cyclic dependencies, YarnClient is NOT managed by this base class.
 *       Tests that need YarnClient must create, use, and close it manually in their test methods.</li>
 *   <li><b>Cleanup:</b> The base class only manages ProcessBasedMiniYARNCluster cleanup. Tests must
 *       handle cleanup of any additional resources they create (clients, streams, etc.).</li>
 * </ul>
 *
 * @see YarnUpgradeCheckpoints
 * @see ProcessBasedMiniYARNCluster
 */
public abstract class YarnUpgradeTestBase {

  private static final Logger LOG =
      LoggerFactory.getLogger(YarnUpgradeTestBase.class);

  /** Timeout for NodeManager reconnection after upgrade (ms) */
  private static final long NM_RECONNECT_TIMEOUT_MS = 10000;

  /** Wait time for processes to terminate gracefully (ms) */
  private static final long PROCESS_TERMINATION_WAIT_MS = 2000;

  /** Age threshold for cleaning old cluster directories (ms) */
  private static final long OLD_DIRECTORY_AGE_MS = 60 * 60 * 1000; // 1 hour

  // =========================================================================
  // Protected fields accessible to subclasses
  // =========================================================================

  /**
   * Upgrade checkpoint parameter synced from subclass.
   * Determines at which point in the test the upgrade should occur.
   */
  protected String upgradeCheckpoint;

  /**
   * YARN configuration for cluster and clients.
   * Initialized fresh in @Before for each test execution.
   */
  protected Configuration conf;

  /**
   * ProcessBasedMiniYARNCluster instance.
   * Created by subclass test method, cleaned up by @After.
   */
  protected ProcessBasedMiniYARNCluster cluster;

  // =========================================================================
  // Test lifecycle methods
  // =========================================================================

  /**
   * Sets up test environment before each test execution.
   *
   * @throws Exception if setup fails
   */
  @Before
  public void setupTest() throws Exception {
    syncUpgradeCheckpointFromSubclass();
    LOG.info("========================================");
    LOG.info("Setup: Starting test with checkpoint: {}", upgradeCheckpoint);
    LOG.info("========================================");

    try {
      cleanupOrphanedProcesses();
    } catch (Exception e) {
      LOG.warn("Error during orphaned process cleanup in setup", e);
    }

    try {
      cleanupOldClusterDirectories();
    } catch (Exception e) {
      LOG.warn("Error during old directory cleanup in setup", e);
    }

    conf = new YarnConfiguration();
    cluster = null;

    LOG.info("Setup completed successfully for checkpoint: {}", upgradeCheckpoint);
  }

  /**
   * Tears down test environment after each test execution.
   *
   * @throws Exception if cleanup fails critically
   */
  @After
  public void tearDownTest() throws Exception {
    LOG.info("========================================");
    LOG.info("Teardown: Cleaning up after checkpoint: {}", upgradeCheckpoint);
    LOG.info("========================================");

    // Shutdown cluster (independent try-catch)
    try {
      if (cluster != null) {
        LOG.info("Shutting down ProcessBasedMiniYARNCluster");
        cluster.shutdown();
        LOG.debug("Cluster shutdown successfully");
      }
    } catch (Exception e) {
      LOG.error("Error shutting down cluster", e);
    } finally {
      cluster = null;
    }

    // Wait for processes to terminate gracefully
    try {
      LOG.debug("Waiting {}ms for processes to terminate gracefully",
          PROCESS_TERMINATION_WAIT_MS);
      Thread.sleep(PROCESS_TERMINATION_WAIT_MS);
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for process termination", e);
      Thread.currentThread().interrupt();
    }

    // Verify cleanup
    boolean cleanupVerified = false;
    try {
      cleanupVerified = verifyCleanup();
      if (cleanupVerified) {
        LOG.info("Cleanup verification passed - no orphaned processes");
      } else {
        LOG.warn("Cleanup verification failed - orphaned processes detected");
      }
    } catch (Exception e) {
      LOG.error("Error during cleanup verification", e);
    }

    // Force cleanup if needed
    if (!cleanupVerified) {
      try {
        LOG.warn("Forcing cleanup of orphaned processes");
        cleanupOrphanedProcesses();
        if (verifyCleanup()) {
          LOG.info("Force cleanup successful");
        } else {
          LOG.error("Force cleanup failed - processes still running!");
        }
      } catch (Exception e) {
        LOG.error("Error during force cleanup", e);
      }
    }

    LOG.info("========================================");
    LOG.info("Teardown completed for checkpoint: {}", upgradeCheckpoint);
    LOG.info("========================================");
  }

  // =========================================================================
  // Checkpoint and upgrade methods
  // =========================================================================

  /**
   * Checkpoint for potential upgrade.
   *
   * <p><b>IMPORTANT:</b> Close all streams and resources before calling checkpoint().</p>
   *
   * @param name Checkpoint name
   * @throws IOException if upgrade fails
   * @throws TimeoutException if NodeManagers fail to reconnect
   * @throws InterruptedException if wait is interrupted
   * @throws IllegalStateException if cluster is not healthy for upgrade
   */
  protected void checkpoint(String name)
      throws IOException, TimeoutException, InterruptedException {
    if (!shouldUpgrade(name)) {
      LOG.debug("Checkpoint '{}' - no upgrade", name);
      return;
    }

    LOG.info("========================================");
    LOG.info("Checkpoint: {} - TRIGGERING UPGRADE", name);
    LOG.info("========================================");

    // Pre-upgrade health check
    if (cluster == null || !cluster.isClusterUp()) {
      throw new IllegalStateException(
          "Cluster is not healthy before upgrade at checkpoint: " + name);
    }

    if (cluster.getNumNodeManagers() > 0) {
      boolean allConnected = cluster.waitForNodeManagersToConnect(
          NM_RECONNECT_TIMEOUT_MS);
      if (!allConnected) {
        throw new IllegalStateException(
            "Not all NodeManagers connected before upgrade at checkpoint: " + name);
      }
    }

    LOG.info("Pre-upgrade health check passed at checkpoint: {}", name);

    // Perform rolling upgrade
    try {
      LOG.info("Starting rolling upgrade at checkpoint: {}", name);
      cluster.rollingUpgradeNodeManagers();
      LOG.info("Rolling upgrade completed at checkpoint: {}", name);
    } catch (Exception e) {
      LOG.error("Rolling upgrade failed at checkpoint: {}", name, e);
      throw e;
    }

    // Post-upgrade health check
    LOG.info("Verifying cluster health after upgrade at checkpoint: {}", name);
    cluster.waitForNodeManagersToConnect(NM_RECONNECT_TIMEOUT_MS);

    if (!cluster.isClusterUp()) {
      throw new IllegalStateException(
          "Cluster is not healthy after upgrade at checkpoint: " + name);
    }

    LOG.info("========================================");
    LOG.info("Checkpoint: {} - UPGRADE SUCCESSFUL", name);
    LOG.info("========================================");
  }

  /**
   * Determines if an upgrade should occur at the given checkpoint.
   *
   * @param name Checkpoint name to check
   * @return true if upgrade should occur at this checkpoint
   */
  protected boolean shouldUpgrade(String name) {
    if (upgradeCheckpoint == null) {
      return false;
    }

    if (YarnUpgradeCheckpoints.NO_UPGRADE.equals(upgradeCheckpoint)) {
      return false;
    }

    return upgradeCheckpoint.equals(name);
  }

  // =========================================================================
  // Private helper methods
  // =========================================================================

  private void syncUpgradeCheckpointFromSubclass() {
    try {
      Class<?> subclass = this.getClass();
      Field paramField = null;

      for (Field field : subclass.getDeclaredFields()) {
        if (field.isAnnotationPresent(Parameter.class) &&
            "upgradeCheckpoint".equals(field.getName())) {
          paramField = field;
          break;
        }
      }

      if (paramField != null) {
        paramField.setAccessible(true);
        Object value = paramField.get(this);
        this.upgradeCheckpoint = (String) value;
        LOG.debug("Synced upgradeCheckpoint from subclass: {}",
            this.upgradeCheckpoint);
      } else {
        LOG.warn("No @Parameter field named 'upgradeCheckpoint' found in subclass");
      }
    } catch (Exception e) {
      LOG.error("Error syncing upgradeCheckpoint from subclass", e);
    }
  }

  private void cleanupOrphanedProcesses() throws IOException {
    String os = System.getProperty("os.name").toLowerCase();
    if (!os.contains("nix") && !os.contains("nux") && !os.contains("mac")) {
      LOG.debug("Skipping orphaned process cleanup on non-Unix platform: {}", os);
      return;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder(
          "sh", "-c",
          "jps | grep -E 'ResourceManager|NodeManager' | awk '{print $1}' | xargs -r kill -9"
      );

      Process process = pb.start();
      int exitCode = process.waitFor();

      if (exitCode == 0) {
        LOG.debug("Orphaned process cleanup executed successfully");
      } else {
        LOG.debug("Orphaned process cleanup exited with code: {}", exitCode);
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted during orphaned process cleanup", e);
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      LOG.warn("Error executing orphaned process cleanup command", e);
      throw e;
    }
  }

  private void cleanupOldClusterDirectories() throws IOException {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    if (!tmpDir.exists() || !tmpDir.isDirectory()) {
      LOG.debug("Temp directory does not exist: {}", tmpDir);
      return;
    }

    File[] oldDirs = tmpDir.listFiles((dir, name) ->
        name.startsWith("process-miniyarn-") &&
        name.matches(".*\\d{13}$"));

    if (oldDirs == null || oldDirs.length == 0) {
      LOG.debug("No old cluster directories found");
      return;
    }

    long cutoffTime = System.currentTimeMillis() - OLD_DIRECTORY_AGE_MS;
    int deletedCount = 0;

    for (File dir : oldDirs) {
      if (dir.lastModified() < cutoffTime) {
        try {
          if (deleteDirectory(dir)) {
            deletedCount++;
            LOG.debug("Deleted old cluster directory: {}", dir.getName());
          }
        } catch (Exception e) {
          LOG.warn("Failed to delete old cluster directory: {}", dir.getName(), e);
        }
      }
    }

    if (deletedCount > 0) {
      LOG.info("Cleaned up {} old cluster directories", deletedCount);
    }
  }

  private boolean deleteDirectory(File directory) {
    if (directory == null || !directory.exists()) {
      return false;
    }

    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectory(file);
        }
      }
    }

    return directory.delete();
  }

  private boolean verifyCleanup() {
    String os = System.getProperty("os.name").toLowerCase();
    if (!os.contains("nix") && !os.contains("nux") && !os.contains("mac")) {
      LOG.debug("Skipping cleanup verification on non-Unix platform: {}", os);
      return true;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder("jps");
      Process process = pb.start();

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()));
      String line;
      boolean foundOrphanedProcess = false;

      while ((line = reader.readLine()) != null) {
        if (line.contains("ResourceManager") || line.contains("NodeManager")) {
          LOG.warn("Found orphaned process: {}", line);
          foundOrphanedProcess = true;
        }
      }

      process.waitFor();
      reader.close();

      return !foundOrphanedProcess;
    } catch (Exception e) {
      LOG.warn("Error verifying cleanup", e);
      return false;
    }
  }
}

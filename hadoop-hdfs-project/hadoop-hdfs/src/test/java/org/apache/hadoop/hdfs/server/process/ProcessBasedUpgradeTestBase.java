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
package org.apache.hadoop.hdfs.server.process;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Base class for ProcessBased upgrade tests with parameterized checkpoint support.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Automatic cleanup via @Before and @After methods</li>
 *   <li>Defensive pre-cleanup of orphaned processes</li>
 *   <li>Cleanup verification after each test</li>
 *   <li>Checkpoint-based upgrade testing support</li>
 *   <li>Complete test isolation for parameterized tests</li>
 * </ul>
 *
 * <p>Usage in parameterized tests:
 * <pre>
 * {@code
 * @RunWith(Parameterized.class)
 * public class MyTest extends ProcessBasedUpgradeTestBase {
 *
 *   @Parameter
 *   public String upgradeCheckpoint;
 *
 *   @Parameters(name = "upgrade-at={0}")
 *   public static Collection<String> checkpoints() {
 *     return Arrays.asList("NO_UPGRADE", "AFTER_CREATE", ...);
 *   }
 *
 *   @Test
 *   public void testSomething() throws Exception {
 *     cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
 *     fs = cluster.getFileSystem();
 *
 *     // Insert checkpoints
 *     fs.create(new Path("/file")).close();
 *     checkpoint("AFTER_CREATE");
 *
 *     // No finally block needed - @After handles cleanup!
 *   }
 * }
 * }</pre>
 *
 * <p><b>Test Isolation:</b> Each parameter execution is completely isolated:
 * <ol>
 *   <li>@Before kills orphaned processes and cleans old directories</li>
 *   <li>Test runs with specific upgrade checkpoint</li>
 *   <li>@After guarantees cleanup (closes fs, shuts down cluster, verifies)</li>
 * </ol>
 *
 * <p><b>Cleanup Guarantee:</b> Uses multiple independent try-catch blocks in @After
 * to ensure each cleanup step runs even if previous steps fail.
 */
public abstract class ProcessBasedUpgradeTestBase {

  protected static final Logger LOG =
      LoggerFactory.getLogger(ProcessBasedUpgradeTestBase.class);

  /**
   * The upgrade checkpoint for this test execution.
   * Subclasses should declare their own @Parameter field and this will be synced in @Before.
   * Value "NO_UPGRADE" means no upgrade is performed (baseline test).
   */
  protected String upgradeCheckpoint;

  /**
   * The ProcessBasedMiniDFSCluster instance for this test.
   * Automatically cleaned up in @After method.
   */
  protected ProcessBasedMiniDFSCluster cluster;

  /**
   * The DistributedFileSystem instance for this test.
   * Automatically closed in @After method.
   */
  protected DistributedFileSystem fs;

  /**
   * Configuration for this test.
   * Initialized fresh in @Before method.
   */
  protected Configuration conf;

  /**
   * Setup executed before each test execution.
   *
   * <p>For parameterized tests, this runs once per parameter value.
   * Performs defensive cleanup of orphaned processes and directories
   * from previous test runs.
   *
   * @throws Exception if setup fails
   */
  @Before
  public void setupTest() throws Exception {
    // Sync @Parameter field from subclass to base class field
    // JUnit injects into subclass @Parameter field, we need to copy it here
    syncUpgradeCheckpointFromSubclass();

    LOG.info("===== Setting up test (checkpoint: {}) =====", upgradeCheckpoint);

    // 1. Force kill any orphaned processes from previous tests
    cleanupOrphanedProcesses();

    // 2. Clean up old cluster directories
    cleanupOldClusterDirectories();

    // 3. Initialize configuration
    conf = new HdfsConfiguration();

    LOG.info("===== Setup complete =====");
  }

  /**
   * Sync the @Parameter upgradeCheckpoint field from subclass to base class.
   * JUnit's Parameterized runner injects values into the subclass field,
   * so we need to copy it to the base class field for proper access.
   */
  private void syncUpgradeCheckpointFromSubclass() {
    try {
      // Look for @Parameter field in the actual test class
      Class<?> testClass = this.getClass();
      Field subclassField = testClass.getDeclaredField("upgradeCheckpoint");

      if (subclassField.isAnnotationPresent(Parameter.class)) {
        subclassField.setAccessible(true);
        Object value = subclassField.get(this);

        // Set the base class field
        Field baseField = ProcessBasedUpgradeTestBase.class
            .getDeclaredField("upgradeCheckpoint");
        baseField.setAccessible(true);
        baseField.set(this, value);

        LOG.debug("Synced upgradeCheckpoint from subclass: {}", value);
      }
    } catch (NoSuchFieldException e) {
      // Subclass doesn't have its own field, that's okay
      LOG.debug("No @Parameter upgradeCheckpoint field in subclass, using base class field");
    } catch (Exception e) {
      LOG.warn("Failed to sync upgradeCheckpoint from subclass", e);
    }
  }

  /**
   * Cleanup executed after each test execution.
   *
   * <p>For parameterized tests, this runs once per parameter value.
   * Guarantees cleanup even if test fails. Each cleanup step is in a
   * separate try-catch to ensure all steps execute.
   */
  @After
  public void tearDownTest() {
    LOG.info("===== Tearing down test (checkpoint: {}) =====", upgradeCheckpoint);

    // Step 1: Close FileSystem
    if (fs != null) {
      try {
        fs.close();
        LOG.info("FileSystem closed successfully");
      } catch (Exception e) {
        LOG.error("Failed to close FileSystem", e);
      } finally {
        fs = null;
      }
    }

    // Step 2: Shutdown cluster with directory deletion
    if (cluster != null) {
      try {
        cluster.shutdown(true); // DELETE directories
        LOG.info("Cluster shutdown successfully");
      } catch (Exception e) {
        LOG.error("Failed to shutdown cluster", e);
      } finally {
        cluster = null;
      }
    }

    // Step 3: Wait for processes to die
    try {
      Thread.sleep(1000); // Give processes time to terminate
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Step 4: Verify cleanup
    try {
      verifyCleanup();
      LOG.info("Cleanup verification passed");
    } catch (Exception e) {
      LOG.error("Cleanup verification failed - forcing cleanup", e);
      // Step 5: Force cleanup if verification failed
      try {
        cleanupOrphanedProcesses();
      } catch (Exception ex) {
        LOG.error("Force cleanup failed", ex);
      }
    }

    LOG.info("===== Teardown complete =====");
  }

  /**
   * Insert an upgrade checkpoint in the test.
   *
   * <p>If the current upgradeCheckpoint parameter matches the checkpoint name,
   * this method performs a rolling upgrade. Otherwise, it does nothing.
   *
   * <p><b>Stream Management:</b> Callers should close any open streams
   * (FSDataOutputStream, FSDataInputStream) before calling this method,
   * as the upgrade will restart DataNodes and break pipelines.
   *
   * <p>Example:
   * <pre>{@code
   * FSDataOutputStream out = fs.create(file);
   * out.write(data);
   * out.close();  // IMPORTANT: Close before checkpoint
   * checkpoint("AFTER_WRITE");
   * out = fs.append(file);  // Reopen after upgrade if needed
   * }</pre>
   *
   * @param name the checkpoint name (should match a value in @Parameters)
   * @throws Exception if upgrade fails
   */
  protected void checkpoint(String name) throws Exception {
    if (shouldUpgrade(name)) {
      LOG.info("=== UPGRADE CHECKPOINT: {} ===", name);

      // Verify cluster is healthy before upgrade
      if (cluster != null && !cluster.isClusterUp()) {
        throw new IllegalStateException(
            "Cannot upgrade at checkpoint '" + name +
            "': cluster is not healthy");
      }

      // Perform upgrade
      cluster.upgrade();

      LOG.info("=== UPGRADE COMPLETED at {} ===", name);

      // Verify cluster is healthy after upgrade
      if (cluster != null) {
        cluster.waitClusterUp();
      }
    }
  }

  /**
   * Check if upgrade should be performed at the given checkpoint.
   *
   * @param name the checkpoint name
   * @return true if upgrade should happen at this checkpoint
   */
  protected boolean shouldUpgrade(String name) {
    return upgradeCheckpoint != null
        && !upgradeCheckpoint.equals(UpgradeCheckpoints.NO_UPGRADE)
        && upgradeCheckpoint.equals(name);
  }

  /**
   * Force kill any orphaned NameNode or DataNode processes.
   *
   * <p>Uses jps to find Java processes and kills any NameNode or DataNode
   * processes. This is a defensive measure to ensure clean state before
   * starting a new cluster.
   */
  private void cleanupOrphanedProcesses() {
    try {
      ProcessBuilder pb = new ProcessBuilder(
          "bash", "-c",
          "jps | grep -E 'NameNode|DataNode' | awk '{print $1}' | xargs -r kill -9"
      );
      Process p = pb.start();
      boolean finished = p.waitFor(5, TimeUnit.SECONDS);

      if (!finished) {
        p.destroyForcibly();
        LOG.warn("Process cleanup command timed out");
      }

      // Give OS time to clean up
      Thread.sleep(2000);
      LOG.info("Orphaned process cleanup completed");
    } catch (Exception e) {
      LOG.warn("Failed to cleanup orphaned processes (may not be critical)", e);
    }
  }

  /**
   * Clean up old cluster directories in /tmp.
   *
   * <p>Deletes process-minicluster-* directories that are older than 1 hour.
   * This prevents disk space exhaustion from accumulated test directories.
   */
  private void cleanupOldClusterDirectories() {
    try {
      String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
      File tmpDirFile = new File(tmpDir);
      File[] oldClusters = tmpDirFile.listFiles((dir, name) ->
          name.startsWith("process-minicluster-"));

      if (oldClusters != null) {
        long now = System.currentTimeMillis();
        int deleted = 0;

        for (File oldCluster : oldClusters) {
          // Delete directories older than 1 hour (safety margin)
          long age = now - oldCluster.lastModified();
          if (age > 3600000) { // 1 hour in milliseconds
            if (deleteDirectory(oldCluster)) {
              deleted++;
            }
          }
        }

        if (deleted > 0) {
          LOG.info("Cleaned up {} old cluster directories", deleted);
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to cleanup old directories (may not be critical)", e);
    }
  }

  /**
   * Recursively delete a directory.
   *
   * @param dir the directory to delete
   * @return true if deletion succeeded
   */
  private boolean deleteDirectory(File dir) {
    if (dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectory(file);
        }
      }
    }
    return dir.delete();
  }

  /**
   * Verify that no NameNode or DataNode processes remain after cleanup.
   *
   * <p>This is a verification step to ensure cleanup was successful.
   * If orphaned processes are found, this method throws an exception
   * to alert that cleanup failed.
   *
   * @throws Exception if orphaned processes are found
   */
  private void verifyCleanup() throws Exception {
    ProcessBuilder pb = new ProcessBuilder("jps");
    Process p = pb.start();

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(p.getInputStream()));

    String line;
    List<String> orphans = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      if (line.contains("NameNode") || line.contains("DataNode")) {
        orphans.add(line);
      }
    }

    p.waitFor(5, TimeUnit.SECONDS);

    if (!orphans.isEmpty()) {
      String errorMsg = "Found orphaned processes after cleanup: " + orphans;
      LOG.error(errorMsg);
      throw new AssertionError(errorMsg);
    }
  }
}

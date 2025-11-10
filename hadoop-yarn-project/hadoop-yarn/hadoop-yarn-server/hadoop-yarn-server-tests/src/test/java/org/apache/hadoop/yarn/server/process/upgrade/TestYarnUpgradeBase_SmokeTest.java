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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smoke test for YarnUpgradeTestBase functionality.
 *
 * <p>This test validates that the YarnUpgradeTestBase infrastructure works
 * correctly by performing a simple cluster lifecycle test with checkpoints.</p>
 *
 * <p><b>Purpose:</b></p>
 * <ul>
 *   <li>Verify YarnUpgradeTestBase lifecycle management (@Before/@After)</li>
 *   <li>Validate checkpoint mechanism works correctly</li>
 *   <li>Ensure NO_UPGRADE baseline test passes</li>
 *   <li>Test rolling upgrade at different checkpoints (if configured)</li>
 *   <li>Verify cleanup is successful (no orphaned processes)</li>
 * </ul>
 *
 * <p><b>Test Scenarios:</b></p>
 * <ul>
 *   <li><b>NO_UPGRADE:</b> Baseline test - cluster starts and stops, no upgrade</li>
 *   <li><b>AFTER_CLUSTER_START:</b> Upgrade immediately after cluster starts</li>
 *   <li><b>AFTER_HEALTH_CHECK:</b> Upgrade after health check</li>
 * </ul>
 *
 * <p><b>Running the Test:</b></p>
 * <pre>
 * # Baseline test only (no upgrade)
 * mvn test -Dtest='TestYarnUpgradeBase_SmokeTest#testBasicLifecycle[upgrade-at=NO_UPGRADE]'
 *
 * # All checkpoints with upgrade distributions
 * mvn test -Dtest=TestYarnUpgradeBase_SmokeTest \
 *   -Dhadoop.start.home=/opt/hadoop-3.3.6 \
 *   -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
 * </pre>
 *
 * <p><b>Note:</b> This is a simple smoke test with no YarnClient usage to avoid
 * cyclic dependencies. For tests that need YarnClient, those tests should be in
 * a different module or manage YarnClient lifecycle themselves.</p>
 *
 * @see YarnUpgradeTestBase
 * @see YarnUpgradeCheckpoints
 */
@RunWith(Parameterized.class)
public class TestYarnUpgradeBase_SmokeTest extends YarnUpgradeTestBase {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestYarnUpgradeBase_SmokeTest.class);

  /** Number of NodeManagers to start in test cluster */
  private static final int NUM_NODE_MANAGERS = 2;

  /**
   * Upgrade checkpoint parameter.
   */
  @Parameter
  public String upgradeCheckpoint;

  /**
   * Test parameters defining checkpoint scenarios.
   *
   * @return Collection of checkpoint names to test
   */
  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        YarnUpgradeCheckpoints.NO_UPGRADE,           // Baseline - no upgrade
        YarnUpgradeCheckpoints.AFTER_CLUSTER_START,  // Upgrade after start
        "AFTER_HEALTH_CHECK"                         // Custom checkpoint
    );
  }

  /**
   * Tests basic cluster lifecycle with checkpoints.
   *
   * <p>This test performs the following operations:</p>
   * <ol>
   *   <li>Create ProcessBasedMiniYARNCluster with 2 NodeManagers</li>
   *   <li>Start cluster</li>
   *   <li><b>Checkpoint:</b> AFTER_CLUSTER_START (upgrade may happen here)</li>
   *   <li>Verify cluster health</li>
   *   <li><b>Checkpoint:</b> AFTER_HEALTH_CHECK (upgrade may happen here)</li>
   *   <li>Verify cluster still operational</li>
   *   <li>Automatic cleanup via @After (no try-finally needed!)</li>
   * </ol>
   *
   * @throws Exception if test fails
   */
  @Test
  public void testBasicLifecycle() throws Exception {
    LOG.info("========================================");
    LOG.info("Starting smoke test with checkpoint: {}", upgradeCheckpoint);
    LOG.info("========================================");

    // Step 1: Create cluster
    LOG.info("Creating ProcessBasedMiniYARNCluster with {} NodeManagers",
        NUM_NODE_MANAGERS);

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(NUM_NODE_MANAGERS)
        .build();

    assertNotNull("Cluster should be created", cluster);
    LOG.info("Cluster created successfully");

    // Step 2: Start cluster
    LOG.info("Starting cluster");
    cluster.start();
    assertTrue("Cluster should be up", cluster.isClusterUp());
    LOG.info("Cluster started successfully");

    // Wait for NodeManagers to connect
    LOG.info("Waiting for {} NodeManagers to connect", NUM_NODE_MANAGERS);
    cluster.waitForNodeManagersToConnect(10000);

    // Checkpoint: AFTER_CLUSTER_START
    LOG.info("=== Checkpoint: AFTER_CLUSTER_START ===");
    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    // Step 3: Perform health check
    LOG.info("Performing health check");
    assertTrue("Cluster should still be up after checkpoint",
        cluster.isClusterUp());

    assertEquals("Number of NodeManagers in cluster",
        NUM_NODE_MANAGERS, cluster.getNumNodeManagers());
    LOG.info("Health check passed");

    // Checkpoint: AFTER_HEALTH_CHECK (custom checkpoint)
    LOG.info("=== Checkpoint: AFTER_HEALTH_CHECK ===");
    checkpoint("AFTER_HEALTH_CHECK");

    // Step 4: Final verification
    LOG.info("Performing final cluster health check");
    assertTrue("Cluster should still be up at end of test",
        cluster.isClusterUp());

    LOG.info("========================================");
    LOG.info("Smoke test completed successfully for checkpoint: {}", upgradeCheckpoint);
    LOG.info("========================================");

    // No cleanup needed here - @After tearDownTest() handles everything!
  }

  /**
   * Tests that multiple checkpoint calls with non-matching names are no-ops.
   *
   * @throws Exception if test fails
   */
  @Test
  public void testMultipleCheckpointsWithNoUpgrade() throws Exception {
    LOG.info("Testing multiple checkpoints");

    // Create minimal cluster
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(1)
        .build();

    cluster.start();

    // Call multiple checkpoints - none should trigger upgrade
    // (unless upgradeCheckpoint parameter matches one of these)
    checkpoint("CHECKPOINT_ONE");
    checkpoint("CHECKPOINT_TWO");
    checkpoint("CHECKPOINT_THREE");

    // Verify cluster still healthy
    assertTrue("Cluster should be up after checkpoints",
        cluster.isClusterUp());

    LOG.info("Multiple checkpoint test completed successfully");
  }
}

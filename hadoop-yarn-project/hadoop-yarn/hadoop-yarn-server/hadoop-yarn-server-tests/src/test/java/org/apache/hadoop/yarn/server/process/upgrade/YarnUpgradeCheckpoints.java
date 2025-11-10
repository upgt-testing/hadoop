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

/**
 * Common checkpoint names for YARN upgrade testing.
 *
 * <p>These constants define common points in YARN application lifecycle where
 * rolling upgrades can be tested. Tests should always include {@link #NO_UPGRADE}
 * as the first checkpoint to establish a baseline that verifies the test passes
 * without any upgrade.</p>
 *
 * <p><b>Usage in Parameterized Tests:</b></p>
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
 *       YarnUpgradeCheckpoints.NO_UPGRADE,           // Always include baseline
 *       YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
 *       YarnUpgradeCheckpoints.AFTER_APP_SUBMIT,
 *       YarnUpgradeCheckpoints.AFTER_APP_RUNNING,
 *       YarnUpgradeCheckpoints.AFTER_APP_FINISHED
 *     );
 *   }
 *
 *   @Test
 *   public void testDistributedShellUpgrade() throws Exception {
 *     cluster = new ProcessBasedMiniYARNCluster.Builder(conf).build();
 *     yarnClient = YarnClient.createYarnClient();
 *     yarnClient.init(cluster.getConfiguration());
 *     yarnClient.start();
 *
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);
 *
 *     ApplicationId appId = submitApp(yarnClient);
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_APP_SUBMIT);
 *
 *     waitForAppState(yarnClient, appId, YarnApplicationState.RUNNING);
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_APP_RUNNING);
 *
 *     waitForAppCompletion(yarnClient, appId);
 *     checkpoint(YarnUpgradeCheckpoints.AFTER_APP_FINISHED);
 *
 *     // Verify application succeeded
 *     ApplicationReport report = yarnClient.getApplicationReport(appId);
 *     assertEquals(FinalApplicationStatus.SUCCEEDED,
 *         report.getFinalApplicationStatus());
 *   }
 * }
 * }</pre>
 *
 * <p><b>Checkpoint Guidelines:</b></p>
 * <ul>
 *   <li><b>NO_UPGRADE</b> - Always include this to verify test correctness</li>
 *   <li><b>AFTER_CLUSTER_START</b> - Test upgrade before any application activity</li>
 *   <li><b>AFTER_APP_SUBMIT</b> - Test upgrade during application queuing</li>
 *   <li><b>AFTER_APP_RUNNING</b> - Test upgrade while containers are running</li>
 *   <li><b>AFTER_APP_FINISHED</b> - Test upgrade after application completion</li>
 *   <li><b>Custom checkpoints</b> - Define additional checkpoints specific to your test</li>
 * </ul>
 *
 * @see YarnUpgradeTestBase
 */
public final class YarnUpgradeCheckpoints {

  /**
   * Baseline checkpoint - no upgrade is performed.
   *
   * <p>This checkpoint verifies that the test passes without any upgrade.
   * It establishes a baseline for comparison with upgrade scenarios and
   * helps identify test failures unrelated to upgrades.</p>
   *
   * <p><b>Always include this checkpoint in your test parameters.</b></p>
   */
  public static final String NO_UPGRADE = "NO_UPGRADE";

  /**
   * Checkpoint after cluster has started and all NodeManagers are connected.
   *
   * <p>Tests upgrade before any YARN applications are submitted.
   * This verifies that:</p>
   * <ul>
   *   <li>Rolling upgrade works on idle cluster</li>
   *   <li>NodeManagers reconnect successfully after upgrade</li>
   *   <li>Cluster remains operational for new applications</li>
   * </ul>
   */
  public static final String AFTER_CLUSTER_START = "AFTER_CLUSTER_START";

  /**
   * Checkpoint after application has been submitted to ResourceManager.
   *
   * <p>Tests upgrade while application is queued but not yet running.
   * This verifies that:</p>
   * <ul>
   *   <li>Queued applications survive NodeManager restarts</li>
   *   <li>Application scheduling continues after upgrade</li>
   *   <li>Pending applications are allocated containers post-upgrade</li>
   * </ul>
   */
  public static final String AFTER_APP_SUBMIT = "AFTER_APP_SUBMIT";

  /**
   * Checkpoint after application is in RUNNING state with containers allocated.
   *
   * <p>Tests upgrade while application containers are actively executing.
   * This verifies that:</p>
   * <ul>
   *   <li>Running containers are handled during NodeManager restart</li>
   *   <li>ApplicationMaster survives or is restarted appropriately</li>
   *   <li>Application can continue execution after upgrade</li>
   *   <li>Container work-preserving restart works (if enabled)</li>
   * </ul>
   *
   * <p><b>Important:</b> Close any application log streams before this
   * checkpoint to avoid broken pipe errors during NodeManager restarts.</p>
   */
  public static final String AFTER_APP_RUNNING = "AFTER_APP_RUNNING";

  /**
   * Checkpoint after application has finished (SUCCEEDED, FAILED, or KILLED).
   *
   * <p>Tests upgrade after application completion.
   * This verifies that:</p>
   * <ul>
   *   <li>Application history is preserved during upgrade</li>
   *   <li>Completed application metadata remains accessible</li>
   *   <li>Logs and diagnostics survive NodeManager restarts</li>
   * </ul>
   */
  public static final String AFTER_APP_FINISHED = "AFTER_APP_FINISHED";

  /**
   * Checkpoint after queue configuration has been refreshed.
   *
   * <p>Tests upgrade after dynamic queue configuration changes.
   * This verifies that:</p>
   * <ul>
   *   <li>Queue configurations survive NodeManager restarts</li>
   *   <li>ResourceManager maintains queue state during upgrade</li>
   *   <li>Applications in different queues continue correctly</li>
   * </ul>
   */
  public static final String AFTER_QUEUE_REFRESH = "AFTER_QUEUE_REFRESH";

  /**
   * Checkpoint after retrieving node reports from ResourceManager.
   *
   * <p>Tests upgrade after verifying NodeManager registration.
   * This verifies that:</p>
   * <ul>
   *   <li>Node reports remain consistent after upgrade</li>
   *   <li>NodeManager health and metrics are preserved</li>
   *   <li>Resource capacity calculations remain correct</li>
   * </ul>
   */
  public static final String AFTER_NODE_REPORT = "AFTER_NODE_REPORT";

  /**
   * Checkpoint after containers have been allocated to an application.
   *
   * <p>Tests upgrade after container allocation but before execution.
   * This verifies that:</p>
   * <ul>
   *   <li>Allocated containers survive NodeManager restarts</li>
   *   <li>Container tokens remain valid after upgrade</li>
   *   <li>ApplicationMaster can launch containers post-upgrade</li>
   * </ul>
   */
  public static final String AFTER_CONTAINER_ALLOCATION = "AFTER_CONTAINER_ALLOCATION";

  /**
   * Private constructor to prevent instantiation.
   * This is a utility class with only static constants.
   */
  private YarnUpgradeCheckpoints() {
    // Utility class - no instantiation
  }
}

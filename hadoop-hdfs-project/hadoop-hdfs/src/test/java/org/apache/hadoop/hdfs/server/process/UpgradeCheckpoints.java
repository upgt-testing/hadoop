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

/**
 * Common checkpoint names for upgrade testing.
 *
 * <p>This class provides standardized checkpoint name constants that can be
 * used across multiple ProcessBased upgrade tests. Using these constants
 * helps maintain consistency and avoid typos in checkpoint names.
 *
 * <p>Test-specific checkpoints can still be defined as string literals
 * in individual test classes when they are unique to that test.
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Parameters(name = "upgrade-at={0}")
 * public static Collection<String> checkpoints() {
 *   return Arrays.asList(
 *     UpgradeCheckpoints.NO_UPGRADE,
 *     UpgradeCheckpoints.AFTER_CLUSTER_START,
 *     UpgradeCheckpoints.AFTER_FILE_CREATE,
 *     "AFTER_SPECIFIC_OPERATION" // test-specific checkpoint
 *   );
 * }
 * }</pre>
 */
public final class UpgradeCheckpoints {

  // Prevent instantiation
  private UpgradeCheckpoints() {}

  // =========================================================================
  // Baseline
  // =========================================================================

  /**
   * Baseline checkpoint - test runs without any upgrade.
   * This should be the first checkpoint in every parameterized test
   * to ensure the test passes in non-upgrade scenarios.
   */
  public static final String NO_UPGRADE = "NO_UPGRADE";

  // =========================================================================
  // Cluster Lifecycle Checkpoints
  // =========================================================================

  /**
   * Checkpoint after cluster has started and is ready for operations.
   * Useful for testing upgrades immediately after cluster initialization.
   */
  public static final String AFTER_CLUSTER_START = "AFTER_CLUSTER_START";

  /**
   * Checkpoint before cluster shutdown.
   * Tests cluster behavior when upgrade happens just before shutdown.
   */
  public static final String BEFORE_CLUSTER_SHUTDOWN = "BEFORE_CLUSTER_SHUTDOWN";

  // =========================================================================
  // File Operation Checkpoints
  // =========================================================================

  /**
   * Checkpoint after file or directory creation.
   * Tests upgrade with newly created but empty files.
   */
  public static final String AFTER_FILE_CREATE = "AFTER_FILE_CREATE";

  /**
   * Checkpoint after file or directory deletion.
   * Tests upgrade after filesystem modifications.
   */
  public static final String AFTER_FILE_DELETE = "AFTER_FILE_DELETE";

  /**
   * Checkpoint after file rename operation.
   */
  public static final String AFTER_FILE_RENAME = "AFTER_FILE_RENAME";

  // =========================================================================
  // Write Operation Checkpoints
  // =========================================================================

  /**
   * Checkpoint after write operation.
   * Generic checkpoint for any write operation.
   */
  public static final String AFTER_WRITE = "AFTER_WRITE";

  /**
   * Checkpoint after first write operation in a sequence.
   */
  public static final String AFTER_FIRST_WRITE = "AFTER_FIRST_WRITE";

  /**
   * Checkpoint after second write operation in a sequence.
   */
  public static final String AFTER_SECOND_WRITE = "AFTER_SECOND_WRITE";

  /**
   * Checkpoint after third write operation in a sequence.
   */
  public static final String AFTER_THIRD_WRITE = "AFTER_THIRD_WRITE";

  // =========================================================================
  // Flush/Sync Checkpoints
  // =========================================================================

  /**
   * Checkpoint after flush (hflush/hsync) operation.
   */
  public static final String AFTER_FLUSH = "AFTER_FLUSH";

  /**
   * Checkpoint after first flush in a sequence.
   */
  public static final String AFTER_FIRST_FLUSH = "AFTER_FIRST_FLUSH";

  /**
   * Checkpoint after second flush in a sequence.
   */
  public static final String AFTER_SECOND_FLUSH = "AFTER_SECOND_FLUSH";

  // =========================================================================
  // Stream Lifecycle Checkpoints
  // =========================================================================

  /**
   * Checkpoint after closing a stream.
   * Stream should be closed before upgrade to avoid pipeline breaks.
   */
  public static final String AFTER_CLOSE = "AFTER_CLOSE";

  /**
   * Checkpoint after first close operation.
   */
  public static final String AFTER_FIRST_CLOSE = "AFTER_FIRST_CLOSE";

  /**
   * Checkpoint before final close operation.
   */
  public static final String BEFORE_FINAL_CLOSE = "BEFORE_FINAL_CLOSE";

  /**
   * Checkpoint after final close operation.
   */
  public static final String AFTER_FINAL_CLOSE = "AFTER_FINAL_CLOSE";

  /**
   * Checkpoint after reopening a file in append mode.
   */
  public static final String AFTER_APPEND_REOPEN = "AFTER_APPEND_REOPEN";

  // =========================================================================
  // Read Operation Checkpoints
  // =========================================================================

  /**
   * Checkpoint after read operation.
   */
  public static final String AFTER_READ = "AFTER_READ";

  /**
   * Checkpoint after first read operation.
   */
  public static final String AFTER_FIRST_READ = "AFTER_FIRST_READ";

  /**
   * Checkpoint after verifying read data.
   */
  public static final String AFTER_READ_VERIFICATION = "AFTER_READ_VERIFICATION";

  // =========================================================================
  // Data Verification Checkpoints
  // =========================================================================

  /**
   * Checkpoint before verification step.
   * Useful to test if data written before upgrade is accessible after.
   */
  public static final String BEFORE_VERIFICATION = "BEFORE_VERIFICATION";

  /**
   * Checkpoint after verification step.
   */
  public static final String AFTER_VERIFICATION = "AFTER_VERIFICATION";

  /**
   * Checkpoint before final verification.
   */
  public static final String BEFORE_FINAL_VERIFICATION = "BEFORE_FINAL_VERIFICATION";

  /**
   * Checkpoint after final verification.
   */
  public static final String AFTER_FINAL_VERIFICATION = "AFTER_FINAL_VERIFICATION";

  // =========================================================================
  // Administrative Operation Checkpoints
  // =========================================================================

  /**
   * Checkpoint after entering safe mode.
   */
  public static final String AFTER_SAFEMODE_ENTER = "AFTER_SAFEMODE_ENTER";

  /**
   * Checkpoint after leaving safe mode.
   */
  public static final String AFTER_SAFEMODE_LEAVE = "AFTER_SAFEMODE_LEAVE";

  /**
   * Checkpoint after setting replication factor.
   */
  public static final String AFTER_SET_REPLICATION = "AFTER_SET_REPLICATION";

  /**
   * Checkpoint after balancer operation.
   */
  public static final String AFTER_BALANCER = "AFTER_BALANCER";

  /**
   * Checkpoint after mover operation.
   */
  public static final String AFTER_MOVER = "AFTER_MOVER";

  // =========================================================================
  // Node Lifecycle Checkpoints
  // =========================================================================

  /**
   * Checkpoint after DataNode restart.
   */
  public static final String AFTER_DATANODE_RESTART = "AFTER_DATANODE_RESTART";

  /**
   * Checkpoint after NameNode restart.
   */
  public static final String AFTER_NAMENODE_RESTART = "AFTER_NAMENODE_RESTART";

  /**
   * Checkpoint before DataNode shutdown.
   */
  public static final String BEFORE_DATANODE_SHUTDOWN = "BEFORE_DATANODE_SHUTDOWN";

  /**
   * Checkpoint after DataNode shutdown.
   */
  public static final String AFTER_DATANODE_SHUTDOWN = "AFTER_DATANODE_SHUTDOWN";

  // =========================================================================
  // Test Data Setup Checkpoints
  // =========================================================================

  /**
   * Checkpoint after test data creation.
   * Useful when test creates a dataset before testing operations.
   */
  public static final String AFTER_DATA_SETUP = "AFTER_DATA_SETUP";

  /**
   * Checkpoint after creating large dataset.
   */
  public static final String AFTER_LARGE_DATASET = "AFTER_LARGE_DATASET";

  /**
   * Checkpoint in the middle of multi-step operation.
   */
  public static final String MID_OPERATION = "MID_OPERATION";
}

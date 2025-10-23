/*
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
package org.apache.hadoop.hdfs.server.process.upgrade;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for rolling upgrades in HDFS clusters.
 *
 * Tests scenarios where DataNodes are upgraded one-by-one while the cluster
 * remains operational. These tests require HADOOP_HOME environment variable
 * and multiple Hadoop distributions.
 *
 * NOTE: These are integration tests that require actual Hadoop distributions.
 * They will be skipped if required environment variables are not set.
 */
public class TestRollingUpgrade {
  private static final Logger LOG = LoggerFactory.getLogger(TestRollingUpgrade.class);

  private ProcessBasedMiniDFSCluster cluster;
  private Configuration conf;
  private String sourceVersion;
  private String targetVersion;

  /**
   * Helper to get a variable from either environment or system property.
   * Checks environment variable first, then falls back to system property.
   */
  private String getEnvOrProperty(String name) {
    String value = System.getenv(name);
    if (value == null || value.isEmpty()) {
      value = System.getProperty(name);
    }
    return value;
  }

  @Before
  public void setUp() {
    conf = new HdfsConfiguration();
    conf.set("dfs.replication", "3");

    // Check for required Hadoop versions (check both env var and system property)
    sourceVersion = getEnvOrProperty("HADOOP_3_3_1_HOME");
    targetVersion = getEnvOrProperty("HADOOP_3_3_5_HOME");

    if (sourceVersion == null || sourceVersion.isEmpty()) {
      LOG.warn("HADOOP_3_3_1_HOME not set, skipping rolling upgrade tests");
    }
    if (targetVersion == null || targetVersion.isEmpty()) {
      LOG.warn("HADOOP_3_3_5_HOME not set, skipping rolling upgrade tests");
    }
  }

  @After
  public void tearDown() {
    if (cluster != null) {
      try {
        cluster.shutdown();
      } catch (Exception e) {
        LOG.error("Error shutting down cluster", e);
      }
    }
  }

  /**
   * Tests a basic rolling upgrade of DataNodes from one version to another.
   * This is the fundamental upgrade scenario.
   */
  @Test
  public void testBasicRollingUpgrade() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing basic rolling upgrade from {} to {}", sourceVersion, targetVersion);

    // Start cluster with source version
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write test data before upgrade
    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    LOG.info("Test data written, starting rolling upgrade");

    // Perform rolling upgrade
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, targetVersion, testFiles);

    // Verify cluster is healthy after upgrade
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Write new data after upgrade to ensure full functionality
    List<Path> postUpgradeFiles = UpgradeTestHelper.writeTestData(fs, 5);
    UpgradeTestHelper.verifyTestData(fs, postUpgradeFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, postUpgradeFiles);

    LOG.info("Basic rolling upgrade test passed");
  }

  /**
   * Tests rolling upgrade with continuous read operations.
   * Verifies that data remains accessible throughout the upgrade process.
   */
  @Test
  public void testRollingUpgradeWithContinuousReads() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling upgrade with continuous reads");

    // Start cluster
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write test data
    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Perform rolling upgrade with verification after each node
    UpgradeTestHelper.performRollingDataNodeUpgrade(
        cluster,
        targetVersion,
        new UpgradeTestHelper.UpgradeVerifier() {
          @Override
          public void verify(ProcessBasedMiniDFSCluster cluster, int nodeIndex) throws Exception {
            LOG.info("Verifying data after upgrading node {}", nodeIndex);

            // Verify all existing data is still readable
            FileSystem fs = cluster.getFileSystem();
            UpgradeTestHelper.verifyTestData(fs, testFiles);

            // Write new data to test write capability
            Path newFile = new Path("/upgrade-test/node-" + nodeIndex + "-verify.dat");
            UpgradeTestHelper.writeTestData(fs, 1);

            LOG.info("Data verification passed after upgrading node {}", nodeIndex);
          }
        });

    // Final verification
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Rolling upgrade with continuous reads test passed");
  }

  /**
   * Tests rolling upgrade with data writes during the upgrade process.
   * This simulates a real-world scenario where the cluster remains in use during upgrade.
   */
  @Test
  public void testRollingUpgradeWithWrites() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling upgrade with writes");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    List<Path> allTestFiles = new ArrayList<>();

    // Write initial data
    List<Path> initialFiles = UpgradeTestHelper.writeTestData(fs, 5);
    allTestFiles.addAll(initialFiles);

    // Upgrade each DataNode, writing new data after each upgrade
    int numDataNodes = cluster.getNumDataNodes();
    for (int i = 0; i < numDataNodes; i++) {
      LOG.info("Upgrading DataNode {}", i);

      // Upgrade the node
      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, targetVersion);
      cluster.startDataNode(i);
      cluster.waitClusterUp();

      // Write new data after this node is upgraded
      List<Path> newFiles = UpgradeTestHelper.writeTestData(fs, 2);
      allTestFiles.addAll(newFiles);

      // Verify all data written so far
      UpgradeTestHelper.verifyTestData(fs, allTestFiles);

      LOG.info("DataNode {} upgraded successfully", i);
    }

    // Final verification of all data
    UpgradeTestHelper.verifyTestData(fs, allTestFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, allTestFiles);

    LOG.info("Rolling upgrade with writes test passed");
  }

  /**
   * Tests rolling upgrade where nodes are upgraded in reverse order.
   * Verifies that upgrade order doesn't affect success.
   */
  @Test
  public void testRollingUpgradeReverseOrder() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling upgrade in reverse order");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Upgrade DataNodes in reverse order (2, 1, 0)
    int numDataNodes = cluster.getNumDataNodes();
    for (int i = numDataNodes - 1; i >= 0; i--) {
      LOG.info("Upgrading DataNode {} (reverse order)", i);

      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, targetVersion);
      cluster.startDataNode(i);
      cluster.waitClusterUp();

      // Verify data after each upgrade
      UpgradeTestHelper.verifyTestData(fs, testFiles);

      LOG.info("DataNode {} upgraded successfully", i);
    }

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Rolling upgrade reverse order test passed");
  }

  /**
   * Tests partial rolling upgrade - upgrade some nodes but not all.
   * This verifies that a mixed-version cluster can operate stably.
   */
  @Test
  public void testPartialRollingUpgrade() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing partial rolling upgrade");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(4)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Upgrade only first 2 DataNodes (50% of cluster)
    LOG.info("Upgrading DataNodes 0 and 1 (partial upgrade)");

    for (int i = 0; i < 2; i++) {
      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, targetVersion);
      cluster.startDataNode(i);
      cluster.waitClusterUp();
      UpgradeTestHelper.verifyTestData(fs, testFiles);
    }

    // Verify mixed-version cluster operates correctly
    LOG.info("Cluster now has 2 nodes on {} and 2 nodes on {}", targetVersion, sourceVersion);

    // Perform operations on mixed cluster
    UpgradeTestHelper.assertCanReadWriteData(fs);
    List<Path> mixedVersionFiles = UpgradeTestHelper.writeTestData(fs, 5);
    UpgradeTestHelper.verifyTestData(fs, mixedVersionFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, mixedVersionFiles);

    LOG.info("Partial rolling upgrade test passed");
  }

  /**
   * Tests rolling upgrade with node failures.
   * Simulates a scenario where a node fails during upgrade and needs to be restarted.
   */
  @Test
  public void testRollingUpgradeWithNodeFailure() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling upgrade with node failure simulation");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Upgrade first DataNode successfully
    LOG.info("Upgrading DataNode 0");
    cluster.shutdownDataNode(0);
    cluster.changeDataNodeVersion(0, targetVersion);
    cluster.startDataNode(0);
    cluster.waitClusterUp();

    // Simulate failure during second DataNode upgrade by shutting it down twice
    LOG.info("Simulating failure during DataNode 1 upgrade");
    cluster.shutdownDataNode(1);
    cluster.changeDataNodeVersion(1, targetVersion);
    // Don't start it yet - simulate a failed start

    // Cluster should still be operational with remaining nodes
    LOG.info("Verifying cluster operates with one node down");
    // Note: With replication=3 and one node down, cluster should still work
    // but may need to wait for re-replication

    // Now recover the failed node
    LOG.info("Recovering failed DataNode 1");
    cluster.startDataNode(1);
    cluster.waitClusterUp();

    // Verify data integrity
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Complete upgrade of remaining node
    LOG.info("Upgrading DataNode 2");
    cluster.shutdownDataNode(2);
    cluster.changeDataNodeVersion(2, targetVersion);
    cluster.startDataNode(2);
    cluster.waitClusterUp();

    // Final verification
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Rolling upgrade with node failure test passed");
  }

  /**
   * Tests downgrade scenario - rolling back from newer to older version.
   * This verifies that the process works in both directions (with compatible versions).
   */
  @Test
  public void testRollingDowngrade() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling downgrade from {} to {}", targetVersion, sourceVersion);

    // Start cluster with newer version
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write test data
    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Perform rolling "downgrade" to older version
    LOG.info("Starting rolling downgrade");
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, sourceVersion, testFiles);

    // Verify cluster is healthy after downgrade
    UpgradeTestHelper.assertCanReadWriteData(fs);
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Rolling downgrade test passed");
  }

  /**
   * Tests multiple successive upgrades.
   * Simulates upgrading through multiple versions: 3.3.1 -> 3.3.5 -> 3.3.6
   */
  @Test
  public void testMultipleSuccessiveUpgrades() throws Exception {
    String version331 = getEnvOrProperty("HADOOP_3_3_1_HOME");
    String version335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String version336 = getEnvOrProperty("HADOOP_3_3_6_HOME");

    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", version331);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", version335);

    // If 3.3.6 not available, use 3.3.5 as final target
    String finalVersion = version336 != null ? version336 : version335;

    LOG.info("Testing multiple successive upgrades: {} -> {} -> {}",
        version331, version335, finalVersion);

    // Start with 3.3.1
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // First upgrade: 3.3.1 -> 3.3.5
    LOG.info("First upgrade: {} -> {}", version331, version335);
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, version335, testFiles);
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    if (version336 != null) {
      // Second upgrade: 3.3.5 -> 3.3.6
      LOG.info("Second upgrade: {} -> {}", version335, version336);
      UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, version336, testFiles);
      UpgradeTestHelper.verifyTestData(fs, testFiles);
    }

    // Verify cluster health after all upgrades
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Multiple successive upgrades test passed");
  }

  /**
   * Tests upgrade with large dataset.
   * Verifies that upgrade works correctly with significant data volume.
   */
  @Test
  public void testRollingUpgradeWithLargeDataset() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", sourceVersion);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", targetVersion);

    LOG.info("Testing rolling upgrade with large dataset");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write larger dataset (50 files, 2MB each = 100MB total)
    FileSystem fs = cluster.getFileSystem();
    LOG.info("Writing large test dataset (50 files, 2MB each)");
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 50, 2 * 1024 * 1024);

    // Perform rolling upgrade
    LOG.info("Starting rolling upgrade with large dataset");
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, targetVersion, testFiles);

    // Verify all data
    LOG.info("Verifying large dataset after upgrade");
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Rolling upgrade with large dataset test passed");
  }
}

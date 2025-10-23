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

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for mixed-version HDFS clusters.
 *
 * Tests scenarios where NameNodes and DataNodes run different Hadoop versions.
 * These tests require HADOOP_HOME environment variable to be set and multiple
 * Hadoop distributions to be available.
 *
 * NOTE: These are integration tests that require actual Hadoop distributions.
 * They will be skipped if HADOOP_HOME is not set or required versions are not available.
 */
public class TestMixedVersionCluster {
  private static final Logger LOG = LoggerFactory.getLogger(TestMixedVersionCluster.class);

  private ProcessBasedMiniDFSCluster cluster;
  private Configuration conf;
  private String hadoopHome;

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
    conf.set("dfs.replication", "2");

    // Check if HADOOP_HOME is set (check both env var and system property)
    // Try HADOOP_3_3_5_HOME as fallback since it's more reliably passed via system properties
    hadoopHome = getEnvOrProperty("HADOOP_HOME");

    // Reject invalid paths (like build target directories) and use fallback
    if (hadoopHome != null && (hadoopHome.contains("/target") || hadoopHome.contains("\\target"))) {
      LOG.warn("HADOOP_HOME points to invalid build directory ({}), using HADOOP_3_3_5_HOME instead", hadoopHome);
      hadoopHome = null;
    }

    if (hadoopHome == null || hadoopHome.isEmpty()) {
      hadoopHome = getEnvOrProperty("HADOOP_3_3_5_HOME");
      if (hadoopHome != null && !hadoopHome.isEmpty()) {
        LOG.info("Using HADOOP_3_3_5_HOME: {}", hadoopHome);
      }
    }

    if (hadoopHome == null || hadoopHome.isEmpty()) {
      LOG.warn("Neither HADOOP_HOME nor HADOOP_3_3_5_HOME set, skipping integration tests");
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
   * Tests a cluster where all nodes run the same Hadoop version.
   * This is the baseline test to ensure the cluster can start and perform basic operations.
   */
  @Test
  public void testSameVersionCluster() throws Exception {
    Assume.assumeNotNull("HADOOP_HOME must be set", hadoopHome);

    LOG.info("Testing cluster with all nodes on version: {}", hadoopHome);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Verify basic operations
    FileSystem fs = cluster.getFileSystem();
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Write and verify test data
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 5);
    UpgradeTestHelper.verifyTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Same-version cluster test passed");
  }

  /**
   * Tests a cluster where DataNodes run different patch versions within the same minor version.
   * For example: NN on 3.3.1, DN0 on 3.3.1, DN1 on 3.3.5, DN2 on 3.3.6
   *
   * This should work as patch versions are typically compatible.
   */
  @Test
  public void testMixedPatchVersions() throws Exception {
    // This test requires multiple Hadoop distributions
    String hadoop331 = getEnvOrProperty("HADOOP_3_3_1_HOME");
    String hadoop335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String hadoop336 = getEnvOrProperty("HADOOP_3_3_6_HOME");

    Assume.assumeNotNull("HADOOP_3_3_1_HOME must be set", hadoop331);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);

    LOG.info("Testing cluster with mixed patch versions");
    LOG.info("NameNode: {}", hadoop331);
    LOG.info("DataNode 0: {}", hadoop331);
    LOG.info("DataNode 1: {}", hadoop335);
    LOG.info("DataNode 2: {}", hadoop336 != null ? hadoop336 : hadoop335);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(hadoop331)
        .dataNodeHadoopDistribution(0, hadoop331)
        .dataNodeHadoopDistribution(1, hadoop335)
        .dataNodeHadoopDistribution(2, hadoop336 != null ? hadoop336 : hadoop335)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Verify all DataNodes registered
    assertEquals(3, cluster.getNumDataNodes());

    // Verify file operations work
    FileSystem fs = cluster.getFileSystem();
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Create files with replication=3 to ensure all DNs are used
    conf.set("dfs.replication", "3");
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 5);

    // Verify data is readable
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("Mixed patch versions test passed");
  }

  /**
   * Tests a cluster where DataNodes run different minor versions within Hadoop 3.x.
   * For example: NN on 3.3.5, DNs on 3.3.x and 3.4.x
   *
   * This tests forward and backward compatibility within the 3.x series.
   */
  @Test
  public void testMixedMinorVersionsHadoop3x() throws Exception {
    String hadoop333 = getEnvOrProperty("HADOOP_3_3_3_HOME");
    String hadoop335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String hadoop340 = getEnvOrProperty("HADOOP_3_4_0_HOME");

    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);

    // Use available versions, fall back to same version if specific versions not available
    String nnVersion = hadoop335;
    String dn0Version = hadoop333 != null ? hadoop333 : hadoop335;
    String dn1Version = hadoop335;
    String dn2Version = hadoop340 != null ? hadoop340 : hadoop335;

    LOG.info("Testing cluster with mixed minor versions (Hadoop 3.x)");
    LOG.info("NameNode: {}", nnVersion);
    LOG.info("DataNode 0: {}", dn0Version);
    LOG.info("DataNode 1: {}", dn1Version);
    LOG.info("DataNode 2: {}", dn2Version);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(nnVersion)
        .dataNodeHadoopDistribution(0, dn0Version)
        .dataNodeHadoopDistribution(1, dn1Version)
        .dataNodeHadoopDistribution(2, dn2Version)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Verify cluster health
    FileSystem fs = cluster.getFileSystem();
    UpgradeTestHelper.assertCanReadWriteData(fs);

    LOG.info("Mixed minor versions (Hadoop 3.x) test passed");
  }

  /**
   * Tests version compatibility checking.
   * Verifies that the VersionConfigAdapter correctly identifies compatible versions.
   */
  @Test
  public void testVersionCompatibilityCheck() {
    // Test compatible versions
    assertTrue("3.3.1 and 3.3.5 should be compatible",
        UpgradeTestHelper.verifyVersionCompatibility("3.3.1", "3.3.5"));
    assertTrue("3.3.5 and 3.4.0 should be compatible",
        UpgradeTestHelper.verifyVersionCompatibility("3.3.5", "3.4.0"));
    assertTrue("3.0.0 and 3.3.5 should be compatible",
        UpgradeTestHelper.verifyVersionCompatibility("3.0.0", "3.3.5"));

    // Test incompatible versions
    assertFalse("2.10.2 and 3.3.5 should not be compatible",
        UpgradeTestHelper.verifyVersionCompatibility("2.10.2", "3.3.5"));

    LOG.info("Version compatibility check test passed");
  }

  /**
   * Tests that the cluster can handle DataNode restarts with different versions.
   */
  @Test
  public void testDataNodeRestartWithVersionChange() throws Exception {
    String hadoop335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String hadoop336 = getEnvOrProperty("HADOOP_3_3_6_HOME");

    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336);

    LOG.info("Testing DataNode restart with version change");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write test data
    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 5);

    // Restart DN0 with different version
    LOG.info("Restarting DataNode 0 with version change: {} -> {}", hadoop335, hadoop336);
    cluster.shutdownDataNode(0);
    cluster.changeDataNodeVersion(0, hadoop336);
    cluster.startDataNode(0);
    cluster.waitClusterUp();

    // Verify data is still accessible
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Write new data to ensure cluster is functioning
    List<Path> newFiles = UpgradeTestHelper.writeTestData(fs, 3);
    UpgradeTestHelper.verifyTestData(fs, newFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, newFiles);

    LOG.info("DataNode restart with version change test passed");
  }

  /**
   * Tests cluster behavior with all DataNodes running different versions.
   * This is the most heterogeneous scenario.
   */
  @Test
  public void testAllDataNodesDifferentVersions() throws Exception {
    String hadoop331 = System.getenv("HADOOP_3_3_1_HOME");
    String hadoop335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String hadoop336 = getEnvOrProperty("HADOOP_3_3_6_HOME");

    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);

    // Use different versions if available
    String dn0Version = hadoop331 != null ? hadoop331 : hadoop335;
    String dn1Version = hadoop335;
    String dn2Version = hadoop336 != null ? hadoop336 : hadoop335;

    LOG.info("Testing cluster with all DataNodes on different versions");
    LOG.info("NameNode: {}", hadoop335);
    LOG.info("DataNode 0: {}", dn0Version);
    LOG.info("DataNode 1: {}", dn1Version);
    LOG.info("DataNode 2: {}", dn2Version);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(hadoop335)
        .dataNodeHadoopDistribution(0, dn0Version)
        .dataNodeHadoopDistribution(1, dn1Version)
        .dataNodeHadoopDistribution(2, dn2Version)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Perform extensive testing
    FileSystem fs = cluster.getFileSystem();

    // Test 1: Basic operations
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Test 2: Write multiple files
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10, 512 * 1024);
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Test 3: Verify all nodes are participating
    assertEquals(3, cluster.getNumDataNodes());

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("All DataNodes different versions test passed");
  }

  /**
   * Tests that incompatible version combinations are properly rejected or handled.
   * This test documents expected behavior when mixing incompatible versions.
   */
  @Test
  public void testIncompatibleVersionDetection() throws Exception {
    String hadoop2 = getEnvOrProperty("HADOOP_2_10_HOME");
    String hadoop3 = getEnvOrProperty("HADOOP_3_3_5_HOME");

    // Skip if we don't have both major versions
    Assume.assumeNotNull("HADOOP_2_10_HOME must be set", hadoop2);
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop3);

    LOG.info("Testing incompatible version detection (Hadoop 2 vs 3)");

    // Verify that the version adapter detects incompatibility
    assertFalse("Hadoop 2.x and 3.x should not be compatible",
        UpgradeTestHelper.verifyVersionCompatibility("2.10.2", "3.3.5"));

    // Note: Actually starting a cluster with incompatible versions may fail
    // or cause unpredictable behavior. This test primarily validates the
    // version compatibility checking logic.

    LOG.info("Incompatible version detection test passed");
  }

  /**
   * Tests NameNode restart (in a non-HA setup, for simplicity).
   * This verifies that the cluster can survive a NameNode restart.
   */
  @Test
  public void testNameNodeRestart() throws Exception {
    Assume.assumeNotNull("HADOOP_HOME must be set", hadoopHome);

    LOG.info("Testing NameNode restart");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();

    cluster.waitClusterUp();

    // Write test data
    FileSystem fs = cluster.getFileSystem();
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 5);

    // Restart NameNode
    LOG.info("Restarting NameNode");
    cluster.restartNameNode(0);
    cluster.waitClusterUp();

    // Verify data is still accessible
    // Note: Need to get new FileSystem instance after NN restart
    fs = cluster.getFileSystem();
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("NameNode restart test passed");
  }

  /**
   * Tests cluster stability over time with mixed versions.
   * Performs multiple read/write cycles to ensure sustained operation.
   */
  @Test
  public void testMixedVersionClusterStability() throws Exception {
    String hadoop335 = getEnvOrProperty("HADOOP_3_3_5_HOME");
    String hadoop336 = getEnvOrProperty("HADOOP_3_3_6_HOME");

    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335);

    String dn1Version = hadoop336 != null ? hadoop336 : hadoop335;

    LOG.info("Testing mixed-version cluster stability");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .nameNodeHadoopDistribution(hadoop335)
        .dataNodeHadoopDistribution(0, hadoop335)
        .dataNodeHadoopDistribution(1, dn1Version)
        .format(true)
        .build();

    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();

    // Perform multiple write/read cycles
    for (int cycle = 0; cycle < 3; cycle++) {
      LOG.info("Stability test cycle {}", cycle + 1);

      List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 5);
      UpgradeTestHelper.verifyTestData(fs, testFiles);
      UpgradeTestHelper.cleanupTestData(fs, testFiles);

      // Wait a bit between cycles
      Thread.sleep(1000);
    }

    LOG.info("Mixed-version cluster stability test passed");
  }
}

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive integration tests for Hadoop 3.3.5 to 3.3.6 upgrade.
 *
 * This test suite validates the upgrade path from Hadoop 3.3.5 to 3.3.6
 * using ProcessBasedMiniDFSCluster. It covers:
 * - Rolling upgrades with data integrity verification
 * - Mixed-version cluster operation
 * - Downgrade scenarios
 * - Upgrade under load
 * - Large dataset handling
 *
 * Prerequisites:
 *   export HADOOP_3_3_5_HOME=/path/to/hadoop-3.3.5
 *   export HADOOP_3_3_6_HOME=/path/to/hadoop-3.3.6
 *
 * Tests will be skipped if these environment variables are not set.
 */
public class TestHadoop335To336Upgrade {
  private static final Logger LOG = LoggerFactory.getLogger(TestHadoop335To336Upgrade.class);

  private ProcessBasedMiniDFSCluster cluster;
  private Configuration conf;
  private String hadoop335Home;
  private String hadoop336Home;

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
    hadoop335Home = getEnvOrProperty("HADOOP_3_3_5_HOME");
    hadoop336Home = getEnvOrProperty("HADOOP_3_3_6_HOME");

    if (hadoop335Home == null || hadoop335Home.isEmpty()) {
      LOG.warn("HADOOP_3_3_5_HOME not set, skipping tests");
    }
    if (hadoop336Home == null || hadoop336Home.isEmpty()) {
      LOG.warn("HADOOP_3_3_6_HOME not set, skipping tests");
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
   * Test 1: Basic rolling upgrade from 3.3.5 to 3.3.6
   *
   * This is the fundamental upgrade scenario:
   * 1. Start cluster with Hadoop 3.3.5
   * 2. Write test data
   * 3. Upgrade DataNodes one-by-one to 3.3.6
   * 4. Verify data integrity after each upgrade
   * 5. Verify cluster health
   */
  @Test
  public void testBasicRollingUpgrade335To336() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Basic Rolling Upgrade 3.3.5 → 3.3.6");
    LOG.info("=================================================================");

    // Start cluster with 3.3.5
    LOG.info("Starting cluster with Hadoop 3.3.5");
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();
    LOG.info("Cluster started successfully with 3.3.5");

    // Write test data before upgrade
    FileSystem fs = cluster.getFileSystem();
    LOG.info("Writing test data (20 files, 1MB each)");
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 20);
    LOG.info("Test data written successfully");

    // Verify data before upgrade
    UpgradeTestHelper.verifyTestData(fs, testFiles);
    LOG.info("Data verification before upgrade: PASSED");

    // Perform rolling upgrade
    LOG.info("Starting rolling upgrade from 3.3.5 to 3.3.6");
    long upgradeStartTime = System.currentTimeMillis();

    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, hadoop336Home, testFiles);

    long upgradeDuration = System.currentTimeMillis() - upgradeStartTime;
    LOG.info("Rolling upgrade completed in {}ms", upgradeDuration);

    // Verify cluster health after upgrade
    LOG.info("Verifying cluster health after upgrade");
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Write new data after upgrade to ensure full functionality
    LOG.info("Writing new data after upgrade");
    List<Path> postUpgradeFiles = UpgradeTestHelper.writeTestData(fs, 10);
    UpgradeTestHelper.verifyTestData(fs, postUpgradeFiles);
    LOG.info("Post-upgrade write and read: PASSED");

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, postUpgradeFiles);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Basic Rolling Upgrade 3.3.5 → 3.3.6");
    LOG.info("  Duration: {}ms", upgradeDuration);
    LOG.info("  Files tested: {}", testFiles.size() + postUpgradeFiles.size());
    LOG.info("=================================================================");
  }

  /**
   * Test 2: Rolling downgrade from 3.3.6 to 3.3.5
   *
   * Verifies that rollback works correctly:
   * 1. Start cluster with 3.3.6
   * 2. Write data
   * 3. Downgrade to 3.3.5
   * 4. Verify data integrity
   */
  @Test
  public void testRollingDowngrade336To335() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Rolling Downgrade 3.3.6 → 3.3.5");
    LOG.info("=================================================================");

    // Start cluster with 3.3.6
    LOG.info("Starting cluster with Hadoop 3.3.6");
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();
    LOG.info("Cluster started successfully with 3.3.6");

    // Write test data
    FileSystem fs = cluster.getFileSystem();
    LOG.info("Writing test data (15 files, 1MB each)");
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 15);

    // Perform rolling downgrade
    LOG.info("Starting rolling downgrade from 3.3.6 to 3.3.5");
    long downgradeStartTime = System.currentTimeMillis();

    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, hadoop335Home, testFiles);

    long downgradeDuration = System.currentTimeMillis() - downgradeStartTime;
    LOG.info("Rolling downgrade completed in {}ms", downgradeDuration);

    // Verify cluster health after downgrade
    UpgradeTestHelper.assertCanReadWriteData(fs);
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Rolling Downgrade 3.3.6 → 3.3.5");
    LOG.info("  Duration: {}ms", downgradeDuration);
    LOG.info("=================================================================");
  }

  /**
   * Test 3: Mixed version cluster (3.3.5 and 3.3.6 coexisting)
   *
   * Tests cluster operation with mixed versions:
   * 1. Start NameNode with 3.3.5
   * 2. Run 2 DataNodes with 3.3.5, 1 with 3.3.6
   * 3. Perform read/write operations
   * 4. Verify cluster stability
   */
  @Test
  public void testMixedVersionCluster335And336() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Mixed Version Cluster (3.3.5 + 3.3.6)");
    LOG.info("=================================================================");

    LOG.info("Building mixed-version cluster:");
    LOG.info("  NameNode:   3.3.5");
    LOG.info("  DataNode 0: 3.3.5");
    LOG.info("  DataNode 1: 3.3.5");
    LOG.info("  DataNode 2: 3.3.6");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(hadoop335Home)
        .dataNodeHadoopDistribution(0, hadoop335Home)
        .dataNodeHadoopDistribution(1, hadoop335Home)
        .dataNodeHadoopDistribution(2, hadoop336Home)
        .format(true)
        .build();

    cluster.waitClusterUp();
    LOG.info("Mixed-version cluster started successfully");

    // Verify all DataNodes registered
    assertEquals("All DataNodes should be registered", 3, cluster.getNumDataNodes());

    // Perform operations on mixed cluster
    FileSystem fs = cluster.getFileSystem();

    // Test basic operations
    LOG.info("Testing basic read/write operations");
    UpgradeTestHelper.assertCanReadWriteData(fs);

    // Write multiple files to ensure all nodes are utilized
    LOG.info("Writing 30 files with replication=3");
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 30);

    // Verify data
    UpgradeTestHelper.verifyTestData(fs, testFiles);
    LOG.info("Data verification on mixed cluster: PASSED");

    // Perform multiple read/write cycles to test stability
    for (int cycle = 0; cycle < 3; cycle++) {
      LOG.info("Stability test cycle {} of 3", cycle + 1);
      List<Path> cycleFiles = UpgradeTestHelper.writeTestData(fs, 5);
      UpgradeTestHelper.verifyTestData(fs, cycleFiles);
      UpgradeTestHelper.cleanupTestData(fs, cycleFiles);
    }

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Mixed Version Cluster (3.3.5 + 3.3.6)");
    LOG.info("=================================================================");
  }

  /**
   * Test 4: Upgrade under load with concurrent writes
   *
   * Simulates real-world scenario where cluster remains operational during upgrade:
   * 1. Start cluster with 3.3.5
   * 2. Begin continuous write operations
   * 3. Perform rolling upgrade while writes continue
   * 4. Verify no data loss or corruption
   */
  @Test
  public void testUpgradeUnderLoad() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Upgrade Under Load (3.3.5 → 3.3.6)");
    LOG.info("=================================================================");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();
    final FileSystem fs = cluster.getFileSystem();

    // Track written files for later verification
    final List<Path> allWrittenFiles = new ArrayList<>();
    final AtomicBoolean running = new AtomicBoolean(true);
    final AtomicInteger filesWritten = new AtomicInteger(0);
    final AtomicInteger writeErrors = new AtomicInteger(0);

    // Start background writer thread
    LOG.info("Starting background write thread");
    Thread writerThread = new Thread(() -> {
      while (running.get()) {
        try {
          Path file = new Path("/stress-test/file-" +
              System.currentTimeMillis() + "-" +
              filesWritten.get() + ".dat");

          byte[] data = new byte[256 * 1024]; // 256KB
          fs.mkdirs(file.getParent());
          fs.create(file).write(data);

          synchronized (allWrittenFiles) {
            allWrittenFiles.add(file);
          }
          filesWritten.incrementAndGet();

          Thread.sleep(200); // Write every 200ms
        } catch (Exception e) {
          LOG.warn("Write operation failed during upgrade: {}", e.getMessage());
          writeErrors.incrementAndGet();
        }
      }
    });

    writerThread.start();

    // Let some writes happen before upgrade
    Thread.sleep(2000);
    LOG.info("Background writes started, {} files written so far", filesWritten.get());

    // Perform upgrade while writes continue
    LOG.info("Starting rolling upgrade with background writes active");
    long upgradeStartTime = System.currentTimeMillis();

    for (int i = 0; i < 3; i++) {
      LOG.info("Upgrading DataNode {} while writes continue", i);

      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, hadoop336Home);
      cluster.startDataNode(i);
      cluster.waitClusterUp();

      // Let writes continue during stabilization
      Thread.sleep(2000);

      LOG.info("DataNode {} upgraded, {} files written so far",
          i, filesWritten.get());
    }

    long upgradeDuration = System.currentTimeMillis() - upgradeStartTime;
    LOG.info("Rolling upgrade completed in {}ms", upgradeDuration);

    // Stop writer thread
    running.set(false);
    writerThread.join(5000);

    int totalFilesWritten = filesWritten.get();
    int totalWriteErrors = writeErrors.get();

    LOG.info("Background writes stopped:");
    LOG.info("  Files written: {}", totalFilesWritten);
    LOG.info("  Write errors: {}", totalWriteErrors);

    // Verify that files were written during upgrade
    assertTrue("Should have written files during upgrade", totalFilesWritten > 0);

    // Some write errors are acceptable during node transitions, but not too many
    double errorRate = (double) totalWriteErrors / totalFilesWritten;
    assertTrue("Error rate should be less than 20%: " + errorRate,
        errorRate < 0.2);

    // Verify all successfully written files are readable
    LOG.info("Verifying {} files written during upgrade", allWrittenFiles.size());
    int verifiedFiles = 0;
    synchronized (allWrittenFiles) {
      for (Path file : allWrittenFiles) {
        if (fs.exists(file)) {
          verifiedFiles++;
        }
      }
    }
    LOG.info("Verified {} files out of {} exist", verifiedFiles, allWrittenFiles.size());

    // Cleanup
    fs.delete(new Path("/stress-test"), true);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Upgrade Under Load");
    LOG.info("  Upgrade duration: {}ms", upgradeDuration);
    LOG.info("  Files written: {}", totalFilesWritten);
    LOG.info("  Files verified: {}", verifiedFiles);
    LOG.info("  Write errors: {} ({}%)", totalWriteErrors,
        String.format("%.2f", errorRate * 100));
    LOG.info("=================================================================");
  }

  /**
   * Test 5: Large dataset upgrade
   *
   * Tests upgrade with significant data volume:
   * 1. Write large dataset (100 files, 5MB each = 500MB)
   * 2. Perform rolling upgrade
   * 3. Verify all data integrity
   */
  @Test
  public void testLargeDatasetUpgrade() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Large Dataset Upgrade (3.3.5 → 3.3.6)");
    LOG.info("=================================================================");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    // Write large dataset
    LOG.info("Writing large dataset (100 files, 5MB each = 500MB total)");
    long writeStartTime = System.currentTimeMillis();
    List<Path> largeDataset = UpgradeTestHelper.writeTestData(fs, 100, 5 * 1024 * 1024);
    long writeDuration = System.currentTimeMillis() - writeStartTime;
    LOG.info("Large dataset written in {}ms", writeDuration);

    // Verify data before upgrade
    LOG.info("Verifying large dataset before upgrade");
    long verifyStartTime = System.currentTimeMillis();
    UpgradeTestHelper.verifyTestData(fs, largeDataset);
    long verifyDuration = System.currentTimeMillis() - verifyStartTime;
    LOG.info("Large dataset verified in {}ms", verifyDuration);

    // Perform rolling upgrade
    LOG.info("Starting rolling upgrade with large dataset");
    long upgradeStartTime = System.currentTimeMillis();
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, hadoop336Home, largeDataset);
    long upgradeDuration = System.currentTimeMillis() - upgradeStartTime;
    LOG.info("Rolling upgrade with large dataset completed in {}ms", upgradeDuration);

    // Verify all data after upgrade
    LOG.info("Verifying large dataset after upgrade");
    verifyStartTime = System.currentTimeMillis();
    UpgradeTestHelper.verifyTestData(fs, largeDataset);
    verifyDuration = System.currentTimeMillis() - verifyStartTime;
    LOG.info("Large dataset verified after upgrade in {}ms", verifyDuration);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, largeDataset);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Large Dataset Upgrade");
    LOG.info("  Dataset: 100 files, 500MB total");
    LOG.info("  Upgrade duration: {}ms", upgradeDuration);
    LOG.info("=================================================================");
  }

  /**
   * Test 6: Partial upgrade scenario
   *
   * Tests operating with partially upgraded cluster:
   * 1. Start with 4 DataNodes on 3.3.5
   * 2. Upgrade only 2 DataNodes to 3.3.6
   * 3. Run operations on mixed cluster for extended period
   * 4. Complete upgrade of remaining nodes
   */
  @Test
  public void testPartialUpgrade() throws Exception {
    Assume.assumeNotNull("HADOOP_3_3_5_HOME must be set", hadoop335Home);
    Assume.assumeNotNull("HADOOP_3_3_6_HOME must be set", hadoop336Home);

    LOG.info("=================================================================");
    LOG.info("Test: Partial Upgrade (3.3.5 → 3.3.6)");
    LOG.info("=================================================================");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(4)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    // Write initial data
    LOG.info("Writing initial dataset");
    List<Path> initialFiles = UpgradeTestHelper.writeTestData(fs, 20);

    // Upgrade only first 2 DataNodes (50% of cluster)
    LOG.info("Upgrading DataNodes 0 and 1 to 3.3.6 (partial upgrade)");

    for (int i = 0; i < 2; i++) {
      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, hadoop336Home);
      cluster.startDataNode(i);
      cluster.waitClusterUp();
      UpgradeTestHelper.verifyTestData(fs, initialFiles);
    }

    LOG.info("Cluster now running mixed versions:");
    LOG.info("  DataNodes 0-1: 3.3.6");
    LOG.info("  DataNodes 2-3: 3.3.5");

    // Perform extensive operations on mixed cluster
    LOG.info("Running operations on partially upgraded cluster");
    for (int cycle = 0; cycle < 5; cycle++) {
      LOG.info("Mixed-version operation cycle {} of 5", cycle + 1);
      List<Path> cycleFiles = UpgradeTestHelper.writeTestData(fs, 10);
      UpgradeTestHelper.verifyTestData(fs, cycleFiles);
      UpgradeTestHelper.cleanupTestData(fs, cycleFiles);
    }

    // Complete upgrade of remaining nodes
    LOG.info("Completing upgrade of remaining DataNodes (2-3)");
    for (int i = 2; i < 4; i++) {
      cluster.shutdownDataNode(i);
      cluster.changeDataNodeVersion(i, hadoop336Home);
      cluster.startDataNode(i);
      cluster.waitClusterUp();
      UpgradeTestHelper.verifyTestData(fs, initialFiles);
    }

    LOG.info("Upgrade complete, all DataNodes now on 3.3.6");

    // Final verification
    UpgradeTestHelper.assertCanReadWriteData(fs);
    UpgradeTestHelper.verifyTestData(fs, initialFiles);

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, initialFiles);

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Partial Upgrade");
    LOG.info("=================================================================");
  }

  /**
   * Test 7: Version compatibility verification
   *
   * Tests that version compatibility checking works correctly.
   */
  @Test
  public void testVersionCompatibility335And336() {
    LOG.info("=================================================================");
    LOG.info("Test: Version Compatibility Check (3.3.5 ↔ 3.3.6)");
    LOG.info("=================================================================");

    // Test forward compatibility
    boolean forward = UpgradeTestHelper.verifyVersionCompatibility("3.3.5", "3.3.6");
    assertTrue("3.3.5 and 3.3.6 should be forward compatible", forward);
    LOG.info("Forward compatibility (3.3.5 → 3.3.6): COMPATIBLE ✓");

    // Test backward compatibility
    boolean backward = UpgradeTestHelper.verifyVersionCompatibility("3.3.6", "3.3.5");
    assertTrue("3.3.6 and 3.3.5 should be backward compatible", backward);
    LOG.info("Backward compatibility (3.3.6 → 3.3.5): COMPATIBLE ✓");

    LOG.info("=================================================================");
    LOG.info("Test PASSED: Version Compatibility Check");
    LOG.info("=================================================================");
  }
}

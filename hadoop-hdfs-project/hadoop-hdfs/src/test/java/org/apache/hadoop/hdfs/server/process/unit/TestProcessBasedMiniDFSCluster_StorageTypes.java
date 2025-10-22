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
package org.apache.hadoop.hdfs.server.process.unit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.DirectoryManager;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/**
 * Unit tests for storage types feature in ProcessBasedMiniDFSCluster.
 * These tests verify the storage type configuration without actually starting the cluster.
 */
public class TestProcessBasedMiniDFSCluster_StorageTypes {

  private Configuration conf;
  private String hadoopHome;
  private ProcessBasedMiniDFSCluster cluster;

  @Before
  public void setUp() {
    conf = new HdfsConfiguration();
    hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster tests", hadoopHome);
  }

  @After
  public void tearDown() {
    if (cluster != null) {
      try {
        cluster.shutdown();
      } catch (Exception e) {
        // Ignore shutdown errors in tests
      }
    }
  }

  /**
   * Test 1: Single storage type for all DataNodes (1D array).
   * All DNs should have the same storage configuration.
   */
  @Test
  public void testSingleStorageTypeForAllDataNodes() throws Exception {
    StorageType[] types = {StorageType.SSD, StorageType.DISK};

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .storageTypes(types)
        .storagesPerDatanode(2)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    // Verify cluster was built successfully
    assertNotNull("Cluster should be created", cluster);

    // The cluster should have been configured with storage types
    // This is a unit test - we're just verifying the builder accepts the configuration
    // Integration tests will verify the actual behavior
  }

  /**
   * Test 2: Heterogeneous storage per DataNode (2D array).
   * Each DN can have different storage configuration.
   */
  @Test
  public void testHeterogeneousStoragePerDataNode() throws Exception {
    StorageType[][] types = {
        {StorageType.SSD, StorageType.DISK},        // DN 0
        {StorageType.DISK, StorageType.ARCHIVE},    // DN 1
        {StorageType.SSD, StorageType.SSD}          // DN 2
    };

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .storageTypes(types)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    assertNotNull("Cluster should be created", cluster);
  }

  /**
   * Test 3: Default behavior when no storage types specified.
   * Should default to single DISK storage per DN.
   */
  @Test
  public void testDefaultStorageTypes() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    assertNotNull("Cluster should be created with default storage", cluster);
  }

  /**
   * Test 4: Validation - array length mismatch should throw exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testStorageTypeArrayLengthMismatch() throws Exception {
    StorageType[][] types = {
        {StorageType.SSD, StorageType.DISK},  // Only 2 DNs configured
        {StorageType.DISK, StorageType.ARCHIVE}
    };

    // Should throw IllegalArgumentException because types.length (2) != numDataNodes (3)
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)  // 3 DataNodes
        .storageTypes(types)  // But only 2 entries in array
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();
  }

  /**
   * Test 5: Multiple storages per DataNode.
   */
  @Test
  public void testMultipleStoragesPerDataNode() throws Exception {
    StorageType[] types = {
        StorageType.SSD,
        StorageType.DISK,
        StorageType.ARCHIVE
    };

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .storageTypes(types)
        .storagesPerDatanode(3)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    assertNotNull("Cluster should be created with multiple storages per DN", cluster);
  }

  /**
   * Test 6: All storage types.
   */
  @Test
  public void testAllStorageTypes() throws Exception {
    StorageType[] types = {
        StorageType.RAM_DISK,
        StorageType.SSD,
        StorageType.DISK,
        StorageType.ARCHIVE
    };

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .storageTypes(types)
        .storagesPerDatanode(4)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    assertNotNull("Cluster should support all storage types", cluster);
  }

  /**
   * Test 7: Backward compatibility - using old API without storage types.
   */
  @Test
  public void testBackwardCompatibility() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();

    assertNotNull("Cluster should work without storage type configuration", cluster);
  }

  /**
   * Test 8: Invalid storagesPerDatanode should throw exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidStoragesPerDataNode() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .storagesPerDatanode(0)  // Invalid: must be >= 1
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .buildWithoutStart();
  }

  /**
   * Test 9: Test DirectoryManager creates correct storage directories.
   */
  @Test
  public void testDirectoryManagerCreatesStorageDirectories() throws Exception {
    File baseDir = new File(System.getProperty("test.build.data", "target/test/data"),
        "test-storage-dirs");
    baseDir.mkdirs();

    DirectoryManager dirManager = new DirectoryManager(baseDir, false);

    StorageType[] types = {StorageType.SSD, StorageType.DISK, StorageType.ARCHIVE};

    DirectoryManager.NodeDirectory nodeDir =
        dirManager.createDataNodeDirectory(0, 3, types);

    // Verify node directory was created
    assertNotNull("Node directory should be created", nodeDir);
    assertNotNull("Node base directory should exist", nodeDir.getNodeBaseDir());
    assertTrue("Node base directory should exist", nodeDir.getNodeBaseDir().exists());

    // Verify storage directories were created
    List<File> storageDirs = nodeDir.getDataStorageDirs();
    assertNotNull("Storage directories list should not be null", storageDirs);
    assertEquals("Should have 3 storage directories", 3, storageDirs.size());

    // Verify storage types are stored correctly
    StorageType[] storedTypes = nodeDir.getStorageTypes();
    assertNotNull("Storage types should be stored", storedTypes);
    assertArrayEquals("Storage types should match", types, storedTypes);

    // Verify each storage directory exists
    for (File storageDir : storageDirs) {
      assertTrue("Storage directory should exist: " + storageDir,
          storageDir.exists());
    }

    // Clean up
    deleteRecursively(baseDir);
  }

  /**
   * Test 10: DirectoryManager backward compatibility - single storage directory.
   */
  @Test
  public void testDirectoryManagerBackwardCompatibility() throws Exception {
    File baseDir = new File(System.getProperty("test.build.data", "target/test/data"),
        "test-single-storage");
    baseDir.mkdirs();

    DirectoryManager dirManager = new DirectoryManager(baseDir, false);

    // Use old API without storage types
    DirectoryManager.NodeDirectory nodeDir = dirManager.createDataNodeDirectory(0);

    assertNotNull("Node directory should be created", nodeDir);

    List<File> storageDirs = nodeDir.getDataStorageDirs();
    assertNotNull("Storage directories list should not be null", storageDirs);
    assertEquals("Should have 1 storage directory by default", 1, storageDirs.size());

    // Clean up
    deleteRecursively(baseDir);
  }

  /**
   * Helper method to delete directory recursively.
   */
  private void deleteRecursively(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursively(child);
        }
      }
    }
    file.delete();
  }
}

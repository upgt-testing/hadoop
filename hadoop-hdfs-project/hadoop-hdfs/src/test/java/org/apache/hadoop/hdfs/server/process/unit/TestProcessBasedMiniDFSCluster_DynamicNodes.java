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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.apache.hadoop.test.LambdaTestUtils.intercept;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/**
 * Unit tests for dynamic DataNode addition in ProcessBasedMiniDFSCluster.
 */
public class TestProcessBasedMiniDFSCluster_DynamicNodes {

    private String hadoopHome;
    private ProcessBasedMiniDFSCluster cluster;

    @Before
    public void setUp() {
        hadoopHome = System.getenv("HADOOP_HOME");
        assumeNotNull("HADOOP_HOME must be set", hadoopHome);
    }

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown(true);
            cluster = null;
        }
    }

    /**
     * Test basic dynamic DataNode addition.
     */
    @Test(timeout = 120000)
    public void testBasicStartDataNodes() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        // Start with 2 DataNodes
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();
        assertEquals("Initial DataNode count", 2, cluster.getNumDataNodes());

        // Write some test data
        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-before-add.txt");
        fs.createNewFile(testFile);
        assertTrue("Test file should exist", fs.exists(testFile));

        // Add 3 more DataNodes
        cluster.startDataNodes(conf, 3, true, null, null, null);

        // Verify new count
        assertEquals("DataNode count after addition", 5, cluster.getNumDataNodes());

        // Verify cluster is still functional
        assertTrue("Cluster should be up", cluster.isClusterUp());
        assertTrue("Test file should still exist", fs.exists(testFile));

        // Create another file to verify new DataNodes are working
        Path testFile2 = new Path("/test-after-add.txt");
        fs.createNewFile(testFile2);
        assertTrue("New test file should exist", fs.exists(testFile2));
    }

    /**
     * Test dynamic DataNode addition with custom storage types.
     */
    @Test(timeout = 120000)
    public void testStartDataNodesWithStorageTypes() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        // Start with 2 DataNodes with default storage
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();
        assertEquals("Initial DataNode count", 2, cluster.getNumDataNodes());

        // Add 2 more DataNodes with heterogeneous storage
        StorageType[][] newStorageTypes = new StorageType[2][];
        newStorageTypes[0] = new StorageType[]{StorageType.SSD, StorageType.DISK};
        newStorageTypes[1] = new StorageType[]{StorageType.DISK, StorageType.ARCHIVE};

        cluster.startDataNodes(conf, 2, newStorageTypes, true, null, null, null, null);

        // Verify new count
        assertEquals("DataNode count after addition", 4, cluster.getNumDataNodes());

        // Verify cluster is still functional
        assertTrue("Cluster should be up", cluster.isClusterUp());

        // Create a file to verify the cluster works
        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-storage-types.txt");
        fs.createNewFile(testFile);
        assertTrue("Test file should exist", fs.exists(testFile));
    }

    /**
     * Test adding DataNodes multiple times.
     */
    @Test(timeout = 180000)
    public void testMultipleStartDataNodesCalls() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        // Start with 1 DataNode
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .format(true)
            .build();

        cluster.waitClusterUp();
        assertEquals("Initial DataNode count", 1, cluster.getNumDataNodes());

        // Add 1 DataNode
        cluster.startDataNodes(conf, 1, true, null, null, null);
        assertEquals("After first addition", 2, cluster.getNumDataNodes());

        // Add 2 more DataNodes
        cluster.startDataNodes(conf, 2, true, null, null, null);
        assertEquals("After second addition", 4, cluster.getNumDataNodes());

        // Add 1 more DataNode
        cluster.startDataNodes(conf, 1, true, null, null, null);
        assertEquals("After third addition", 5, cluster.getNumDataNodes());

        // Verify cluster is still functional
        assertTrue("Cluster should be up", cluster.isClusterUp());

        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-multiple-adds.txt");
        fs.createNewFile(testFile);
        assertTrue("Test file should exist", fs.exists(testFile));
    }

    /**
     * Test startDataNodes with zero nodes (should be a no-op).
     */
    @Test(timeout = 120000)
    public void testStartDataNodesWithZero() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();
        assertEquals("Initial DataNode count", 2, cluster.getNumDataNodes());

        // Try to add 0 DataNodes (should be ignored)
        cluster.startDataNodes(conf, 0, true, null, null, null);

        // Count should remain the same
        assertEquals("DataNode count should be unchanged", 2, cluster.getNumDataNodes());
    }

    /**
     * Test comprehensive startDataNodes signature with all parameters.
     */
    @Test(timeout = 120000)
    public void testComprehensiveStartDataNodesSignature() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();
        assertEquals("Initial DataNode count", 2, cluster.getNumDataNodes());

        // Use comprehensive signature (like TestExternalStoragePolicySatisfier)
        int numNewNodes = 3;
        StorageType[][] newTypes = new StorageType[numNewNodes][];
        for (int i = 0; i < numNewNodes; i++) {
            newTypes[i] = new StorageType[]{StorageType.DISK};
        }

        long[][] capacities = new long[numNewNodes][1];
        for (int i = 0; i < numNewNodes; i++) {
            capacities[i][0] = 1024L * 1024L * 1024L;  // 1GB
        }

        // Call with full signature (most parameters ignored but should not error)
        cluster.startDataNodes(conf, numNewNodes, newTypes, true, null, null, null,
            null, capacities, null, false, false, false, null, null, null);

        // Verify count
        assertEquals("DataNode count after addition", 5, cluster.getNumDataNodes());

        // Verify cluster functionality
        assertTrue("Cluster should be up", cluster.isClusterUp());
    }

    /**
     * Test error handling for invalid storage types array length.
     */
    @Test(timeout = 120000)
    public void testInvalidStorageTypesLength() throws Exception {
        Configuration conf = new HdfsConfiguration();

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();

        // Try to add 3 DataNodes but provide storage types for only 2
        StorageType[][] invalidStorageTypes = new StorageType[2][];
        invalidStorageTypes[0] = new StorageType[]{StorageType.DISK};
        invalidStorageTypes[1] = new StorageType[]{StorageType.SSD};

        intercept(IllegalArgumentException.class,
            "storageTypes array length",
            () -> cluster.startDataNodes(conf, 3, invalidStorageTypes, true,
                null, null, null, null));
    }

    /**
     * Test adding DataNodes and then restarting one.
     */
    @Test(timeout = 120000)
    public void testStartDataNodesThenRestart() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

        cluster.waitClusterUp();

        // Add 2 more DataNodes
        cluster.startDataNodes(conf, 2, true, null, null, null);
        assertEquals("After addition", 4, cluster.getNumDataNodes());

        // Restart the newly added DataNode (index 3)
        cluster.restartDataNode(3);
        cluster.waitClusterUp();

        // Verify cluster is still functional
        assertTrue("Cluster should be up", cluster.isClusterUp());
        assertEquals("DataNode count unchanged", 4, cluster.getNumDataNodes());

        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-restart.txt");
        fs.createNewFile(testFile);
        assertTrue("Test file should exist", fs.exists(testFile));
    }
}

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
package org.apache.hadoop.hdfs.server.process.integration;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * Integration tests for ProcessBasedMiniDFSCluster.
 *
 * NOTE: These tests require a valid Hadoop distribution to be available.
 * Set the HADOOP_HOME environment variable or skip these tests.
 */
public class TestProcessBasedMiniDFSCluster {

    private static final String HADOOP_HOME = System.getenv("HADOOP_HOME");
    private static final boolean SKIP_TESTS = (HADOOP_HOME == null || HADOOP_HOME.isEmpty());

    private Configuration conf;
    private ProcessBasedMiniDFSCluster cluster;
    private File testDir;

    @Before
    public void setUp() throws Exception {
        if (SKIP_TESTS) {
            System.out.println("Skipping test: HADOOP_HOME not set");
            return;
        }

        // Create test directory
        testDir = new File(System.getProperty("java.io.tmpdir"),
            "test-process-cluster-" + System.currentTimeMillis());
        testDir.mkdirs();

        // Create configuration
        conf = new Configuration();
        conf.set(DFSConfigKeys.DFS_REPLICATION_KEY, "1");

        // Reduce timeouts for faster tests
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
        conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_SAFEMODE_EXTENSION_KEY, 0);
    }

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown(true);
            cluster = null;
        }

        if (testDir != null && testDir.exists()) {
            deleteRecursive(testDir);
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    @Test
    public void testBasicClusterStartup() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        assertTrue("Cluster should be up", cluster.isClusterUp());
        assertEquals(1, cluster.getNumNameNodes());
        assertEquals(1, cluster.getNumDataNodes());

        // Verify URI is valid
        URI uri = cluster.getURI();
        assertNotNull(uri);
        assertEquals("hdfs", uri.getScheme());
    }

    @Test
    public void testMultipleDataNodes() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        assertTrue("Cluster should be up", cluster.isClusterUp());
        assertEquals(3, cluster.getNumDataNodes());
    }

    @Test
    public void testGetFileSystem() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // Get FileSystem
        FileSystem fs = cluster.getFileSystem();
        assertNotNull(fs);
        assertTrue(fs instanceof DistributedFileSystem);

        // Verify it works
        assertNotNull(fs.getStatus());
    }

    @Test
    public void testBasicFileOperations() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test.txt");

        // Write file
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("Hello World");
        }

        // Verify file exists
        assertTrue("File should exist", fs.exists(testFile));

        // Read file
        try (FSDataInputStream in = fs.open(testFile)) {
            String content = in.readUTF();
            assertEquals("Hello World", content);
        }

        // Delete file
        assertTrue("Delete should succeed", fs.delete(testFile, false));
        assertFalse("File should not exist", fs.exists(testFile));
    }

    @Test
    public void testDataNodeRestart() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-restart.txt");

        // Write file
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("Test Data");
        }

        // Restart DataNode 0
        cluster.restartDataNode(0);

        // Wait for cluster to be ready
        cluster.waitClusterUp();

        // Verify file is still readable
        assertTrue("File should exist after DN restart", fs.exists(testFile));
        try (FSDataInputStream in = fs.open(testFile)) {
            String content = in.readUTF();
            assertEquals("Test Data", content);
        }
    }

    @Test
    public void testNameNodeRestart() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test-nn-restart.txt");

        // Write file
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("NN Restart Test");
        }

        // Restart NameNode
        cluster.restartNameNode(0);

        // Need to get new FileSystem after NN restart
        fs = cluster.getNewFileSystemInstance();

        // Wait for cluster to be ready
        cluster.waitClusterUp();

        // Verify file is still accessible
        assertTrue("File should exist after NN restart", fs.exists(testFile));
        try (FSDataInputStream in = fs.open(testFile)) {
            String content = in.readUTF();
            assertEquals("NN Restart Test", content);
        }
    }

    @Test
    public void testShutdownAndCleanup() throws Exception {
        if (SKIP_TESTS) return;

        File clusterDir = new File(testDir, "cluster1");
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(clusterDir)
            .format(true)
            .build();

        assertTrue("Cluster directory should exist", clusterDir.exists());
        assertTrue("Cluster should be up", cluster.isClusterUp());

        // Shutdown with cleanup
        cluster.shutdown(true);

        assertFalse("Cluster should not be up", cluster.isClusterUp());
        // Note: Directory cleanup depends on DirectoryManager implementation
    }

    @Test
    public void testMultipleSequentialClusters() throws Exception {
        if (SKIP_TESTS) return;

        // Start first cluster
        File dir1 = new File(testDir, "cluster1");
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(dir1)
            .format(true)
            .build();

        FileSystem fs1 = cluster.getFileSystem();
        Path file1 = new Path("/file1.txt");
        try (FSDataOutputStream out = fs1.create(file1)) {
            out.writeUTF("Cluster 1");
        }

        cluster.shutdown(true);

        // Start second cluster
        File dir2 = new File(testDir, "cluster2");
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(dir2)
            .format(true)
            .build();

        FileSystem fs2 = cluster.getFileSystem();
        Path file2 = new Path("/file2.txt");
        try (FSDataOutputStream out = fs2.create(file2)) {
            out.writeUTF("Cluster 2");
        }

        // Second cluster should be independent
        assertFalse("File from first cluster should not exist",
            fs2.exists(new Path("/file1.txt")));
        assertTrue("File from second cluster should exist", fs2.exists(file2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetNameNodeThrowsException() throws Exception {
        if (SKIP_TESTS) {
            throw new UnsupportedOperationException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // This should throw UnsupportedOperationException
        cluster.getNameNode();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataNodeThrowsException() throws Exception {
        if (SKIP_TESTS) {
            throw new UnsupportedOperationException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // This should throw UnsupportedOperationException
        cluster.getDataNode(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataNodesThrowsException() throws Exception {
        if (SKIP_TESTS) {
            throw new UnsupportedOperationException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // This should throw UnsupportedOperationException
        cluster.getDataNodes();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInjectBlocksThrowsException() throws Exception {
        if (SKIP_TESTS) {
            throw new UnsupportedOperationException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // This should throw UnsupportedOperationException
        cluster.injectBlocks(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderValidation_NoDataNodes() throws Exception {
        // Should fail - numDataNodes must be at least 1
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(0)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderValidation_NoHadoopHome() throws Exception {
        // Should fail - no Hadoop distribution specified
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .build();
    }

    @Test
    public void testGetRpcAddress() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // Get RPC address
        assertNotNull("RPC address should not be null", cluster.getNameNodeRpcAddress());
        assertTrue("RPC port should be > 0", cluster.getNameNodeRpcAddress().getPort() > 0);
    }

    @Test
    public void testAutoCloseableInterface() throws Exception {
        if (SKIP_TESTS) return;

        // Use try-with-resources
        try (ProcessBasedMiniDFSCluster autoCluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build()) {

            assertTrue("Cluster should be up", autoCluster.isClusterUp());
            FileSystem fs = autoCluster.getFileSystem();
            assertNotNull(fs);
        }

        // Cluster should be shut down after try block
        // (We can't check this easily since cluster is out of scope)
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNameNodeIndex() throws Exception {
        if (SKIP_TESTS) {
            throw new IllegalArgumentException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // Should throw - invalid NN index
        cluster.getURI(999);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDataNodeIndex() throws Exception {
        if (SKIP_TESTS) {
            throw new IllegalArgumentException("Skipped");
        }

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        // Should throw - invalid DN index
        cluster.restartDataNode(999);
    }

    @Test
    public void testPortRangeConfiguration() throws Exception {
        if (SKIP_TESTS) return;

        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(1)
            .allNodesHadoopDistribution(HADOOP_HOME)
            .baseDir(testDir)
            .portRange(55000, 55999)
            .format(true)
            .build();

        int port = cluster.getNameNodeRpcAddress().getPort();
        assertTrue("Port should be in range", port >= 55000 && port <= 55999);
    }

    @Test
    public void testPerNodeHadoopDistribution() throws Exception {
        if (SKIP_TESTS) return;

        // This test verifies that per-node distribution configuration works
        // Even if all nodes use the same distribution, the mechanism should work
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .nameNodeHadoopDistribution(0, HADOOP_HOME)
            .dataNodeHadoopDistribution(0, HADOOP_HOME)
            .dataNodeHadoopDistribution(1, HADOOP_HOME)
            .baseDir(testDir)
            .format(true)
            .build();

        assertTrue("Cluster should be up", cluster.isClusterUp());
        assertEquals(2, cluster.getNumDataNodes());
    }
}

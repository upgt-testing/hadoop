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
package org.apache.hadoop.hdfs.restart;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.core.RestartMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for HdfsClusterAdapter.
 */
public class TestHdfsClusterAdapter {

    private MiniDFSCluster cluster;
    private Configuration conf;
    private HdfsClusterAdapter adapter;
    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .build();
        cluster.waitActive();
        fs = cluster.getFileSystem();
        adapter = new HdfsClusterAdapter();
    }

    @After
    public void tearDown() throws Exception {
        if (fs != null) {
            fs.close();
        }
        if (cluster != null) {
            cluster.shutdown();
        }
    }

    @Test
    public void testGetClusterType() {
        assertEquals(MiniDFSCluster.class, adapter.getClusterType());
    }

    @Test
    public void testGetNodeCountNameNode() throws Exception {
        int count = adapter.getNodeCount(cluster, "namenode");
        assertEquals(1, count);
    }

    @Test
    public void testGetNodeCountDataNode() throws Exception {
        int count = adapter.getNodeCount(cluster, "datanode");
        assertEquals(3, count);
    }

    @Test
    public void testGetNodeCountAll() throws Exception {
        int count = adapter.getNodeCount(cluster, "all");
        assertEquals(4, count); // 1 namenode + 3 datanodes
    }

    @Test
    public void testRoleNormalizationMaster() throws Exception {
        // "master" should be treated as "namenode"
        int count = adapter.getNodeCount(cluster, "master");
        assertEquals(1, count);
    }

    @Test
    public void testRoleNormalizationWorker() throws Exception {
        // "worker" should be treated as "datanode"
        int count = adapter.getNodeCount(cluster, "worker");
        assertEquals(3, count);
    }

    @Test
    public void testDataNodeGracefulRestart() throws Exception {
        // Write test data before restart
        Path testFile = new Path("/test-graceful.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }
        assertTrue(fs.exists(testFile));

        // Restart DataNode gracefully
        adapter.restartNode(cluster, "datanode", 0, RestartMode.GRACEFUL);

        // Verify cluster is still healthy
        adapter.waitActive(cluster);
        assertEquals(3, cluster.getDataNodes().size());
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testDataNodeCrashRestart() throws Exception {
        // Write test data before restart
        Path testFile = new Path("/test-crash.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }
        assertTrue(fs.exists(testFile));

        // Restart DataNode with crash mode
        adapter.restartNode(cluster, "datanode", 0, RestartMode.CRASH);

        // Verify cluster is still healthy
        adapter.waitActive(cluster);
        assertEquals(3, cluster.getDataNodes().size());
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testDataNodeDelayedCrashRestart() throws Exception {
        // Set a short delay for testing
        adapter.setCrashDelayMs(100);

        // Write test data before restart
        Path testFile = new Path("/test-delayed-crash.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }
        assertTrue(fs.exists(testFile));

        // Restart DataNode with delayed crash mode
        adapter.restartNode(cluster, "datanode", 0, RestartMode.DELAYED_CRASH);

        // Verify cluster is still healthy
        adapter.waitActive(cluster);
        assertEquals(3, cluster.getDataNodes().size());
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testNameNodeGracefulRestart() throws Exception {
        // Write test data before restart
        Path testFile = new Path("/test-nn-graceful.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }
        assertTrue(fs.exists(testFile));

        // Restart NameNode gracefully
        adapter.restartNode(cluster, "namenode", 0, RestartMode.GRACEFUL);

        // Verify cluster is still healthy and data persists
        adapter.waitActive(cluster);

        // Need to get a new FileSystem after NameNode restart
        fs.close();
        fs = cluster.getFileSystem();
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testNameNodeCrashRestart() throws Exception {
        // Write test data before restart
        Path testFile = new Path("/test-nn-crash.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }
        assertTrue(fs.exists(testFile));

        // Restart NameNode with crash mode
        adapter.restartNode(cluster, "namenode", 0, RestartMode.CRASH);

        // Verify cluster is still healthy and data persists
        adapter.waitActive(cluster);

        // Need to get a new FileSystem after NameNode restart
        fs.close();
        fs = cluster.getFileSystem();
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testWaitActive() throws Exception {
        // Just verify waitActive doesn't throw
        adapter.waitActive(cluster);
    }

    @Test
    public void testGetStateCapture() {
        assertNotNull(adapter.getStateCapture());
        assertTrue(adapter.getStateCapture() instanceof NoOpStateCapture);
    }

    @Test
    public void testGetHealthCheck() {
        assertNotNull(adapter.getHealthCheck());
        assertTrue(adapter.getHealthCheck() instanceof NoOpHealthCheck);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRole() throws Exception {
        adapter.getNodeCount(cluster, "invalid-role");
    }

    @Test
    public void testRestartAllDataNodes() throws Exception {
        // Write test data before restart
        Path testFile = new Path("/test-all-dn.txt");
        try (FSDataOutputStream out = fs.create(testFile)) {
            out.writeUTF("test data");
        }

        // Restart all DataNodes
        adapter.restartAllNodes(cluster, "datanode", RestartMode.GRACEFUL);

        // Verify all DataNodes are back
        adapter.waitActive(cluster);
        assertEquals(3, cluster.getDataNodes().size());
        assertTrue(fs.exists(testFile));
    }

    @Test
    public void testCrashDelayConfiguration() {
        assertEquals(500, adapter.getCrashDelayMs()); // default
        adapter.setCrashDelayMs(1000);
        assertEquals(1000, adapter.getCrashDelayMs());
    }
}

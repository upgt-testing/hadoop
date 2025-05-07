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
package org.apache.hadoop.hdfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.junit.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil.ShortCircuitTestContext;
import org.junit.Test;

public class TestRead {

    final private int BLOCK_SIZE = 512;

    private void testEOF(MiniDFSClusterInJVM cluster, int fileLength) throws IOException {
        FileSystem fs = cluster.getFileSystem();
        Path path = new Path("testEOF." + fileLength);
        DFSTestUtil.createFile(fs, path, fileLength, (short) 1, 0xBEEFBEEF);
        FSDataInputStream fis = fs.open(path);
        ByteBuffer empty = ByteBuffer.allocate(0);
        // A read into an empty bytebuffer at the beginning of the file gives 0.
        Assert.assertEquals(0, fis.read(empty));
        fis.seek(fileLength);
        // A read into an empty bytebuffer at the end of the file gives -1.
        Assert.assertEquals(-1, fis.read(empty));
        if (fileLength > BLOCK_SIZE) {
            fis.seek(fileLength - BLOCK_SIZE + 1);
            ByteBuffer dbb = ByteBuffer.allocateDirect(BLOCK_SIZE);
            Assert.assertEquals(BLOCK_SIZE - 1, fis.read(dbb));
        }
        fis.close();
    }

    @Test(timeout = 60000)
    public void testEOFWithBlockReaderLocal() throws Exception {
        ShortCircuitTestContext testContext = new ShortCircuitTestContext("testEOFWithBlockReaderLocal");
        try {
            final Configuration conf = testContext.newConfiguration();
            conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
            MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
            testEOF(cluster, 1);
            testEOF(cluster, 14);
            testEOF(cluster, 10000);
            cluster.shutdown();
        } finally {
            testContext.close();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithRemoteBlockReader() throws Exception {
        final Configuration conf = new Configuration();
        conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        testEOF(cluster, 1);
        testEOF(cluster, 14);
        testEOF(cluster, 10000);
        cluster.shutdown();
    }

    /**
     * Regression test for HDFS-7045.
     * If deadlock happen, the test will time out.
     * @throws Exception
     */
    @Test(timeout = 60000)
    public void testReadReservedPath() throws Exception {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        try {
            FileSystem fs = cluster.getFileSystem();
            fs.open(new Path("/.reserved/.inodes/file"));
            Assert.fail("Open a non existing file should fail.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            cluster.shutdown();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithBlockReaderLocal_withUpgrade20() throws Exception {
        ShortCircuitTestContext testContext = new ShortCircuitTestContext("testEOFWithBlockReaderLocal");
        try {
            final Configuration conf = testContext.newConfiguration();
            conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
            MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
            testEOF(cluster, 1);
            testEOF(cluster, 14);
            testEOF(cluster, 10000);
            cluster.shutdown();
        } finally {
            testContext.close();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithBlockReaderLocal_withUpgrade40() throws Exception {
        ShortCircuitTestContext testContext = new ShortCircuitTestContext("testEOFWithBlockReaderLocal");
        try {
            final Configuration conf = testContext.newConfiguration();
            conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
            MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
            testEOF(cluster, 1);
            testEOF(cluster, 14);
            testEOF(cluster, 10000);
            cluster.shutdown();
        } finally {
            testContext.close();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithBlockReaderLocal_withUpgrade60() throws Exception {
        ShortCircuitTestContext testContext = new ShortCircuitTestContext("testEOFWithBlockReaderLocal");
        try {
            final Configuration conf = testContext.newConfiguration();
            conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
            MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
            testEOF(cluster, 1);
            testEOF(cluster, 14);
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
            testEOF(cluster, 10000);
            cluster.shutdown();
        } finally {
            testContext.close();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithBlockReaderLocal_withUpgrade80() throws Exception {
        ShortCircuitTestContext testContext = new ShortCircuitTestContext("testEOFWithBlockReaderLocal");
        try {
            final Configuration conf = testContext.newConfiguration();
            conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
            MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
            testEOF(cluster, 1);
            testEOF(cluster, 14);
            testEOF(cluster, 10000);
            cluster.shutdown();
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
        } finally {
            testContext.close();
        }
    }

    @Test(timeout = 60000)
    public void testEOFWithRemoteBlockReader_withUpgrade20() throws Exception {
        final Configuration conf = new Configuration();
        conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        testEOF(cluster, 1);
        cluster.restartNodeForTesting(0);
        cluster.upgradeNodeForTesting(0);
        testEOF(cluster, 14);
        testEOF(cluster, 10000);
        cluster.shutdown();
    }

    @Test(timeout = 60000)
    public void testEOFWithRemoteBlockReader_withUpgrade40() throws Exception {
        final Configuration conf = new Configuration();
        conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        testEOF(cluster, 1);
        testEOF(cluster, 14);
        cluster.restartNodeForTesting(0);
        cluster.upgradeNodeForTesting(0);
        testEOF(cluster, 10000);
        cluster.shutdown();
    }

    @Test(timeout = 60000)
    public void testEOFWithRemoteBlockReader_withUpgrade60() throws Exception {
        final Configuration conf = new Configuration();
        conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        testEOF(cluster, 1);
        testEOF(cluster, 14);
        testEOF(cluster, 10000);
        cluster.restartNodeForTesting(0);
        cluster.upgradeNodeForTesting(0);
        cluster.shutdown();
    }

    @Test(timeout = 60000)
    public void testEOFWithRemoteBlockReader_withUpgrade80() throws Exception {
        final Configuration conf = new Configuration();
        conf.setLong(HdfsClientConfigKeys.DFS_CLIENT_CACHE_READAHEAD, BLOCK_SIZE);
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        testEOF(cluster, 1);
        testEOF(cluster, 14);
        testEOF(cluster, 10000);
        cluster.shutdown();
        cluster.restartNodeForTesting(0);
        cluster.upgradeNodeForTesting(0);
    }

    @Test(timeout = 60000)
    public void testReadReservedPath_withUpgrade20() throws Exception {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        try {
            FileSystem fs = cluster.getFileSystem();
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
            fs.open(new Path("/.reserved/.inodes/file"));
            Assert.fail("Open a non existing file should fail.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            cluster.shutdown();
        }
    }

    @Test(timeout = 60000)
    public void testReadReservedPath_withUpgrade40() throws Exception {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        try {
            FileSystem fs = cluster.getFileSystem();
            fs.open(new Path("/.reserved/.inodes/file"));
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
            Assert.fail("Open a non existing file should fail.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            cluster.shutdown();
        }
    }

    @Test(timeout = 60000)
    public void testReadReservedPath_withUpgrade60() throws Exception {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        try {
            FileSystem fs = cluster.getFileSystem();
            fs.open(new Path("/.reserved/.inodes/file"));
            Assert.fail("Open a non existing file should fail.");
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            cluster.shutdown();
        }
    }

    @Test(timeout = 60000)
    public void testReadReservedPath_withUpgrade80() throws Exception {
        Configuration conf = new Configuration();
        MiniDFSClusterInJVM cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(1).format(true).build();
        try {
            FileSystem fs = cluster.getFileSystem();
            fs.open(new Path("/.reserved/.inodes/file"));
            Assert.fail("Open a non existing file should fail.");
        } catch (FileNotFoundException e) {
            // Expected
        } finally {
            cluster.shutdown();
            cluster.restartNodeForTesting(0);
            cluster.upgradeNodeForTesting(0);
        }
    }
}

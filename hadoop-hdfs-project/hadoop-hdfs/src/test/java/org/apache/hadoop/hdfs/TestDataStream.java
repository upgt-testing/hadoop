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

import java.io.IOException;
import java.util.Random;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.GenericTestUtils.LogCapturer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDataStream {

    static MiniDockerDFSCluster cluster;

    static int PACKET_SIZE = 1024;

    @BeforeClass
    public static void setup() throws IOException {
        Configuration conf = new Configuration();
        conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_WRITE_PACKET_SIZE_KEY, PACKET_SIZE);
        conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SLOW_IO_WARNING_THRESHOLD_KEY, 10000);
        conf.setInt(HdfsClientConfigKeys.DFS_CLIENT_SOCKET_TIMEOUT_KEY, 60000);
        conf.setInt("dfs.client.block.write.retry.count", 10);
        conf.setInt("dfs.client.block.write.locateFollowingBlock.retries", 5);
        conf.setInt("dfs.replication", 2);
        conf.set("dfs.client.block.write.replace-datanode-on-failure.policy", "ALWAYS");
        cluster = new MiniDockerDFSCluster.Builder(conf).numDataNodes(3).build();
    }

    @Test
    public void testDfsClient() throws IOException, InterruptedException {
        LogCapturer logs = GenericTestUtils.LogCapturer.captureLogs(LoggerFactory.getLogger(DataStreamer.class));
        byte[] toWrite = new byte[PACKET_SIZE];
        new Random(1).nextBytes(toWrite);
        final Path path = new Path("/file1");
        final DistributedFileSystem dfs = cluster.getDistributedFileSystem();
        FSDataOutputStream out = null;
        final short repl = 2;
        out = dfs.create(path, repl);
        out.write(toWrite);
        out.write(toWrite);
        out.hflush();
        cluster.upgradeDatanode(0);
        // Wait to cross slow IO warning threshold
        Thread.sleep(15 * 1000);
        out.write(toWrite);
        out.write(toWrite);
        out.hflush();
        // Wait for capturing logs in busy cluster
        Thread.sleep(5 * 1000);
        out.close();
        logs.stopCapturing();
        GenericTestUtils.assertDoesNotMatch(logs.getOutput(), "Slow ReadProcessor read fields for block");
    }

    @AfterClass
    public static void tearDown() {
        cluster.shutdown();
    }
}

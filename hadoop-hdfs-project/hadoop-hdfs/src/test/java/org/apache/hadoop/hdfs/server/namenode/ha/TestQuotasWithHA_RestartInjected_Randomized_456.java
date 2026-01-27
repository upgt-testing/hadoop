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
package org.apache.hadoop.hdfs.server.namenode.ha;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HAUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.StandbyException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestQuotasWithHA_RestartInjected_Randomized_456 {

    private static final Path TEST_DIR = new Path("/test");

    private static final Path TEST_FILE = new Path(TEST_DIR, "file");

    private static final String TEST_DIR_STR = TEST_DIR.toUri().getPath();

    private static final long NS_QUOTA = 10000;

    private static final long DS_QUOTA = 10000;

    // 1KB blocks
    private static final long BLOCK_SIZE = 1024;

    private MiniDFSCluster cluster;

    private NameNode nn0;

    private NameNode nn1;

    private FileSystem fs;

    @Before
    public void setupCluster() throws Exception {
        Configuration conf = new Configuration();
        conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
        conf.setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);
        conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
        HAUtil.setAllowStandbyReads(conf, true);
        cluster = new MiniDFSCluster.Builder(conf).nnTopology(MiniDFSNNTopology.simpleHATopology()).numDataNodes(1).waitSafeMode(false).build();
        cluster.waitActive();
        nn0 = cluster.getNameNode(0);
        nn1 = cluster.getNameNode(1);
        fs = HATestUtil.configureFailoverFs(cluster, conf);
        cluster.transitionToActive(0);
    }

    @After
    public void shutdownCluster() throws IOException {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    /**
     * Test that quotas are properly tracked by the standby through
     * create, append, delete.
     */
    @Test
    public void testQuotasTrackedOnStandby() throws Exception {
        fs.mkdirs(TEST_DIR);
        RestartFramework.at("after_delete_quota").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        DistributedFileSystem dfs = (DistributedFileSystem) fs;
        dfs.setQuota(TEST_DIR, NS_QUOTA, DS_QUOTA);
        RestartFramework.at("after_file_creation_quota").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        long expectedSize = 3 * BLOCK_SIZE + BLOCK_SIZE / 2;
        DFSTestUtil.createFile(fs, TEST_FILE, expectedSize, (short) 1, 1L);
        HATestUtil.waitForStandbyToCatchUp(nn0, nn1);
        RestartFramework.at("after_first_verification_quota").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        ContentSummary cs = nn1.getRpcServer().getContentSummary(TEST_DIR_STR);
        assertEquals(NS_QUOTA, cs.getQuota());
        assertEquals(DS_QUOTA, cs.getSpaceQuota());
        assertEquals(expectedSize, cs.getSpaceConsumed());
        assertEquals(1, cs.getDirectoryCount());
        assertEquals(1, cs.getFileCount());
        // Append to the file and make sure quota is updated correctly.
        FSDataOutputStream stm = fs.append(TEST_FILE);
        try {
            byte[] data = new byte[(int) (BLOCK_SIZE * 3 / 2)];
            stm.write(data);
            expectedSize += data.length;
        } finally {
            IOUtils.closeStream(stm);
        }
        HATestUtil.waitForStandbyToCatchUp(nn0, nn1);
        cs = nn1.getRpcServer().getContentSummary(TEST_DIR_STR);
        RestartFramework.at("after_mkdir_quota").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(NS_QUOTA, cs.getQuota());
        assertEquals(DS_QUOTA, cs.getSpaceQuota());
        assertEquals(expectedSize, cs.getSpaceConsumed());
        RestartFramework.at("after_third_verification_quota").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_set_quota").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(1, cs.getDirectoryCount());
        RestartFramework.at("after_second_verification_quota").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(1, cs.getFileCount());
        fs.delete(TEST_FILE, true);
        expectedSize = 0;
        RestartFramework.at("after_append_quota").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        HATestUtil.waitForStandbyToCatchUp(nn0, nn1);
        cs = nn1.getRpcServer().getContentSummary(TEST_DIR_STR);
        assertEquals(NS_QUOTA, cs.getQuota());
        assertEquals(DS_QUOTA, cs.getSpaceQuota());
        assertEquals(expectedSize, cs.getSpaceConsumed());
        assertEquals(1, cs.getDirectoryCount());
        assertEquals(0, cs.getFileCount());
    }

    /**
     * Test that getContentSummary on Standby should should throw standby
     * exception.
     */
    @Test(expected = StandbyException.class)
    public void testGetContentSummaryOnStandby() throws Exception {
        Configuration nn1conf = cluster.getConfiguration(1);
        RestartFramework.at("before_content_summary_call").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // just reset the standby reads to default i.e False on standby.
        HAUtil.setAllowStandbyReads(nn1conf, false);
        RestartFramework.at("after_standby_config_change").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.restartNameNode(1);
        cluster.getNameNodeRpc(1).getContentSummary("/");
    }

    /**
     * Test that getQuotaUsage on Standby should should throw standby exception.
     */
    @Test(expected = StandbyException.class)
    public void testGetQuotaUsageOnStandby() throws Exception {
        RestartFramework.at("before_quota_usage_call").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Configuration nn1conf = cluster.getConfiguration(1);
        // just reset the standby reads to default i.e False on standby.
        HAUtil.setAllowStandbyReads(nn1conf, false);
        RestartFramework.at("after_quota_standby_config_change").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.restartNameNode(1);
        cluster.getNameNodeRpc(1).getQuotaUsage("/");
    }
}

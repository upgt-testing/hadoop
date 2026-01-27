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
package org.apache.hadoop.hdfs.tools;

import java.io.IOException;
import java.io.PrintStream;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.tools.GetGroupsTestBase;
import org.apache.hadoop.util.Tool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

/**
 * Tests for the HDFS implementation of {@link GetGroups}
 */
public class TestGetGroups_RestartInjected_Randomized_123 extends GetGroupsTestBase {

    private MiniDFSCluster cluster;

    @Before
    public void setUpNameNode() throws IOException {
        conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(0).build();
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @After
    public void tearDownNameNode() {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    @Override
    protected Tool getTool(PrintStream o) {
        return new GetGroups(conf, o);
    }

    @Test
    @Override
    public void testNoUserGiven() throws Exception {
        RestartFramework.at("after_get_groups_no_user").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testNoUserGiven();
    }

    @Test
    @Override
    public void testExistingUser() throws Exception {
        super.testExistingUser();
        RestartFramework.at("after_get_groups_existing_user").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMultipleExistingUsers() throws Exception {
        RestartFramework.at("after_get_groups_multiple_users").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMultipleExistingUsers();
    }

    @Test
    @Override
    public void testNonExistentUser() throws Exception {
        RestartFramework.at("after_get_groups_nonexistent_user").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testNonExistentUser();
    }

    @Test
    @Override
    public void testMultipleNonExistingUsers() throws Exception {
        super.testMultipleNonExistingUsers();
        RestartFramework.at("after_get_groups_multiple_nonexistent").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testExistingInterleavedWithNonExistentUsers() throws Exception {
        super.testExistingInterleavedWithNonExistentUsers();
        RestartFramework.at("after_get_groups_interleaved_users").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }
}

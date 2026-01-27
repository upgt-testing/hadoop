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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.tools.GetGroups;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.tools.GetGroupsTestBase;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import static org.junit.Assert.assertEquals;

public class TestGetGroupsWithHA_RestartInjected_Randomized_456 extends GetGroupsTestBase {

    private MiniDFSCluster cluster;

    private UserGroupInformation testUser1;

    private UserGroupInformation testUser2;

    @Before
    public void setUpNameNode() throws IOException {
        conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).nnTopology(MiniDFSNNTopology.simpleHATopology()).numDataNodes(0).build();
        HATestUtil.setFailoverConfigurations(cluster, conf);
        UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
        UserGroupInformation.createUserForTesting(currentUser.getUserName(), currentUser.getGroupNames());
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        testUser1 = UserGroupInformation.createUserForTesting("foo", new String[] { "bar", "baz" });
        testUser2 = UserGroupInformation.createUserForTesting("fiz", new String[] { "buz", "boz" });
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
    public void testNoUserGiven() throws Exception {
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        String actualOutput = runTool(conf, new String[0], true);
        UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
        assertEquals("No user provided should default to current user", getExpectedOutput(currentUser), actualOutput);
    }

    @Test
    public void testExistingUser() throws Exception {
        String actualOutput = runTool(conf, new String[] { testUser1.getUserName() }, true);
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Show only the output of the user given", getExpectedOutput(testUser1), actualOutput);
    }

    @Test
    public void testMultipleExistingUsers() throws Exception {
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        String actualOutput = runTool(conf, new String[] { testUser1.getUserName(), testUser2.getUserName() }, true);
        assertEquals("Show the output for both users given", getExpectedOutput(testUser1) + getExpectedOutput(testUser2), actualOutput);
    }

    @Test
    public void testNonExistentUser() throws Exception {
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        String actualOutput = runTool(conf, new String[] { "does-not-exist" }, true);
        assertEquals("Show the output for only the user given, with no groups", getExpectedOutput(UserGroupInformation.createRemoteUser("does-not-exist")), actualOutput);
    }

    @Test
    public void testMultipleNonExistingUsers() throws Exception {
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        String actualOutput = runTool(conf, new String[] { "does-not-exist1", "does-not-exist2" }, true);
        assertEquals("Show the output for only the user given, with no groups", getExpectedOutput(UserGroupInformation.createRemoteUser("does-not-exist1")) + getExpectedOutput(UserGroupInformation.createRemoteUser("does-not-exist2")), actualOutput);
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    public void testExistingInterleavedWithNonExistentUsers() throws Exception {
        RestartFramework.at("after_tool_execution").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        String actualOutput = runTool(conf, new String[] { "does-not-exist1", testUser1.getUserName(), "does-not-exist2", testUser2.getUserName() }, true);
        RestartFramework.at("before_tool_execution").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Show the output for only the user given, with no groups", getExpectedOutput(UserGroupInformation.createRemoteUser("does-not-exist1")) + getExpectedOutput(testUser1) + getExpectedOutput(UserGroupInformation.createRemoteUser("does-not-exist2")) + getExpectedOutput(testUser2), actualOutput);
    }

    private static String getExpectedOutput(UserGroupInformation user) {
        String expectedOutput = user.getUserName() + " :";
        for (String group : user.getGroupNames()) {
            expectedOutput += " " + group;
        }
        return expectedOutput + System.getProperty("line.separator");
    }

    private String runTool(Configuration conf, String[] args, boolean success) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(o, true);
        try {
            int ret = ToolRunner.run(getTool(out), args);
            assertEquals(success, ret == 0);
            return o.toString();
        } finally {
            o.close();
            out.close();
        }
    }
}

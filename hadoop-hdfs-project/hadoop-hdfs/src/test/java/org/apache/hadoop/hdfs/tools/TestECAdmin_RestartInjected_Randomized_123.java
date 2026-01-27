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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.SystemErasureCodingPolicies;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

/**
 * Tests some ECAdmin scenarios that are hard to test from
 * {@link org.apache.hadoop.cli.TestErasureCodingCLI}.
 */
public class TestECAdmin_RestartInjected_Randomized_123 {

    public static final Logger LOG = LoggerFactory.getLogger(TestECAdmin_RestartInjected.class);

    private Configuration conf = new Configuration();

    private MiniDFSCluster cluster;

    private ECAdmin admin = new ECAdmin(conf);

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private static final PrintStream OLD_OUT = System.out;

    private static final PrintStream OLD_ERR = System.err;

    private final static String RS_3_2 = SystemErasureCodingPolicies.getByID(SystemErasureCodingPolicies.RS_3_2_POLICY_ID).getName();

    private final static String RS_6_3 = SystemErasureCodingPolicies.getByID(SystemErasureCodingPolicies.RS_6_3_POLICY_ID).getName();

    private final static String RS_10_4 = SystemErasureCodingPolicies.getByID(SystemErasureCodingPolicies.RS_10_4_POLICY_ID).getName();

    private final static String XOR_2_1 = SystemErasureCodingPolicies.getByID(SystemErasureCodingPolicies.XOR_2_1_POLICY_ID).getName();

    @Before
    public void setup() throws Exception {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @After
    public void tearDown() throws Exception {
        try {
            System.out.flush();
            System.err.flush();
            resetOutputs();
        } finally {
            System.setOut(OLD_OUT);
            System.setErr(OLD_ERR);
        }
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    @Test
    public void testRS63MinDN() throws Exception {
        final int numDataNodes = 6;
        final int numRacks = 3;
        final int expectedNumDataNodes = 9;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        int ret = runCommandWithParams("-verifyClusterSetup");
        assertEquals("Return value of the command is not successful", 2, ret);
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertNotEnoughDataNodesMessage(RS_6_3, numDataNodes, expectedNumDataNodes);
    }

    @Test
    public void testRS104MinRacks() throws Exception {
        final String testPolicy = RS_10_4;
        final int numDataNodes = 15;
        final int numRacks = 3;
        final int expectedNumRacks = 4;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_policy_change").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        cluster.getFileSystem().enableErasureCodingPolicy(testPolicy);
        int ret = runCommandWithParams("-verifyClusterSetup");
        assertEquals("Return value of the command is not successful", 2, ret);
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertNotEnoughRacksMessage(testPolicy, numRacks, expectedNumRacks);
    }

    @Test
    public void testXOR21MinRacks() throws Exception {
        final String testPolicy = XOR_2_1;
        final int numDataNodes = 5;
        final int numRacks = 2;
        final int expectedNumRacks = 3;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.getFileSystem().enableErasureCodingPolicy(testPolicy);
        int ret = runCommandWithParams("-verifyClusterSetup");
        RestartFramework.at("after_policy_change").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Return value of the command is not successful", 2, ret);
        assertNotEnoughRacksMessage(testPolicy, numRacks, expectedNumRacks);
    }

    @Test
    public void testRS32MinRacks() throws Exception {
        RestartFramework.at("after_policy_change").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final String testPolicy = RS_3_2;
        final int numDataNodes = 5;
        final int numRacks = 2;
        final int expectedNumRacks = 3;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        cluster.getFileSystem().enableErasureCodingPolicy(testPolicy);
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        int ret = runCommandWithParams("-verifyClusterSetup");
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Return value of the command is not successful", 2, ret);
        assertNotEnoughRacksMessage(testPolicy, numRacks, expectedNumRacks);
    }

    @Test
    public void testRS63Good() throws Exception {
        cluster = DFSTestUtil.setupCluster(conf, 9, 3, 0);
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        int ret = runCommandWithParams("-verifyClusterSetup");
        assertEquals("Return value of the command is successful", 0, ret);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue("Result of cluster topology verify " + "should be logged correctly", out.toString().contains("The cluster setup can support EC policies: " + RS_6_3));
        assertTrue("Error output should be empty", err.toString().isEmpty());
    }

    @Test
    public void testNoECEnabled() throws Exception {
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster = DFSTestUtil.setupCluster(conf, 9, 3, 0);
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        int ret = runCommandWithParams("-verifyClusterSetup");
        assertEquals("Return value of the command is successful", 0, ret);
        assertTrue("Result of cluster topology verify " + "should be logged correctly", out.toString().contains("No erasure coding policy is given"));
        RestartFramework.at("after_disable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_verify_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue("Error output should be empty", err.toString().isEmpty());
    }

    @Test
    public void testUnsuccessfulEnablePolicyMessage() throws Exception {
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final String testPolicy = RS_3_2;
        RestartFramework.at("after_disable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final int numDataNodes = 5;
        RestartFramework.at("after_enable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final int numRacks = 2;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        final int ret = runCommandWithParams("-enablePolicy", "-policy", testPolicy);
        assertEquals("Return value of the command is successful", 0, ret);
        assertTrue("Enabling policy should be logged", out.toString().contains("Erasure coding policy " + testPolicy + " is enabled"));
        assertTrue("Warning about cluster topology should be printed", err.toString().contains("Warning: The cluster setup does not support " + "EC policy " + testPolicy + ". Reason:"));
        assertTrue("Warning about cluster topology should be printed", err.toString().contains(" racks are required for the erasure coding policies: " + testPolicy));
    }

    @Test
    public void testSuccessfulEnablePolicyMessage() throws Exception {
        final String testPolicy = RS_3_2;
        cluster = DFSTestUtil.setupCluster(conf, 5, 3, 0);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        RestartFramework.at("after_disable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final int ret = runCommandWithParams("-enablePolicy", "-policy", testPolicy);
        assertEquals("Return value of the command is successful", 0, ret);
        RestartFramework.at("after_enable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue("Enabling policy should be logged", out.toString().contains("Erasure coding policy " + testPolicy + " is enabled"));
        assertFalse("Warning about cluster topology should not be printed", out.toString().contains("Warning: The cluster setup does not support"));
        assertTrue("Error output should be empty", err.toString().isEmpty());
    }

    @Test
    public void testEnableNonExistentPolicyMessage() throws Exception {
        RestartFramework.at("after_enable_policy_attempt").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster = DFSTestUtil.setupCluster(conf, 5, 3, 0);
        RestartFramework.at("after_disable_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.getFileSystem().disableErasureCodingPolicy(RS_6_3);
        final int ret = runCommandWithParams("-enablePolicy", "-policy", "NonExistentPolicy");
        assertEquals("Return value of the command is unsuccessful", 2, ret);
        assertFalse("Enabling policy should not be logged when " + "it was unsuccessful", out.toString().contains("is enabled"));
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue("Error message should be printed", err.toString().contains("RemoteException: The policy name " + "NonExistentPolicy does not exist"));
    }

    @Test
    public void testVerifyClusterSetupWithGivenPolicies() throws Exception {
        final int numDataNodes = 5;
        final int numRacks = 2;
        cluster = DFSTestUtil.setupCluster(conf, numDataNodes, numRacks, 0);
        RestartFramework.at("after_verify_multiple_policies").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        int ret = runCommandWithParams("-verifyClusterSetup", "-policy", RS_3_2);
        RestartFramework.at("after_verify_rs32").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_cluster_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Return value of the command is not successful", 2, ret);
        assertNotEnoughRacksMessage(RS_3_2, numRacks, 3);
        resetOutputs();
        ret = runCommandWithParams("-verifyClusterSetup", "-policy", RS_10_4, RS_3_2);
        RestartFramework.at("after_verify_invalid_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("Return value of the command is not successful", 2, ret);
        assertNotEnoughDataNodesMessage(RS_10_4 + ", " + RS_3_2, numDataNodes, 14);
        resetOutputs();
        RestartFramework.at("after_verify_no_policy_arg").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ret = runCommandWithParams("-verifyClusterSetup", "-policy", "invalidPolicy");
        assertEquals("Return value of the command is not successful", -1, ret);
        assertTrue("Error message should be logged", err.toString().contains("The given erasure coding policy invalidPolicy " + "does not exist."));
        resetOutputs();
        ret = runCommandWithParams("-verifyClusterSetup", "-policy");
        assertEquals("Return value of the command is not successful", -1, ret);
        assertTrue("Error message should be logged", err.toString().contains("NotEnoughArgumentsException: Not enough arguments: " + "expected 1 but got 0"));
    }

    private void resetOutputs() {
        out.reset();
        err.reset();
    }

    private void assertNotEnoughDataNodesMessage(String policy, int numDataNodes, int expectedNumDataNodes) {
        assertTrue("Result of cluster topology verify " + "should be logged correctly", out.toString().contains(expectedNumDataNodes + " DataNodes are required " + "for the erasure coding policies: " + policy + ". The number of DataNodes is only " + numDataNodes));
        assertTrue("Error output should be empty", err.toString().isEmpty());
    }

    private void assertNotEnoughRacksMessage(String policy, int numRacks, int expectedNumRacks) {
        assertTrue("Result of cluster topology verify " + "should be logged correctly", out.toString().contains(expectedNumRacks + " racks are required for " + "the erasure coding policies: " + policy + ". The number of racks is only " + numRacks));
        assertTrue("Error output should be empty", err.toString().isEmpty());
    }

    private int runCommandWithParams(String... args) throws Exception {
        final int ret = admin.run(args);
        LOG.info("Command stdout: {}", out.toString());
        LOG.info("Command stderr: {}", err.toString());
        return ret;
    }
}

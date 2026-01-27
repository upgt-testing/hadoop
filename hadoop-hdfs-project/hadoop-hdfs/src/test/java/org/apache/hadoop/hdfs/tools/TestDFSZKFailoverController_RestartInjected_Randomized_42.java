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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_BIND_HOST_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ha.ClientBaseWithFixes;
import org.apache.hadoop.ha.HAServiceProtocol.HAServiceState;
import org.apache.hadoop.ha.HAServiceProtocol.RequestSource;
import org.apache.hadoop.ha.HAServiceProtocol.StateChangeRequestInfo;
import org.apache.hadoop.ha.HealthMonitor;
import org.apache.hadoop.ha.TestNodeFencer.AlwaysSucceedFencer;
import org.apache.hadoop.ha.ZKFCTestUtil;
import org.apache.hadoop.ha.ZKFailoverController;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.server.namenode.EditLogFileOutputStream;
import org.apache.hadoop.hdfs.server.namenode.MockNameNodeResourceChecker;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.ha.HATestUtil;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.net.ServerSocketUtil;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.LambdaTestUtils;
import org.apache.hadoop.test.MultithreadedTestUtil.TestContext;
import org.apache.hadoop.test.MultithreadedTestUtil.TestingThread;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.function.Supplier;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestDFSZKFailoverController_RestartInjected_Randomized_42 extends ClientBaseWithFixes {

    private static final String LOCALHOST_SERVER_ADDRESS = "127.0.0.1";

    private static final String WILDCARD_ADDRESS = "0.0.0.0";

    private Configuration conf;

    private MiniDFSCluster cluster;

    private TestContext ctx;

    private ZKFCThread thr1, thr2;

    private FileSystem fs;

    static {
        // Make tests run faster by avoiding fsync()
        EditLogFileOutputStream.setShouldSkipFsyncForTesting(true);
    }

    @Before
    public void setup() throws Exception {
        conf = new Configuration();
        // Specify the quorum per-nameservice, to ensure that these configs
        // can be nameservice-scoped.
        conf.set(ZKFailoverController.ZK_QUORUM_KEY + ".ns1", hostPort);
        conf.set(DFSConfigKeys.DFS_HA_FENCE_METHODS_KEY, AlwaysSucceedFencer.class.getName());
        conf.setBoolean(DFSConfigKeys.DFS_HA_AUTO_FAILOVER_ENABLED_KEY, true);
        // Turn off IPC client caching, so that the suite can handle
        // the restart of the daemons between test cases.
        conf.setInt(CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
        // Get random port numbers in advance. Because ZKFCs and DFSHAAdmin
        // needs rpc port numbers of all ZKFCs, Setting 0 does not work here.
        conf.setInt(DFSConfigKeys.DFS_HA_ZKFC_PORT_KEY + ".ns1.nn1", ServerSocketUtil.getPort(10023, 100));
        conf.setInt(DFSConfigKeys.DFS_HA_ZKFC_PORT_KEY + ".ns1.nn2", ServerSocketUtil.getPort(10024, 100));
    }

    private void startCluster() throws Exception {
        // prefer non-ephemeral port to avoid port collision on restartNameNode
        MiniDFSNNTopology topology = new MiniDFSNNTopology().addNameservice(new MiniDFSNNTopology.NSConf("ns1").addNN(new MiniDFSNNTopology.NNConf("nn1").setIpcPort(ServerSocketUtil.getPort(10021, 100))).addNN(new MiniDFSNNTopology.NNConf("nn2").setIpcPort(ServerSocketUtil.getPort(10022, 100))));
        cluster = new MiniDFSCluster.Builder(conf).nnTopology(topology).numDataNodes(0).build();
        cluster.waitActive();
        ctx = new TestContext();
        ctx.addThread(thr1 = new ZKFCThread(ctx, 0));
        assertEquals(0, thr1.zkfc.run(new String[] { "-formatZK" }));
        thr1.start();
        waitForHAState(0, HAServiceState.ACTIVE);
        ctx.addThread(thr2 = new ZKFCThread(ctx, 1));
        thr2.start();
        // Wait for the ZKFCs to fully start up
        ZKFCTestUtil.waitForHealthState(thr1.zkfc, HealthMonitor.State.SERVICE_HEALTHY, ctx);
        ZKFCTestUtil.waitForHealthState(thr2.zkfc, HealthMonitor.State.SERVICE_HEALTHY, ctx);
        fs = HATestUtil.configureFailoverFs(cluster, conf);
    }

    @After
    public void shutdown() throws Exception {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
        if (thr1 != null) {
            thr1.interrupt();
            thr1 = null;
        }
        if (thr2 != null) {
            thr2.interrupt();
            thr2 = null;
        }
        if (ctx != null) {
            ctx.stop();
            ctx = null;
        }
    }

    @Test
    public /**
     * Ensure the cluster simply starts with a hdfs jceks credential provider
     * configured. HDFS-14013.
     */
    void testZFFCStartsWithCredentialProviderReferencingHDFS() throws Exception {
        // Create a provider path on HDFS
        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, "jceks://hdfs/tmp/test.jceks");
        //
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    /**
     * Test that thread dump is captured after NN state changes.
     */
    @Test
    public void testThreadDumpCaptureAfterNNStateChange() throws Exception {
        RestartFramework.at("after_resource_check_set").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        startCluster();
        MockNameNodeResourceChecker mockResourceChecker = new MockNameNodeResourceChecker(conf);
        RestartFramework.at("after_standby_transition").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        mockResourceChecker.setResourcesAvailable(false);
        cluster.getNameNode(0).getNamesystem().setNNResourceChecker(mockResourceChecker);
        waitForHAState(0, HAServiceState.STANDBY);
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        while (!thr1.zkfc.isThreadDumpCaptured()) {
            Thread.sleep(1000);
        }
    }

    /**
     * Test that automatic failover is triggered by shutting the
     * active NN down.
     */
    @Test
    public void testFailoverAndBackOnNNShutdown() throws Exception {
        startCluster();
        Path p1 = new Path("/dir1");
        Path p2 = new Path("/dir2");
        // Write some data on the first NN
        fs.mkdirs(p1);
        RestartFramework.at("after_mkdir_p1").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // Shut it down, causing automatic failover
        cluster.shutdownNameNode(0);
        RestartFramework.at("after_shutdown_nn0").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        // Data should still exist. Write some on the new NN
        assertTrue(fs.exists(p1));
        fs.mkdirs(p2);
        assertEquals(AlwaysSucceedFencer.getLastFencedService().getAddress(), thr1.zkfc.getLocalTarget().getAddress());
        // Start the first node back up
        cluster.restartNameNode(0);
        RestartFramework.at("after_shutdown_nn1").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_mkdir_p2").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // This should have no effect -- the new node should be STANDBY.
        waitForHAState(0, HAServiceState.STANDBY);
        assertTrue(fs.exists(p1));
        RestartFramework.at("after_restart_nn0").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue(fs.exists(p2));
        // Shut down the second node, which should failback to the first
        cluster.shutdownNameNode(1);
        RestartFramework.at("after_failback_active").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        waitForHAState(0, HAServiceState.ACTIVE);
        // First node should see what was written on the second node while it was down.
        assertTrue(fs.exists(p1));
        assertTrue(fs.exists(p2));
        assertEquals(AlwaysSucceedFencer.getLastFencedService().getAddress(), thr2.zkfc.getLocalTarget().getAddress());
    }

    @Test
    public void testManualFailover() throws Exception {
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_graceful_failover_nn1").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        thr2.zkfc.getLocalTarget().getZKFCProxy(conf, 15000).gracefulFailover();
        waitForHAState(0, HAServiceState.STANDBY);
        RestartFramework.at("after_graceful_failover_nn2").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        waitForHAState(1, HAServiceState.ACTIVE);
        thr1.zkfc.getLocalTarget().getZKFCProxy(conf, 15000).gracefulFailover();
        waitForHAState(0, HAServiceState.ACTIVE);
        waitForHAState(1, HAServiceState.STANDBY);
    }

    @Test
    public void testWithoutBindAddressSet() throws Exception {
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        DFSZKFailoverController zkfc = DFSZKFailoverController.create(conf);
        assertEquals("Bind address not expected to be wildcard by default.", zkfc.getRpcAddressToBindTo().getHostString(), LOCALHOST_SERVER_ADDRESS);
    }

    @Test
    public void testWithBindAddressSet() throws Exception {
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        conf.set(DFS_NAMENODE_SERVICE_RPC_BIND_HOST_KEY, WILDCARD_ADDRESS);
        DFSZKFailoverController zkfc = DFSZKFailoverController.create(conf);
        String addr = zkfc.getRpcAddressToBindTo().getHostString();
        assertEquals("Bind address " + addr + " is not wildcard.", addr, WILDCARD_ADDRESS);
    }

    /**
     * Tests that a Namenode in Observer state rejects any state transition
     * request from ZKFC, as a result of namenode's participation in the ZK
     * election before getting transition to Observer state, since Observer
     * Namenode isn't supposed to participate in ZKFC elections.
     */
    @Test
    public void testObserverRejectZkfcCall() throws Exception {
        RestartFramework.at("after_transition_to_observer").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        startCluster();
        NamenodeProtocols nn1 = cluster.getNameNode(1).getRpcServer();
        nn1.transitionToObserver(new StateChangeRequestInfo(RequestSource.REQUEST_BY_USER_FORCED));
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        waitForHAState(1, HAServiceState.OBSERVER);
        LambdaTestUtils.intercept(AccessControlException.class, "denied since the namenode is in Observer state.", () -> nn1.transitionToStandby(new StateChangeRequestInfo(RequestSource.REQUEST_BY_ZKFC)));
    }

    @Test
    public void testManualFailoverWithDFSHAAdmin() throws Exception {
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        DFSHAAdmin tool = new DFSHAAdmin();
        tool.setConf(conf);
        assertEquals(0, tool.run(new String[] { "-failover", "nn1", "nn2" }));
        waitForHAState(0, HAServiceState.STANDBY);
        RestartFramework.at("after_failover_nn1_to_nn2").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_transition_to_standby").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_failover_nn2_to_nn1").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        waitForHAState(1, HAServiceState.ACTIVE);
        assertEquals(0, tool.run(new String[] { "-failover", "nn2", "nn1" }));
        waitForHAState(0, HAServiceState.ACTIVE);
        waitForHAState(1, HAServiceState.STANDBY);
        // Answer "yes" to the prompt for --forcemanual
        InputStream inOriginial = System.in;
        System.setIn(new ByteArrayInputStream("yes\n".getBytes()));
        int result = tool.run(new String[] { "-transitionToObserver", "-forcemanual", "nn2" });
        assertEquals("State transition returned: " + result, 0, result);
        RestartFramework.at("after_transition_to_observer").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
        waitForHAState(1, HAServiceState.OBSERVER);
        // Answer "yes" to the prompt for --forcemanual
        System.setIn(new ByteArrayInputStream("yes\n".getBytes()));
        result = tool.run(new String[] { "-transitionToStandby", "-forcemanual", "nn2" });
        System.setIn(inOriginial);
        assertEquals("State transition returned: " + result, 0, result);
        waitForHAState(1, HAServiceState.STANDBY);
    }

    @Test
    public void testElectionOnObserver() throws Exception {
        startCluster();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        InputStream inOriginial = System.in;
        try {
            DFSHAAdmin tool = new DFSHAAdmin();
            tool.setConf(conf);
            // Transition nn2 to Observer
            System.setIn(new ByteArrayInputStream("yes\n".getBytes()));
            int result = tool.run(new String[] { "-transitionToObserver", "-forcemanual", "nn2" });
            assertEquals("State transition returned: " + result, 0, result);
            RestartFramework.at("after_transition_to_observer").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            waitForHAState(1, HAServiceState.OBSERVER);
            waitForZKFCState(thr2.zkfc, HAServiceState.OBSERVER);
            // Call recheckElectability
            thr2.zkfc.getLocalTarget().getZKFCProxy(conf, 15000).cedeActive(-1);
            RestartFramework.at("after_cede_active").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            // This namenode is in observer state, it shouldn't join election
            assertEquals(false, thr2.zkfc.getElectorForTests().getWantToBeInElection());
        } finally {
            System.setIn(inOriginial);
        }
    }

    private void waitForHAState(int nnidx, final HAServiceState state) throws TimeoutException, InterruptedException {
        final NameNode nn = cluster.getNameNode(nnidx);
        GenericTestUtils.waitFor(new Supplier<Boolean>() {

            @Override
            public Boolean get() {
                try {
                    return nn.getRpcServer().getServiceStatus().getState() == state;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }, 50, 15000);
    }

    private void waitForZKFCState(DFSZKFailoverController zkfc, final HAServiceState state) throws TimeoutException, InterruptedException {
        GenericTestUtils.waitFor(() -> zkfc.getServiceState() == state, 50, 15000);
    }

    /**
     * Test-thread which runs a ZK Failover Controller corresponding
     * to a given NameNode in the minicluster.
     */
    private class ZKFCThread extends TestingThread {

        private final DFSZKFailoverController zkfc;

        public ZKFCThread(TestContext ctx, int idx) {
            super(ctx);
            this.zkfc = DFSZKFailoverController.create(cluster.getConfiguration(idx));
        }

        @Override
        public void doWork() throws Exception {
            try {
                assertEquals(0, zkfc.run(new String[0]));
            } catch (InterruptedException ie) {
                // Interrupted by main thread, that's OK.
            }
        }
    }
}

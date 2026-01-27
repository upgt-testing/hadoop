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
package org.apache.hadoop.hdfs.server.datanode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.hdfs.MiniDFSNNTopology.NNConf;
import org.apache.hadoop.hdfs.MiniDFSNNTopology.NSConf;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import org.apache.hadoop.thirdparty.com.google.common.base.Joiner;
import org.apache.hadoop.thirdparty.com.google.common.collect.Sets;

/**
 * Tests datanode refresh namenode list functionality.
 */
public class TestRefreshNamenodes_RestartInjected_Randomized_123 {

    private final int nnPort1 = 2221;

    private final int nnPort2 = 2224;

    private final int nnPort3 = 2227;

    private final int nnPort4 = 2230;

    @Test
    public void testRefreshNamenodes() throws IOException {
        Configuration conf = new Configuration();
        MiniDFSCluster cluster = null;
        try {
            MiniDFSNNTopology topology = new MiniDFSNNTopology().addNameservice(new NSConf("ns1").addNN(new NNConf(null).setIpcPort(nnPort1))).setFederation(true);
            cluster = new MiniDFSCluster.Builder(conf).nnTopology(topology).build();
            RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DataNode dn = cluster.getDataNodes().get(0);
            assertEquals(1, dn.getAllBpOs().size());
            cluster.addNameNode(conf, nnPort2);
            RestartFramework.at("after_add_namenode_2").on(cluster).restart("namenode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            assertEquals(2, dn.getAllBpOs().size());
            cluster.addNameNode(conf, nnPort3);
            RestartFramework.at("after_add_namenode_3").on(cluster).restart("namenode").withIndex(2).withMode(RestartMode.GRACEFUL).execute();
            assertEquals(3, dn.getAllBpOs().size());
            cluster.addNameNode(conf, nnPort4);
            RestartFramework.at("after_add_namenode_4").on(cluster).restart("namenode").withIndex(3).withMode(RestartMode.GRACEFUL).execute();
            Set<InetSocketAddress> nnAddrsFromCluster = Sets.newHashSet();
            for (int i = 0; i < 4; i++) {
                assertTrue(nnAddrsFromCluster.add(cluster.getNameNode(i).getNameNodeAddress()));
            }
            Set<InetSocketAddress> nnAddrsFromDN = Sets.newHashSet();
            for (BPOfferService bpos : dn.getAllBpOs()) {
                for (BPServiceActor bpsa : bpos.getBPServiceActors()) {
                    assertTrue(nnAddrsFromDN.add(bpsa.getNNSocketAddress()));
                }
            }
            assertEquals("", Joiner.on(",").join(Sets.symmetricDifference(nnAddrsFromCluster, nnAddrsFromDN)));
        } finally {
            if (cluster != null) {
                cluster.shutdown();
            }
        }
    }

    @Test
    public void testRefreshNameNodeDeadLock() throws Exception {
        Configuration conf = new HdfsConfiguration();
        MiniDFSCluster cluster = null;
        try {
            cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();
            cluster.waitActive();
            RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DataNodeFaultInjector.set(new DataNodeFaultInjector() {

                @Override
                public void delayWhenOfferServiceHoldLock() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            RestartFramework.at("after_fault_injector_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DataNode dn = cluster.getDataNodes().get(0);
            Configuration dnConf = dn.getConf();
            dnConf.set(DFSConfigKeys.DFS_NAMESERVICES, "ns1");
            dnConf.set(DFSConfigKeys.DFS_NAMENODE_LIFELINE_RPC_ADDRESS_KEY + ".ns1", "mock:8022");
            dn.refreshNamenodes(dnConf);
            RestartFramework.at("after_refresh_namenodes").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        } finally {
            if (cluster != null) {
                cluster.shutdown();
            }
        }
    }
}

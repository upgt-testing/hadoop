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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.RollingUpgradeAction;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.hdfs.protocol.RollingUpgradeInfo;
import org.apache.hadoop.hdfs.qjournal.MiniQJMHACluster;
import org.apache.hadoop.hdfs.server.namenode.NNStorage;
import org.apache.hadoop.hdfs.server.namenode.NameNodeLayoutVersion;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import org.junit.Assert;
import org.junit.Test;

public class TestRollingUpgradeDowngrade_RestartInjected_Randomized_42 {

    /**
     * Downgrade option is already obsolete. It should throw exception.
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDowngrade() throws Exception {
        final Configuration conf = new HdfsConfiguration();
        MiniQJMHACluster cluster = null;
        final Path foo = new Path("/foo");
        final Path bar = new Path("/bar");
        try {
            cluster = new MiniQJMHACluster.Builder(conf).build();
            MiniDFSCluster dfsCluster = cluster.getDfsCluster();
            dfsCluster.waitActive();
            RestartFramework.at("after_cluster_creation").on(dfsCluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            // let NN1 tail editlog every 1s
            dfsCluster.getConfiguration(1).setInt(DFSConfigKeys.DFS_HA_TAILEDITS_PERIOD_KEY, 1);
            dfsCluster.restartNameNode(1);
            RestartFramework.at("after_namenode_restart").on(dfsCluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            dfsCluster.transitionToActive(0);
            RestartFramework.at("after_active_transition").on(dfsCluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DistributedFileSystem dfs = dfsCluster.getFileSystem(0);
            dfs.mkdirs(foo);
            RestartFramework.at("after_foo_creation").on(dfsCluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            // start rolling upgrade
            RollingUpgradeInfo info = dfs.rollingUpgrade(RollingUpgradeAction.PREPARE);
            Assert.assertTrue(info.isStarted());
            RestartFramework.at("after_upgrade_prepare").on(dfsCluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            dfs.mkdirs(bar);
            RestartFramework.at("after_bar_creation").on(dfsCluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            TestRollingUpgrade.queryForPreparation(dfs);
            RestartFramework.at("after_query_preparation").on(dfsCluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            dfs.close();
            dfsCluster.restartNameNode(0, true, "-rollingUpgrade", "downgrade");
            // Once downgraded, there should be no more fsimage for rollbacks.
            Assert.assertFalse(dfsCluster.getNamesystem(0).getFSImage().hasRollbackFSImage());
            // shutdown NN1
            dfsCluster.shutdownNameNode(1);
            dfsCluster.transitionToActive(0);
            dfs = dfsCluster.getFileSystem(0);
            Assert.assertTrue(dfs.exists(foo));
            Assert.assertTrue(dfs.exists(bar));
        } finally {
            if (cluster != null) {
                cluster.shutdown();
            }
        }
    }

    /**
     * Ensure that restart namenode with downgrade option should throw exception
     * because it has been obsolete.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectNewFsImage() throws IOException {
        final Configuration conf = new Configuration();
        MiniDFSCluster cluster = null;
        try {
            cluster = new MiniDFSCluster.Builder(conf).numDataNodes(0).build();
            cluster.waitActive();
            RestartFramework.at("after_cluster_creation").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DistributedFileSystem fs = cluster.getFileSystem();
            fs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
            fs.saveNamespace();
            fs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
            RestartFramework.at("after_namespace_save").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            NNStorage storage = spy(cluster.getNameNode().getFSImage().getStorage());
            int futureVersion = NameNodeLayoutVersion.CURRENT_LAYOUT_VERSION - 1;
            doReturn(futureVersion).when(storage).getServiceLayoutVersion();
            storage.writeAll();
            RestartFramework.at("after_storage_modification").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            cluster.restartNameNode(0, true, "-rollingUpgrade", "downgrade");
        } finally {
            if (cluster != null) {
                cluster.shutdown();
            }
        }
    }
}

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

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;
import org.apache.hadoop.hdfs.server.protocol.SlowDiskReports;
import org.apache.hadoop.hdfs.server.protocol.SlowPeerReports;
import org.apache.hadoop.hdfs.server.protocol.StorageReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;

public class TestStorageReport_RestartInjected_Randomized_123 {

    public static final Logger LOG = LoggerFactory.getLogger(TestStorageReport_RestartInjected.class);

    private static final short REPL_FACTOR = 1;

    // pick non-default.
    private static final StorageType storageType = StorageType.SSD;

    private static Configuration conf;

    private MiniDFSCluster cluster;

    private DistributedFileSystem fs;

    static String bpid;

    @Before
    public void startUpCluster() throws IOException {
        conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPL_FACTOR).storageTypes(new StorageType[] { storageType, storageType }).build();
        fs = cluster.getFileSystem();
        bpid = cluster.getNamesystem().getBlockPoolId();
    }

    @After
    public void shutDownCluster() throws IOException {
        if (cluster != null) {
            fs.close();
            cluster.shutdown();
            cluster = null;
        }
    }

    /**
     * Ensure that storage type and storage state are propagated
     * in Storage Reports.
     */
    @Test
    public void testStorageReportHasStorageTypeAndState() throws IOException {
        RestartFramework.at("after_trigger_heartbeat").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertNotSame(storageType, StorageType.DEFAULT);
        NameNode nn = cluster.getNameNode();
        DataNode dn = cluster.getDataNodes().get(0);
        DatanodeProtocolClientSideTranslatorPB nnSpy = InternalDataNodeTestUtils.spyOnBposToNN(dn, nn);
        RestartFramework.at("after_cluster_setup").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_spy_setup").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nn = cluster.getNameNode();
        DataNodeTestUtils.triggerHeartbeat(dn);
        dn = cluster.getDataNodes().get(0);
        ArgumentCaptor<StorageReport[]> captor = ArgumentCaptor.forClass(StorageReport[].class);
        Mockito.verify(nnSpy).sendHeartbeat(any(DatanodeRegistration.class), captor.capture(), anyLong(), anyLong(), anyInt(), anyInt(), anyInt(), any(), Mockito.anyBoolean(), Mockito.any(SlowPeerReports.class), Mockito.any(SlowDiskReports.class));
        StorageReport[] reports = captor.getValue();
        for (StorageReport report : reports) {
            assertThat(report.getStorage().getStorageType(), is(storageType));
            assertThat(report.getStorage().getState(), is(DatanodeStorage.State.NORMAL));
        }
    }
}

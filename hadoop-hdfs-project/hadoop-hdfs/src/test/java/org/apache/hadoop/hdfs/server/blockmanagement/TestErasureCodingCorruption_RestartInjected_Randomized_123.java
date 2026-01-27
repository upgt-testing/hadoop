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
package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSNNTopology;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_CORRUPT_BLOCK_DELETE_IMMEDIATELY_ENABLED;

/**
 * In case of Erasure Coding the entire block group is marked corrupted, in
 * case there is a failure during write, the replica when reported to
 * Namenode marks the block group as corrupt, but since the file is complete
 * and the replica has a lower gen stamp, the replica is marked to be
 * deleted, but in case the replica is on stale storage(Namenode marks
 * datanodes as stale in case of failover, etc), then the deletion of block
 * is postponed and hence the block group isn't removed from the corrupt
 * replica map. So, in case of striped blocks, if the replica is suppose to
 * get deleted, explicitly remove the block group from corrupt-replica map.
 */
public class TestErasureCodingCorruption_RestartInjected_Randomized_123 {

    @Test
    public void testCorruptionDuringFailover() throws Exception {
        Configuration conf = new Configuration();
        // Set removal of corrupt replicas immediately as false, to trigger this
        // case.
        conf.setBoolean(DFS_NAMENODE_CORRUPT_BLOCK_DELETE_IMMEDIATELY_ENABLED, false);
        try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).nnTopology(MiniDFSNNTopology.simpleHATopology()).numDataNodes(8).build()) {
            cluster.transitionToActive(0);
            cluster.waitActive();
            RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            DistributedFileSystem dfs = cluster.getFileSystem(0);
            dfs.mkdirs(new Path("/dir"));
            dfs.setErasureCodingPolicy(new Path("/dir"), "RS-6-3-1024k");
            RestartFramework.at("after_set_ec_policy").on(cluster).restart("datanode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            FSDataOutputStream out = dfs.create(new Path("/dir/file"));
            RestartFramework.at("after_file_create").on(cluster).restart("datanode").withIndex(2).withMode(RestartMode.GRACEFUL).execute();
            // Write more than one stripe, so that data can get flushed to all
            // datanodes.
            for (int i = 0; i < 15 * 1024 * 1024; i++) {
                out.write(i);
            }
            RestartFramework.at("after_first_write").on(cluster).restart("datanode").withIndex(3).withMode(RestartMode.GRACEFUL).execute();
            // Stop one datanode, so as to trigger update pipeline.
            MiniDFSCluster.DataNodeProperties dn = cluster.stopDataNode(0);
            RestartFramework.at("after_stop_datanode").on(cluster).restart("datanode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            // Write some more data and close the file.
            for (int i = 0; i < 7 * 1024 * 1024; i++) {
                out.write(i);
            }
            out.close();
            RestartFramework.at("after_file_close").on(cluster).restart("datanode").withIndex(2).withMode(RestartMode.GRACEFUL).execute();
            BlockManager bm = cluster.getNamesystem(0).getBlockManager();
            // Transition to standby and then to active.
            cluster.transitionToStandby(0);
            cluster.transitionToActive(0);
            RestartFramework.at("after_failover").on(cluster).restart("datanode").withIndex(3).withMode(RestartMode.GRACEFUL).execute();
            // Restart the stopped Datanode, this datanode would report a replica
            // that failed during write.
            cluster.restartDataNode(dn);
            RestartFramework.at("after_restart_datanode").on(cluster).restart("datanode").withIndex(1).withMode(RestartMode.GRACEFUL).execute();
            GenericTestUtils.waitFor(() -> bm.getCorruptECBlockGroups() == 0, 100, 10000);
        }
    }
}

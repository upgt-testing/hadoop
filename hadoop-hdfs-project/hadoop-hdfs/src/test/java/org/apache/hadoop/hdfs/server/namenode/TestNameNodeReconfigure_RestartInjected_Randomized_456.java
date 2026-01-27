/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_IMAGE_PARALLEL_LOAD_KEY;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ReconfigurationException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.protocol.BlockType;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.namenode.sps.StoragePolicySatisfyManager;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_CALLER_CONTEXT_ENABLED_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_CALLER_CONTEXT_ENABLED_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_DATANODE_PEER_STATS_ENABLED_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_DEFAULT;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_INVALIDATE_LIMIT_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_AVOID_SLOW_DATANODE_FOR_READ_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_BLOCKPLACEMENTPOLICY_EXCLUDE_SLOW_NODES_ENABLED_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_MAX_SLOWPEER_COLLECT_NODES_KEY;
import static org.apache.hadoop.fs.CommonConfigurationKeys.IPC_BACKOFF_ENABLE_DEFAULT;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestNameNodeReconfigure_RestartInjected_Randomized_456 {

    public static final Logger LOG = LoggerFactory.getLogger(TestNameNodeReconfigure_RestartInjected.class);

    private MiniDFSCluster cluster;

    private final int customizedBlockInvalidateLimit = 500;

    @Before
    public void setUp() throws IOException {
        Configuration conf = new HdfsConfiguration();
        conf.setInt(DFS_BLOCK_INVALIDATE_LIMIT_KEY, customizedBlockInvalidateLimit);
        cluster = new MiniDFSCluster.Builder(conf).build();
        cluster.waitActive();
    }

    @Test
    public void testReconfigureCallerContextEnabled() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        FSNamesystem nameSystem = nameNode.getNamesystem();
        RestartFramework.at("after_revert_to_default").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        nameSystem = cluster.getNamesystem();
        // try invalid values
        nameNode.reconfigureProperty(HADOOP_CALLER_CONTEXT_ENABLED_KEY, "text");
        verifyReconfigureCallerContextEnabled(nameNode, nameSystem, false);
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        nameSystem = cluster.getNamesystem();
        RestartFramework.at("after_enable_caller_context").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // enable CallerContext
        nameNode.reconfigureProperty(HADOOP_CALLER_CONTEXT_ENABLED_KEY, "true");
        RestartFramework.at("after_disable_caller_context").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        verifyReconfigureCallerContextEnabled(nameNode, nameSystem, true);
        nameNode = cluster.getNameNode();
        nameSystem = cluster.getNamesystem();
        // disable CallerContext
        nameNode.reconfigureProperty(HADOOP_CALLER_CONTEXT_ENABLED_KEY, "false");
        verifyReconfigureCallerContextEnabled(nameNode, nameSystem, false);
        nameNode = cluster.getNameNode();
        nameSystem = cluster.getNamesystem();
        RestartFramework.at("after_invalid_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // revert to default
        nameNode.reconfigureProperty(HADOOP_CALLER_CONTEXT_ENABLED_KEY, null);
        nameNode = cluster.getNameNode();
        nameSystem = cluster.getNamesystem();
        // verify default
        assertEquals(HADOOP_CALLER_CONTEXT_ENABLED_KEY + " has wrong value", false, nameSystem.getCallerContextEnabled());
        assertEquals(HADOOP_CALLER_CONTEXT_ENABLED_KEY + " has wrong value", null, nameNode.getConf().get(HADOOP_CALLER_CONTEXT_ENABLED_KEY));
    }

    void verifyReconfigureCallerContextEnabled(final NameNode nameNode, final FSNamesystem nameSystem, boolean expected) {
        assertEquals(HADOOP_CALLER_CONTEXT_ENABLED_KEY + " has wrong value", expected, nameNode.getNamesystem().getCallerContextEnabled());
        assertEquals(HADOOP_CALLER_CONTEXT_ENABLED_KEY + " has wrong value", expected, nameNode.getConf().getBoolean(HADOOP_CALLER_CONTEXT_ENABLED_KEY, HADOOP_CALLER_CONTEXT_ENABLED_DEFAULT));
    }

    /**
     * Test to reconfigure enable/disable IPC backoff
     */
    @Test
    public void testReconfigureIPCBackoff() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_enable_ipc_backoff").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        NameNodeRpcServer nnrs = (NameNodeRpcServer) nameNode.getRpcServer();
        String ipcClientRPCBackoffEnable = NameNode.buildBackoffEnableKey(nnrs.getClientRpcServer().getPort());
        nameNode = cluster.getNameNode();
        RestartFramework.at("after_revert_to_default").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_disable_ipc_backoff").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // try invalid values
        verifyReconfigureIPCBackoff(nameNode, nnrs, ipcClientRPCBackoffEnable, false);
        // enable IPC_CLIENT_RPC_BACKOFF
        nameNode.reconfigureProperty(ipcClientRPCBackoffEnable, "true");
        verifyReconfigureIPCBackoff(nameNode, nnrs, ipcClientRPCBackoffEnable, true);
        nameNode = cluster.getNameNode();
        // disable IPC_CLIENT_RPC_BACKOFF
        nameNode.reconfigureProperty(ipcClientRPCBackoffEnable, "false");
        verifyReconfigureIPCBackoff(nameNode, nnrs, ipcClientRPCBackoffEnable, false);
        nameNode = cluster.getNameNode();
        // revert to default
        nameNode.reconfigureProperty(ipcClientRPCBackoffEnable, null);
        nameNode = cluster.getNameNode();
        assertEquals(ipcClientRPCBackoffEnable + " has wrong value", false, nnrs.getClientRpcServer().isClientBackoffEnabled());
        assertEquals(ipcClientRPCBackoffEnable + " has wrong value", null, nameNode.getConf().get(ipcClientRPCBackoffEnable));
    }

    void verifyReconfigureIPCBackoff(final NameNode nameNode, final NameNodeRpcServer nnrs, String property, boolean expected) {
        assertEquals(property + " has wrong value", expected, nnrs.getClientRpcServer().isClientBackoffEnabled());
        assertEquals(property + " has wrong value", expected, nameNode.getConf().getBoolean(property, IPC_BACKOFF_ENABLE_DEFAULT));
    }

    /**
     * Test to reconfigure interval of heart beat check and re-check.
     */
    @Test
    public void testReconfigureHearbeatCheck() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        final DatanodeManager datanodeManager = nameNode.namesystem.getBlockManager().getDatanodeManager();
        nameNode = cluster.getNameNode();
        // change properties
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, "" + 6);
        nameNode.reconfigureProperty(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, "" + (10 * 60 * 1000));
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        // try invalid values
        try {
            nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, "text");
            fail("ReconfigurationException expected");
        } catch (ReconfigurationException expected) {
            assertTrue(expected.getCause() instanceof NumberFormatException);
        }
        try {
            nameNode.reconfigureProperty(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, "text");
            fail("ReconfigurationException expected");
        } catch (ReconfigurationException expected) {
            assertTrue(expected.getCause() instanceof NumberFormatException);
        }
        // verify change
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", 6, nameNode.getConf().getLong(DFS_HEARTBEAT_INTERVAL_KEY, DFS_HEARTBEAT_INTERVAL_DEFAULT));
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", 6, datanodeManager.getHeartbeatInterval());
        assertEquals(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY + " has wrong value", 10 * 60 * 1000, nameNode.getConf().getInt(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_DEFAULT));
        assertEquals(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY + " has wrong value", 10 * 60 * 1000, datanodeManager.getHeartbeatRecheckInterval());
        // change to a value with time unit
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, "1m");
        nameNode = cluster.getNameNode();
        RestartFramework.at("after_heartbeat_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", 60, nameNode.getConf().getLong(DFS_HEARTBEAT_INTERVAL_KEY, DFS_HEARTBEAT_INTERVAL_DEFAULT));
        RestartFramework.at("after_heartbeat_time_unit_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_revert_to_defaults").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", 60, datanodeManager.getHeartbeatInterval());
        // revert to defaults
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, null);
        nameNode.reconfigureProperty(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, null);
        nameNode = cluster.getNameNode();
        // verify defaults
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", null, nameNode.getConf().get(DFS_HEARTBEAT_INTERVAL_KEY));
        assertEquals(DFS_HEARTBEAT_INTERVAL_KEY + " has wrong value", DFS_HEARTBEAT_INTERVAL_DEFAULT, datanodeManager.getHeartbeatInterval());
        assertEquals(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY + " has wrong value", null, nameNode.getConf().get(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY));
        assertEquals(DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY + " has wrong value", DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_DEFAULT, datanodeManager.getHeartbeatRecheckInterval());
    }

    /**
     * Tests enable/disable Storage Policy Satisfier dynamically when
     * "dfs.storage.policy.enabled" feature is disabled.
     *
     * @throws ReconfigurationException
     * @throws IOException
     */
    @Test
    public void testReconfigureSPSWithStoragePolicyDisabled() throws ReconfigurationException, IOException {
        // shutdown cluster
        cluster.shutdown();
        Configuration conf = new HdfsConfiguration();
        conf.setBoolean(DFSConfigKeys.DFS_STORAGE_POLICY_ENABLED_KEY, false);
        RestartFramework.at("after_cluster_restart").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster = new MiniDFSCluster.Builder(conf).build();
        cluster.waitActive();
        NameNode nameNode = cluster.getNameNode();
        verifySPSEnabled(nameNode, DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE, false);
        RestartFramework.at("after_sps_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // enable SPS internally by keeping DFS_STORAGE_POLICY_ENABLED_KEY
        nameNode.reconfigureProperty(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.EXTERNAL.toString());
        nameNode = cluster.getNameNode();
        // Since DFS_STORAGE_POLICY_ENABLED_KEY is disabled, SPS can't be enabled.
        assertNull("SPS shouldn't start as " + DFSConfigKeys.DFS_STORAGE_POLICY_ENABLED_KEY + " is disabled", nameNode.getNamesystem().getBlockManager().getSPSManager());
        verifySPSEnabled(nameNode, DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.EXTERNAL, false);
        assertEquals(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY + " has wrong value", StoragePolicySatisfierMode.EXTERNAL.toString(), nameNode.getConf().get(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, DFS_STORAGE_POLICY_SATISFIER_MODE_DEFAULT));
    }

    /**
     * Tests enable/disable Storage Policy Satisfier dynamically.
     */
    @Test
    public void testReconfigureStoragePolicySatisfierEnabled() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        nameNode = cluster.getNameNode();
        verifySPSEnabled(nameNode, DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE, false);
        // try invalid values
        try {
            nameNode.reconfigureProperty(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, "text");
            fail("ReconfigurationException expected");
        } catch (ReconfigurationException e) {
            GenericTestUtils.assertExceptionContains("For enabling or disabling storage policy satisfier, must " + "pass either internal/external/none string value only", e.getCause());
        }
        RestartFramework.at("after_enable_external_sps").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_disable_sps").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // disable SPS
        nameNode.reconfigureProperty(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE.toString());
        verifySPSEnabled(nameNode, DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE, false);
        nameNode = cluster.getNameNode();
        // enable external SPS
        nameNode.reconfigureProperty(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.EXTERNAL.toString());
        nameNode = cluster.getNameNode();
        assertEquals(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY + " has wrong value", false, nameNode.getNamesystem().getBlockManager().getSPSManager().isSatisfierRunning());
        assertEquals(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY + " has wrong value", StoragePolicySatisfierMode.EXTERNAL.toString(), nameNode.getConf().get(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, DFS_STORAGE_POLICY_SATISFIER_MODE_DEFAULT));
    }

    /**
     * Test to satisfy storage policy after disabled storage policy satisfier.
     */
    @Test
    public void testSatisfyStoragePolicyAfterSatisfierDisabled() throws ReconfigurationException, IOException {
        RestartFramework.at("after_set_storage_policy").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        NameNode nameNode = cluster.getNameNode();
        nameNode = cluster.getNameNode();
        // disable SPS
        nameNode.reconfigureProperty(DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE.toString());
        verifySPSEnabled(nameNode, DFS_STORAGE_POLICY_SATISFIER_MODE_KEY, StoragePolicySatisfierMode.NONE, false);
        nameNode = cluster.getNameNode();
        Path filePath = new Path("/testSPS");
        DistributedFileSystem fileSystem = cluster.getFileSystem();
        fileSystem.create(filePath);
        RestartFramework.at("after_disable_sps").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_file_create").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        fileSystem.setStoragePolicy(filePath, "COLD");
        nameNode = cluster.getNameNode();
        try {
            fileSystem.satisfyStoragePolicy(filePath);
            fail("Expected to fail, as storage policy feature has disabled.");
        } catch (RemoteException e) {
            GenericTestUtils.assertExceptionContains("Cannot request to satisfy storage policy " + "when storage policy satisfier feature has been disabled" + " by admin. Seek for an admin help to enable it " + "or use Mover tool.", e);
        }
    }

    void verifySPSEnabled(final NameNode nameNode, String property, StoragePolicySatisfierMode expected, boolean isSatisfierRunning) {
        StoragePolicySatisfyManager spsMgr = nameNode.getNamesystem().getBlockManager().getSPSManager();
        boolean isSPSRunning = spsMgr != null ? spsMgr.isSatisfierRunning() : false;
        assertEquals(property + " has wrong value", isSPSRunning, isSPSRunning);
        String actual = nameNode.getConf().get(property, DFS_STORAGE_POLICY_SATISFIER_MODE_DEFAULT);
        assertEquals(property + " has wrong value", expected, StoragePolicySatisfierMode.fromString(actual));
    }

    @Test
    public void testBlockInvalidateLimitAfterReconfigured() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final DatanodeManager datanodeManager = nameNode.namesystem.getBlockManager().getDatanodeManager();
        nameNode = cluster.getNameNode();
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not correctly set", customizedBlockInvalidateLimit, datanodeManager.getBlockInvalidateLimit());
        RestartFramework.at("after_second_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, Integer.toString(6));
        RestartFramework.at("after_first_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        // 20 * 6 = 120 < 500
        // Invalid block limit should stay same as before after reconfiguration.
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not honored after reconfiguration", customizedBlockInvalidateLimit, datanodeManager.getBlockInvalidateLimit());
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, Integer.toString(50));
        nameNode = cluster.getNameNode();
        // 20 * 50 = 1000 > 500
        // Invalid block limit should be reset to 1000
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not reconfigured correctly", 1000, datanodeManager.getBlockInvalidateLimit());
    }

    @Test
    public void testEnableParallelLoadAfterReconfigured() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        // By default, enableParallelLoad is false
        assertEquals(false, FSImageFormatProtobuf.getEnableParallelLoad());
        RestartFramework.at("after_enable_parallel_load").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode.reconfigureProperty(DFS_IMAGE_PARALLEL_LOAD_KEY, Boolean.toString(true));
        nameNode = cluster.getNameNode();
        // After reconfigured, enableParallelLoad is true
        assertEquals(true, FSImageFormatProtobuf.getEnableParallelLoad());
    }

    @Test
    public void testEnableSlowNodesParametersAfterReconfigured() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        RestartFramework.at("after_avoid_slow_datanode_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final BlockManager blockManager = nameNode.namesystem.getBlockManager();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        DatanodeManager datanodeManager = blockManager.getDatanodeManager();
        nameNode = cluster.getNameNode();
        datanodeManager = cluster.getNamesystem().getBlockManager().getDatanodeManager();
        // By default, avoidSlowDataNodesForRead is false.
        assertEquals(false, datanodeManager.getEnableAvoidSlowDataNodesForRead());
        nameNode.reconfigureProperty(DFS_NAMENODE_AVOID_SLOW_DATANODE_FOR_READ_KEY, Boolean.toString(true));
        nameNode = cluster.getNameNode();
        datanodeManager = cluster.getNamesystem().getBlockManager().getDatanodeManager();
        // After reconfigured, avoidSlowDataNodesForRead is true.
        assertEquals(true, datanodeManager.getEnableAvoidSlowDataNodesForRead());
        RestartFramework.at("after_exclude_slow_nodes_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        // By default, excludeSlowNodesEnabled is false.
        assertEquals(false, blockManager.getExcludeSlowNodesEnabled(BlockType.CONTIGUOUS));
        assertEquals(false, blockManager.getExcludeSlowNodesEnabled(BlockType.STRIPED));
        nameNode.reconfigureProperty(DFS_NAMENODE_BLOCKPLACEMENTPOLICY_EXCLUDE_SLOW_NODES_ENABLED_KEY, Boolean.toString(true));
        nameNode = cluster.getNameNode();
        datanodeManager = cluster.getNamesystem().getBlockManager().getDatanodeManager();
        // After reconfigured, excludeSlowNodesEnabled is true.
        assertEquals(true, blockManager.getExcludeSlowNodesEnabled(BlockType.CONTIGUOUS));
        assertEquals(true, blockManager.getExcludeSlowNodesEnabled(BlockType.STRIPED));
    }

    @Test
    public void testReconfigureMaxSlowpeerCollectNodes() throws ReconfigurationException {
        NameNode nameNode = cluster.getNameNode();
        final DatanodeManager datanodeManager = nameNode.namesystem.getBlockManager().getDatanodeManager();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_reconfigure_max_slowpeer").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        // By default, DFS_NAMENODE_MAX_SLOWPEER_COLLECT_NODES_KEY is 5.
        assertEquals(5, datanodeManager.getMaxSlowpeerCollectNodes());
        // Reconfigure.
        nameNode.reconfigureProperty(DFS_NAMENODE_MAX_SLOWPEER_COLLECT_NODES_KEY, Integer.toString(10));
        nameNode = cluster.getNameNode();
        // Assert DFS_NAMENODE_MAX_SLOWPEER_COLLECT_NODES_KEY is 10.
        assertEquals(10, datanodeManager.getMaxSlowpeerCollectNodes());
    }

    @Test
    public void testBlockInvalidateLimit() throws ReconfigurationException {
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        NameNode nameNode = cluster.getNameNode();
        final DatanodeManager datanodeManager = nameNode.namesystem.getBlockManager().getDatanodeManager();
        nameNode = cluster.getNameNode();
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not correctly set", customizedBlockInvalidateLimit, datanodeManager.getBlockInvalidateLimit());
        RestartFramework.at("after_block_invalidate_limit_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_heartbeat_interval_reconfigure").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            nameNode.reconfigureProperty(DFS_BLOCK_INVALIDATE_LIMIT_KEY, "non-numeric");
            fail("Should not reach here");
        } catch (ReconfigurationException e) {
            assertEquals("Could not change property dfs.block.invalidate.limit from '500' to 'non-numeric'", e.getMessage());
        }
        nameNode.reconfigureProperty(DFS_BLOCK_INVALIDATE_LIMIT_KEY, "2500");
        nameNode = cluster.getNameNode();
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not honored after reconfiguration", 2500, datanodeManager.getBlockInvalidateLimit());
        nameNode.reconfigureProperty(DFS_HEARTBEAT_INTERVAL_KEY, "500");
        nameNode = cluster.getNameNode();
        // 20 * 500 (10000) > 2500
        // Hence, invalid block limit should be reset to 10000
        assertEquals(DFS_BLOCK_INVALIDATE_LIMIT_KEY + " is not reconfigured correctly", 10000, datanodeManager.getBlockInvalidateLimit());
    }

    @Test
    public void testSlowPeerTrackerEnabled() throws Exception {
        NameNode nameNode = cluster.getNameNode();
        final DatanodeManager datanodeManager = nameNode.namesystem.getBlockManager().getDatanodeManager();
        RestartFramework.at("after_cluster_start").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        assertFalse("SlowNode tracker is already enabled. It should be disabled by default", datanodeManager.getSlowPeerTracker().isSlowPeerTrackerEnabled());
        try {
            nameNode.reconfigurePropertyImpl(DFS_DATANODE_PEER_STATS_ENABLED_KEY, "non-boolean");
            fail("should not reach here");
        } catch (ReconfigurationException e) {
            assertEquals("Could not change property dfs.datanode.peer.stats.enabled from 'false' to 'non-boolean'", e.getMessage());
        }
        nameNode.reconfigurePropertyImpl(DFS_DATANODE_PEER_STATS_ENABLED_KEY, "True");
        RestartFramework.at("after_revert_slow_peer_tracker").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        nameNode = cluster.getNameNode();
        RestartFramework.at("after_enable_slow_peer_tracker").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertTrue("SlowNode tracker is still disabled. Reconfiguration could not be successful", datanodeManager.getSlowPeerTracker().isSlowPeerTrackerEnabled());
        nameNode.reconfigurePropertyImpl(DFS_DATANODE_PEER_STATS_ENABLED_KEY, null);
        nameNode = cluster.getNameNode();
        assertFalse("SlowNode tracker is still enabled. Reconfiguration could not be successful", datanodeManager.getSlowPeerTracker().isSlowPeerTrackerEnabled());
    }

    @After
    public void shutDown() throws IOException {
        if (cluster != null) {
            cluster.shutdown();
        }
    }
}

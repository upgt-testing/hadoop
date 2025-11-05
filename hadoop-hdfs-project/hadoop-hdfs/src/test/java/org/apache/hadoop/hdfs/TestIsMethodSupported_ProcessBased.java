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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocolPB.ClientDatanodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolPB;
import org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.InterDatanodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.JournalProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.NamenodeProtocolPB;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.server.protocol.JournalProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocol;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RpcClientUtil;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.RefreshUserMappingsProtocol;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.RefreshAuthorizationPolicyProtocol;
import org.apache.hadoop.security.protocolPB.RefreshAuthorizationPolicyProtocolClientSideTranslatorPB;
import org.apache.hadoop.security.protocolPB.RefreshUserMappingsProtocolClientSideTranslatorPB;
import org.apache.hadoop.ipc.protocolPB.RefreshCallQueueProtocolClientSideTranslatorPB;
import org.apache.hadoop.ipc.RefreshCallQueueProtocol;
import org.apache.hadoop.tools.GetUserMappingsProtocol;
import org.apache.hadoop.tools.protocolPB.GetUserMappingsProtocolClientSideTranslatorPB;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestIsMethodSupported}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Test cases to verify that client side translators correctly implement the
 * isMethodSupported method in ProtocolMetaInterface.
 *
 * @see TestIsMethodSupported Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestIsMethodSupported_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "BEFORE_VERIFICATION"
    );
  }

  @Test
  public void testNamenodeProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    NamenodeProtocol np =
        NameNodeProxies.createNonHAProxy(conf,
            nnAddress, NamenodeProtocol.class, UserGroupInformation.getCurrentUser(),
            true).getProxy();

    boolean exists = RpcClientUtil.isMethodSupported(np,
        NamenodeProtocolPB.class, RPC.RpcKind.RPC_PROTOCOL_BUFFER,
        RPC.getProtocolVersion(NamenodeProtocolPB.class), "rollEditLog");

    assertTrue(exists);
    exists = RpcClientUtil.isMethodSupported(np,
        NamenodeProtocolPB.class, RPC.RpcKind.RPC_PROTOCOL_BUFFER,
        RPC.getProtocolVersion(NamenodeProtocolPB.class), "bogusMethod");
    assertFalse(exists);

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testDatanodeProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    DatanodeProtocolClientSideTranslatorPB translator =
        new DatanodeProtocolClientSideTranslatorPB(nnAddress, conf);
    assertTrue(translator.isMethodSupported("sendHeartbeat"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testClientDatanodeProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    // Get DataNode address from client-side API
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    DatanodeInfo[] datanodes = dfs.getDataNodeStats();
    assertTrue("Expected at least one DataNode", datanodes.length > 0);
    DatanodeInfo dnInfo = datanodes[0];
    InetSocketAddress dnAddress = new InetSocketAddress(dnInfo.getIpAddr(), dnInfo.getIpcPort());

    ClientDatanodeProtocolTranslatorPB translator =
        new ClientDatanodeProtocolTranslatorPB(nnAddress,
            UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf));
    //Namenode doesn't implement ClientDatanodeProtocol
    assertFalse(translator.isMethodSupported("refreshNamenodes"));

    translator = new ClientDatanodeProtocolTranslatorPB(
        dnAddress, UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf));
    assertTrue(translator.isMethodSupported("refreshNamenodes"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testClientNamenodeProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    ClientProtocol cp =
        NameNodeProxies.createNonHAProxy(
            conf, nnAddress, ClientProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    RpcClientUtil.isMethodSupported(cp,
        ClientNamenodeProtocolPB.class, RPC.RpcKind.RPC_PROTOCOL_BUFFER,
        RPC.getProtocolVersion(ClientNamenodeProtocolPB.class), "mkdirs");

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testJournalProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    JournalProtocolTranslatorPB translator = (JournalProtocolTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress, JournalProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    //Namenode doesn't implement JournalProtocol
    assertFalse(translator.isMethodSupported("startLogSegment"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testInterDatanodeProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    // Get DataNode address from client-side API
    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    DatanodeInfo[] datanodes = dfs.getDataNodeStats();
    assertTrue("Expected at least one DataNode", datanodes.length > 0);
    DatanodeInfo dnInfo = datanodes[0];
    InetSocketAddress dnAddress = new InetSocketAddress(dnInfo.getIpAddr(), dnInfo.getIpcPort());

    InterDatanodeProtocolTranslatorPB translator =
        new InterDatanodeProtocolTranslatorPB(
            nnAddress, UserGroupInformation.getCurrentUser(), conf,
            NetUtils.getDefaultSocketFactory(conf), 0);
    //Not supported at namenode
    assertFalse(translator.isMethodSupported("initReplicaRecovery"));

    translator = new InterDatanodeProtocolTranslatorPB(
        dnAddress, UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf), 0);
    assertTrue(translator.isMethodSupported("initReplicaRecovery"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testGetUserMappingsProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    GetUserMappingsProtocolClientSideTranslatorPB translator =
        (GetUserMappingsProtocolClientSideTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress,
            GetUserMappingsProtocol.class, UserGroupInformation.getCurrentUser(),
            true).getProxy();
    assertTrue(translator.isMethodSupported("getGroupsForUser"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testRefreshAuthorizationPolicyProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    RefreshAuthorizationPolicyProtocolClientSideTranslatorPB translator =
      (RefreshAuthorizationPolicyProtocolClientSideTranslatorPB)
      NameNodeProxies.createNonHAProxy(conf, nnAddress,
          RefreshAuthorizationPolicyProtocol.class,
          UserGroupInformation.getCurrentUser(), true).getProxy();
    assertTrue(translator.isMethodSupported("refreshServiceAcl"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testRefreshUserMappingsProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    RefreshUserMappingsProtocolClientSideTranslatorPB translator =
        (RefreshUserMappingsProtocolClientSideTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress,
            RefreshUserMappingsProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    assertTrue(
        translator.isMethodSupported("refreshUserToGroupsMappings"));

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test
  public void testRefreshCallQueueProtocol() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress nnAddress = cluster.getNameNodeRpcAddress();

    RefreshCallQueueProtocolClientSideTranslatorPB translator =
        (RefreshCallQueueProtocolClientSideTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress,
            RefreshCallQueueProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    assertTrue(
        translator.isMethodSupported("refreshCallQueue"));

    checkpoint("BEFORE_VERIFICATION");
  }
}

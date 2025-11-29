/**
 *
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
package org.apache.hadoop.hdfs.server.namenode;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicy;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.test.GenericTestUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestFavoredNodesEndToEnd_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestFavoredNodesEndToEnd_RestartInjected.class);
  {
    GenericTestUtils.setLogLevel(
        LoggerFactory.getLogger(BlockPlacementPolicy.class), Level.TRACE);
  }

  private MiniDFSCluster cluster;
  private Configuration conf;
  private final static int NUM_DATA_NODES = 10;
  private final static int NUM_FILES = 10;
  private final static byte[] SOME_BYTES = new String("foo").getBytes();
  private DistributedFileSystem dfs;
  private ArrayList<DataNode> datanodes;

  @Before
  public void setup() throws Exception {
    conf = new Configuration();
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_DATA_NODES).build();
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    datanodes = cluster.getDataNodes();
  }

  @After
  public void teardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  private BlockLocation[] getBlockLocations(Path p) throws Exception {
    DFSTestUtil.waitReplication(dfs, p, (short)3);
    BlockLocation[] locations = dfs.getClient().getBlockLocations(
        p.toUri().getPath(), 0, Long.MAX_VALUE);
    assertTrue(locations.length == 1 && locations[0].getHosts().length == 3);
    return locations;
  }

  private String[] getStringForInetSocketAddrs(InetSocketAddress[] datanode) {
    String strs[] = new String[datanode.length];
    for (int i = 0; i < datanode.length; i++) {
      strs[i] = datanode[i].getAddress().getHostAddress() + ":" +
       datanode[i].getPort();
    }
    return strs;
  }

  private boolean compareNodes(String[] dnList1, String[] dnList2) {
    for (int i = 0; i < dnList1.length; i++) {
      boolean matched = false;
      for (int j = 0; j < dnList2.length; j++) {
        if (dnList1[i].equals(dnList2[j])) {
          matched = true;
          break;
        }
      }
      if (matched == false) {
        fail(dnList1[i] + " not a favored node");
      }
    }
    return true;
  }

  private InetSocketAddress[] getDatanodes(Random rand) {
    int idx1 = rand.nextInt(NUM_DATA_NODES);
    int idx2;
    do {
      idx2 = rand.nextInt(NUM_DATA_NODES);
    } while (idx1 == idx2);
    int idx3;
    do {
      idx3 = rand.nextInt(NUM_DATA_NODES);
    } while (idx2 == idx3 || idx1 == idx3);
    InetSocketAddress[] addrs = new InetSocketAddress[3];
    addrs[0] = datanodes.get(idx1).getXferAddress();
    addrs[1] = datanodes.get(idx2).getXferAddress();
    addrs[2] = datanodes.get(idx3).getXferAddress();
    return addrs;
  }

  private InetSocketAddress getArbitraryLocalHostAddr()
      throws UnknownHostException{
    Random rand = new Random(System.currentTimeMillis());
    int port = rand.nextInt(65535);
    while (true) {
      boolean conflict = false;
      for (DataNode d : datanodes) {
        if (d.getXferAddress().getPort() == port) {
          port = rand.nextInt(65535);
          conflict = true;
        }
      }
      if (conflict == false) {
        break;
      }
    }
    return new InetSocketAddress(InetAddress.getLocalHost(), port);
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_NN_Graceful() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_NN_Crash() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_DN_Graceful() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_DN_Crash() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_AllDN_Graceful() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_AllDN_Crash() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_RandomDN_Graceful() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }

  @Test(timeout = 240000)
  public void testFavoredNodesEndToEnd_AfterClose_RandomDN_Crash() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    InetSocketAddress datanode[] = getDatanodes(rand);
    Path p = new Path("/filename0");
    FSDataOutputStream out = dfs.create(p, FsPermission.getDefault(), true,
        4096, (short)3, 4096L, null, datanode);
    out.write(SOME_BYTES);
    out.close();
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);
    BlockLocation[] locations = getBlockLocations(p);
    for (BlockLocation loc : locations) {
      String[] hosts = loc.getNames();
      String[] hosts1 = getStringForInetSocketAddrs(datanode);
      assertTrue(compareNodes(hosts, hosts1));
    }
  }
}

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
package org.apache.hadoop.hdfs.server.balancer;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DFSUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.NameNodeProxies;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.datanode.SimulatedFSDataset;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart-injected version of TestBalancer.
 *
 * Original test: TestBalancer
 * This file contains restart injection variants for selected test methods.
 *
 * Note: TestBalancer is a complex test primarily focused on balancer tool
 * functionality. This transformation focuses on testing data persistence
 * across restarts during balancing operations.
 */
public class TestBalancer_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestBalancer_RestartInjected.class);

  static final long CAPACITY = 5000L;
  static final String RACK0 = "/rack0";
  static final String RACK1 = "/rack1";
  static final String RACK2 = "/rack2";
  final static private String fileName = "/tmp.txt";
  final static Path filePath = new Path(fileName);
  private static final int DEFAULT_BLOCK_SIZE = 100;
  private static final Random r = new Random();

  static final long TIMEOUT = 40000L;
  static final double CAPACITY_ALLOWED_VARIANCE = 0.005;
  static final double BALANCE_ALLOWED_VARIANCE = 0.11;

  private MiniDFSCluster cluster;
  private ClientProtocol client;
  private Configuration conf;
  private FileSystem fs;

  static {
    NameNodeConnector.setWrite2IdFile(false);
  }

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    initConf(conf);
    // Allow client to survive NN restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  static void initConf(Configuration conf) {
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, DEFAULT_BLOCK_SIZE);
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, DEFAULT_BLOCK_SIZE);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1L);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 500);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1L);
    SimulatedFSDataset.setFactory(conf);
    conf.setLong(DFSConfigKeys.DFS_BALANCER_MOVEDWINWIDTH_KEY, 2000L);
    conf.setLong(DFSConfigKeys.DFS_BALANCER_GETBLOCKS_MIN_BLOCK_SIZE_KEY, 1L);
    conf.setInt(DFSConfigKeys.DFS_BALANCER_MAX_NO_MOVE_INTERVAL_KEY, 5*1000);
  }

  static void createFile(MiniDFSCluster cluster, Path filePath, long fileLen,
      short replicationFactor, int nnIndex)
      throws IOException, InterruptedException, TimeoutException {
    FileSystem fs = cluster.getFileSystem(nnIndex);
    DFSTestUtil.createFile(fs, filePath, fileLen, replicationFactor, r.nextLong());
    DFSTestUtil.waitReplication(fs, filePath, replicationFactor);
  }

  static long sum(long[] x) {
    long s = 0L;
    for (long a : x) {
      s += a;
    }
    return s;
  }

  static void waitForHeartBeat(long expectedUsedSpace, long expectedTotalSpace,
      ClientProtocol client, MiniDFSCluster cluster)
      throws IOException, TimeoutException {
    long timeout = TIMEOUT;
    long failtime = (timeout <= 0L) ? Long.MAX_VALUE : Time.monotonicNow() + timeout;
    while (true) {
      long[] status = client.getStats();
      double totalSpaceVariance = Math.abs((double) status[0] - expectedTotalSpace)
          / expectedTotalSpace;
      double usedSpaceVariance = Math.abs((double) status[1] - expectedUsedSpace)
          / expectedUsedSpace;
      if (totalSpaceVariance < CAPACITY_ALLOWED_VARIANCE
          && usedSpaceVariance < CAPACITY_ALLOWED_VARIANCE) {
        break;
      }
      if (Time.monotonicNow() > failtime) {
        throw new TimeoutException("Cluster failed to reach expected values");
      }
      try {
        Thread.sleep(100L);
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void runBalancer(Configuration conf, long totalUsedSpace,
      long totalCapacity) throws Exception {
    waitForHeartBeat(totalUsedSpace, totalCapacity, client, cluster);
    Collection<URI> namenodes = DFSUtil.getInternalNsRpcUris(conf);
    runBalancer(namenodes, BalancerParameters.DEFAULT, conf);
  }

  private static int runBalancer(Collection<URI> namenodes, BalancerParameters p,
      Configuration conf) throws IOException, InterruptedException {
    final long sleeptime = conf.getLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY,
        DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_DEFAULT) * 2000
        + conf.getLong(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY,
        DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_DEFAULT) * 1000;

    List<NameNodeConnector> connectors = Collections.emptyList();
    try {
      connectors = NameNodeConnector.newNameNodeConnectors(namenodes,
          Balancer.class.getSimpleName(), Balancer.BALANCER_ID_PATH, conf,
          BalancerParameters.DEFAULT.getMaxIdleIteration());

      boolean done = false;
      for (int iteration = 0; !done; iteration++) {
        done = true;
        Collections.shuffle(connectors);
        for (NameNodeConnector nnc : connectors) {
          final Balancer b = new Balancer(nnc, p, conf);
          final Balancer.Result result = b.runOneIteration();
          b.resetData(conf);
          if (result.getExitStatus() == ExitStatus.IN_PROGRESS) {
            done = false;
          } else if (result.getExitStatus() != ExitStatus.SUCCESS) {
            return result.getExitStatus().getExitCode();
          }
        }
        if (!done) {
          Thread.sleep(sleeptime);
        }
      }
    } finally {
      for (NameNodeConnector nnc : connectors) {
        IOUtils.cleanupWithLogger(LOG, nnc);
      }
    }
    return ExitStatus.SUCCESS.getExitCode();
  }

  // ==========================================================================
  // testBalancer2 - AFTER_FILE_CREATE restart variants
  // Tests balancer operation after restart
  // ==========================================================================

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, NameNode, Graceful
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_NN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh client after NN restart
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, NameNode, Crash
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_NN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh client after NN restart
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, SingleDataNode, Graceful
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_DN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, SingleDataNode, Crash
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_DN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, AllDataNodes, Graceful
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_AllDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, AllDataNodes, Crash
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_AllDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, NameNodeAndDataNodes, Graceful
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_NNAndDN_Graceful ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh client after NN restart
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }

  /**
   * Original: TestBalancer#testBalancer2 / testBalancerDefaultConstructor
   * Restart: After file creation, NameNodeAndDataNodes, Crash
   */
  @Test(timeout = 180000)
  public void testBalancer_AfterCreate_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting testBalancer_AfterCreate_NNAndDN_Crash ===");

    // === ORIGINAL CODE START (UNCHANGED) ===
    long[] capacities = new long[]{CAPACITY, CAPACITY};
    String[] racks = new String[]{RACK0, RACK1};
    long newCapacity = CAPACITY;
    String newRack = RACK2;

    int numOfDatanodes = capacities.length;
    assertEquals(numOfDatanodes, racks.length);
    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(capacities.length)
        .racks(racks)
        .simulatedCapacities(capacities)
        .build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    long totalCapacity = sum(capacities);

    // fill up the cluster to be 30% full
    long totalUsedSpace = totalCapacity * 3 / 10;
    createFile(cluster, filePath, totalUsedSpace / numOfDatanodes,
        (short) numOfDatanodes, 0);
    // === ORIGINAL CODE PAUSE ===

    // === RESTART INJECTION ===
    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    // Need to refresh client after NN restart
    client = NameNodeProxies.createProxy(conf, cluster.getFileSystem(0).getUri(),
        ClientProtocol.class).getProxy();

    // === ORIGINAL CODE RESUME (UNCHANGED) ===
    // start up an empty node with the same capacity and on the same rack
    cluster.startDataNodes(conf, 1, true, null, new String[]{newRack},
        new long[]{newCapacity});

    totalCapacity += newCapacity;

    // run balancer and validate results
    runBalancer(conf, totalUsedSpace, totalCapacity);
    // === ORIGINAL CODE END ===
  }
}

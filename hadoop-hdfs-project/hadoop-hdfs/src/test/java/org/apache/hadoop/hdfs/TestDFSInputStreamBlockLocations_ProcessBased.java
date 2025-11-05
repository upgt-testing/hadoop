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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SIZE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.Time;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSInputStreamBlockLocations}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSInputStreamBlockLocations Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSInputStreamBlockLocations_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final int BLOCK_SIZE = 1024 * 1024;
  private static final String[] RACKS = new String[] {
      "/d1/r1", "/d1/r1", "/d1/r2", "/d1/r2", "/d1/r2", "/d2/r3", "/d2/r3" };
  private static final int NUM_DATA_NODES = RACKS.length;
  private static final short REPLICATION_FACTOR = (short) 4;
  private final int staleInterval = 8000;
  private final int numOfBlocks = 24;
  private final int fileLength = numOfBlocks * BLOCK_SIZE;
  private final int dfsClientPrefetchSize = fileLength / 2;
  // locatedBlocks expiration set to 1 hour
  private final long dfsInputLocationsTimeout = 60 * 60 * 1000L;

  private DFSClient dfsClient;
  private Path filePath;

  @Parameter(0)
  public String upgradeCheckpoint;

  @Parameter(1)
  public Boolean enableBlkExpiration;

  @Parameters(name = "upgrade-at={0}, cacheExpiration={1}")
  public static Collection<Object[]> checkpoints() {
    return Arrays.asList(new Object[][] {
      {UpgradeCheckpoints.NO_UPGRADE, Boolean.TRUE},
      {UpgradeCheckpoints.NO_UPGRADE, Boolean.FALSE},
      {UpgradeCheckpoints.AFTER_CLUSTER_START, Boolean.TRUE},
      {UpgradeCheckpoints.AFTER_CLUSTER_START, Boolean.FALSE},
      {UpgradeCheckpoints.AFTER_FILE_CREATE, Boolean.TRUE},
      {UpgradeCheckpoints.AFTER_FILE_CREATE, Boolean.FALSE},
      {"BEFORE_VERIFICATION", Boolean.TRUE},
      {"BEFORE_VERIFICATION", Boolean.FALSE}
    });
  }

  @Before
  public void setup() throws Exception {
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_AVOID_STALE_DATANODE_FOR_READ_KEY, true);
    // set the heartbeat intervals and stale considerations
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_STALE_DATANODE_INTERVAL_KEY,
        staleInterval);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY,
        staleInterval / 2);
    // disable shortcircuit reading
    conf.setBoolean(HdfsClientConfigKeys.Read.ShortCircuit.KEY, false);
    // set replication factor
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, REPLICATION_FACTOR);
    // set block size and other sizes
    conf.setLong(DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setLong(HdfsClientConfigKeys.Read.PREFETCH_SIZE_KEY,
        dfsClientPrefetchSize);
    if (enableBlkExpiration) {
      // set the refresh locations for every dfsInputLocationsTimeout
      conf.setLong(
          HdfsClientConfigKeys.DFS_CLIENT_REFRESH_READ_BLOCK_LOCATIONS_MS_KEY,
          dfsInputLocationsTimeout);
    }
    // start the cluster and create a DFSClient
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(NUM_DATA_NODES).racks(RACKS).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    // Verify DataNode count using client API
    assertEquals(NUM_DATA_NODES, cluster.getNumDataNodes());

    InetSocketAddress addr = cluster.getNameNodeRpcAddress();
    dfsClient = new DFSClient(addr, conf);
  }

  @After
  public void teardown() throws IOException {
    if (dfsClient != null) {
      dfsClient.close();
      dfsClient = null;
    }
    if (fs != null && filePath != null) {
      fs.deleteOnExit(filePath);
    }
  }

  @Test
  public void testRefreshBlockLocations() throws Exception {
    final String fileName = "/test_cache_locations";
    filePath = createFile(fileName);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    try (DFSInputStream fin = dfsClient.open(fileName)) {
      LocatedBlocks existing = fin.locatedBlocks;
      long lastRefreshedAt = fin.getLastRefreshedBlocksAtForTesting();

      assertFalse("should not have attempted refresh",
          fin.refreshBlockLocations(null));
      assertEquals("should not have updated lastRefreshedAt",
          lastRefreshedAt, fin.getLastRefreshedBlocksAtForTesting());
      assertSame("should not have modified locatedBlocks",
          existing, fin.locatedBlocks);

      // fake a dead node to force refresh
      // refreshBlockLocations should return true, indicating we attempted a refresh
      // nothing should be changed, because locations have not changed
      fin.addToLocalDeadNodes(dfsClient.datanodeReport(DatanodeReportType.LIVE)[0]);
      assertTrue("should have attempted refresh",
          fin.refreshBlockLocations(null));
      verifyChanged(fin, existing, lastRefreshedAt);

      // reset
      lastRefreshedAt = fin.getLastRefreshedBlocksAtForTesting();
      existing = fin.locatedBlocks;

      checkpoint("BEFORE_VERIFICATION");

      // It's hard to test explicitly for non-local nodes, but we can fake it
      // because we also treat unresolved as non-local. Pass in a cache where all the datanodes
      // are unresolved hosts.
      Map<String, InetSocketAddress> mockAddressCache = new HashMap<>();
      InetSocketAddress unresolved = InetSocketAddress.createUnresolved("www.google.com", 80);

      // Use client-side API to get DataNode info instead of direct DataNode access
      DatanodeInfo[] datanodes = ((DistributedFileSystem)fs).getDataNodeStats();
      for (DatanodeInfo datanodeInfo : datanodes) {
        mockAddressCache.put(datanodeInfo.getDatanodeUuid(), unresolved);
      }

      assertTrue("should have attempted refresh",
          fin.refreshBlockLocations(mockAddressCache));
      verifyChanged(fin, existing, lastRefreshedAt);
    }
  }

  private void verifyChanged(DFSInputStream fin, LocatedBlocks existing, long lastRefreshedAt) {
    assertTrue("lastRefreshedAt should have incremented",
        fin.getLastRefreshedBlocksAtForTesting() > lastRefreshedAt);
    assertNotSame("located blocks should have changed",
        existing, fin.locatedBlocks);
    assertTrue("deadNodes should be empty",
        fin.getLocalDeadNodes().isEmpty());
  }

  @Test
  public void testDeferredRegistrationStatefulRead() throws Exception {
    testWithRegistrationMethod(DFSInputStream::read);
  }

  @Test
  public void testDeferredRegistrationPositionalRead() throws Exception {
    testWithRegistrationMethod(fin -> fin.readFully(0, new byte[1]));
  }

  @Test
  public void testDeferredRegistrationGetAllBlocks() throws Exception {
    testWithRegistrationMethod(DFSInputStream::getAllBlocks);
  }

  @FunctionalInterface
  interface ThrowingConsumer {
    void accept(DFSInputStream fin) throws IOException;
  }

  private void testWithRegistrationMethod(ThrowingConsumer registrationMethod) throws Exception {
    final String fileName = "/test_cache_locations";
    filePath = createFile(fileName);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    DFSInputStream fin = null;
    try {
      fin = dfsClient.open(fileName);
      assertFalse("should not be tracking input stream on open",
          dfsClient.getLocatedBlockRefresher().isInputStreamTracked(fin));

      // still not registered because it hasn't been an hour by the time we call this
      registrationMethod.accept(fin);
      assertFalse("should not be tracking input stream after first read",
          dfsClient.getLocatedBlockRefresher().isInputStreamTracked(fin));

      checkpoint("BEFORE_VERIFICATION");

      // artificially make it have been an hour
      fin.setLastRefreshedBlocksAtForTesting(Time.monotonicNow() - (dfsInputLocationsTimeout + 1));
      registrationMethod.accept(fin);
      assertEquals("SHOULD be tracking input stream on read after interval, only if enabled",
          enableBlkExpiration, dfsClient.getLocatedBlockRefresher().isInputStreamTracked(fin));
    } finally {
      if (fin != null) {
        fin.close();
        assertFalse(dfsClient.getLocatedBlockRefresher().isInputStreamTracked(fin));
      }
      fs.delete(filePath, true);
    }
  }

  private Path createFile(String fileName) throws Exception {
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    Path path = new Path(fileName);
    try (FSDataOutputStream fout = fs.create(path, REPLICATION_FACTOR)) {
      fout.write(new byte[(fileLength)]);
    }
    return path;
  }
}

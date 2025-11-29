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
package org.apache.hadoop.hdfs.server.namenode;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SIZE_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_CACHEREPORT_INTERVAL_MSEC_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_DATANODE_MAX_LOCKED_MEMORY_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_PATH_BASED_CACHE_REFRESH_INTERVAL_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystemTestHelper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.CacheDirectiveEntry;
import org.apache.hadoop.hdfs.protocol.CacheDirectiveInfo;
import org.apache.hadoop.hdfs.protocol.CachePoolEntry;
import org.apache.hadoop.hdfs.protocol.CachePoolInfo;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor.CachedBlocksList.Type;
import org.apache.hadoop.io.nativeio.NativeIO;
import org.apache.hadoop.io.nativeio.NativeIO.POSIX.CacheManipulator;
import org.apache.hadoop.io.nativeio.NativeIO.POSIX.NoMlockCacheManipulator;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.util.GSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

import java.util.function.Supplier;

/**
 * Restart-injected version of TestCacheDirectives.
 *
 * Tests cache pool and cache directive operations survive component restarts.
 * The original TestCacheDirectives#testCacheManagerRestart already tests
 * NN restart scenarios, so we focus on testing cache operations at different
 * restart injection points.
 *
 * Original tests transformed:
 * - testBasicPoolOperations: Tests cache pool CRUD operations
 * - testAddRemoveDirectives: Tests cache directive CRUD operations
 *
 * Restart points:
 * - AfterPoolAdd: After creating cache pools
 * - AfterDirectiveAdd: After adding cache directives
 *
 * Generated variants:
 * - 2 restart points x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestCacheDirectives_RestartInjected {
  static final Logger LOG = LoggerFactory.getLogger(TestCacheDirectives_RestartInjected.class);

  private static final long BLOCK_SIZE = 4096;
  private static final int NUM_DATANODES = 4;
  private static final long CACHE_CAPACITY = 64 * 1024 / NUM_DATANODES;

  private Configuration conf;
  private MiniDFSCluster cluster;
  private DistributedFileSystem dfs;
  private NameNode namenode;
  private CacheManipulator prevCacheManipulator;

  static {
    NativeIO.POSIX.setCacheManipulator(new NoMlockCacheManipulator());
  }

  private static HdfsConfiguration createCachingConf() {
    HdfsConfiguration conf = new HdfsConfiguration();
    conf.setLong(DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setLong(DFS_DATANODE_MAX_LOCKED_MEMORY_KEY, CACHE_CAPACITY);
    conf.setLong(DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setLong(DFS_CACHEREPORT_INTERVAL_MSEC_KEY, 1000);
    conf.setLong(DFS_NAMENODE_PATH_BASED_CACHE_REFRESH_INTERVAL_MS, 1000);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_LIST_CACHE_POOLS_NUM_RESPONSES, 2);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_LIST_CACHE_DIRECTIVES_NUM_RESPONSES, 2);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    return conf;
  }

  @Before
  public void setup() throws Exception {
    conf = createCachingConf();
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_DATANODES).build();
    cluster.waitActive();
    dfs = cluster.getFileSystem();
    namenode = cluster.getNameNode();
    prevCacheManipulator = NativeIO.POSIX.getCacheManipulator();
    NativeIO.POSIX.setCacheManipulator(new NoMlockCacheManipulator());
  }

  @After
  public void teardown() throws Exception {
    if (dfs != null) {
      // Remove cache directives left behind by tests
      RemoteIterator<CacheDirectiveEntry> iter = dfs.listCacheDirectives(null);
      while (iter.hasNext()) {
        dfs.removeCacheDirective(iter.next().getInfo().getId());
      }
    }
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
    NativeIO.POSIX.setCacheManipulator(prevCacheManipulator);
  }

  /**
   * Wait for the NameNode to have an expected number of cached blocks.
   */
  private void waitForCachedBlocks(NameNode nn, final int expectedCachedBlocks,
      final int expectedCachedReplicas, final String logString) throws Exception {
    final FSNamesystem namesystem = nn.getNamesystem();
    final CacheManager cacheManager = namesystem.getCacheManager();
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        int numCachedBlocks = 0, numCachedReplicas = 0;
        namesystem.readLock();
        try {
          GSet<CachedBlock, CachedBlock> cachedBlocks = cacheManager.getCachedBlocks();
          if (cachedBlocks != null) {
            for (Iterator<CachedBlock> iter = cachedBlocks.iterator(); iter.hasNext(); ) {
              CachedBlock cachedBlock = iter.next();
              numCachedBlocks++;
              numCachedReplicas += cachedBlock.getDatanodes(Type.CACHED).size();
            }
          }
        } finally {
          namesystem.readUnlock();
        }
        if (expectedCachedBlocks == -1 || numCachedBlocks == expectedCachedBlocks) {
          if (expectedCachedReplicas == -1 || numCachedReplicas == expectedCachedReplicas) {
            return true;
          }
        }
        return false;
      }
    }, 500, 60000);
  }

  // ============================================================
  // Test: Pool operations with restart after adding pool
  // AfterPoolAdd x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create cache pool, inject restart, verify pool persisted.
   * Based on TestCacheDirectives#testBasicPoolOperations
   */
  private void testPoolOperationsWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    final String poolName = "pool1";
    CachePoolInfo info = new CachePoolInfo(poolName)
        .setOwnerName("bob").setGroupName("bobgroup")
        .setMode(new FsPermission((short)0755)).setLimit(150l);

    // === ORIGINAL CODE: Add a pool ===
    dfs.addCachePool(info);

    // Verify pool was added
    RemoteIterator<CachePoolEntry> poolIter = dfs.listCachePools();
    assertTrue("Pool should exist after add", poolIter.hasNext());
    CachePoolInfo poolInfo = poolIter.next().getInfo();
    assertEquals(poolName, poolInfo.getPoolName());

    // === RESTART INJECTION POINT: After pool add ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    dfs = cluster.getFileSystem();
    namenode = cluster.getNameNode();
    verifyClusterHealth(cluster, dfs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify pool persisted ===
    poolIter = dfs.listCachePools();
    assertTrue("Pool should persist after restart", poolIter.hasNext());
    poolInfo = poolIter.next().getInfo();
    assertEquals("Pool name should persist", poolName, poolInfo.getPoolName());
    assertEquals("Pool owner should persist", "bob", poolInfo.getOwnerName());
    assertEquals("Pool group should persist", "bobgroup", poolInfo.getGroupName());
    assertEquals("Pool mode should persist", new FsPermission((short)0755), poolInfo.getMode());
    assertEquals("Pool limit should persist", 150l, (long)poolInfo.getLimit());

    // Modify the pool after restart
    info.setOwnerName("jane").setGroupName("janegroup")
        .setMode(new FsPermission((short)0700)).setLimit(314l);
    dfs.modifyCachePool(info);

    poolIter = dfs.listCachePools();
    poolInfo = poolIter.next().getInfo();
    assertEquals("jane", poolInfo.getOwnerName());
    assertEquals("janegroup", poolInfo.getGroupName());
    assertEquals(new FsPermission((short)0700), poolInfo.getMode());
    assertEquals(314l, (long)poolInfo.getLimit());

    // Remove the pool
    dfs.removeCachePool(poolName);
    poolIter = dfs.listCachePools();
    assertFalse("Pool should be removed", poolIter.hasNext());
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_NN_Graceful() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_NN_Crash() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_SingleDN_Graceful() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_SingleDN_Crash() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_AllDN_Graceful() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_AllDN_Crash() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_NNDN_Graceful() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testPoolOperations_AfterPoolAdd_NNDN_Crash() throws Exception {
    testPoolOperationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: Directive operations with restart after adding directives
  // AfterDirectiveAdd x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create pool and directives, inject restart, verify directives persisted.
   * Based on TestCacheDirectives#testAddRemoveDirectives
   */
  private void testDirectiveOperationsWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    // === ORIGINAL CODE: Create pools ===
    dfs.addCachePool(new CachePoolInfo("pool1").setMode(new FsPermission((short)0777)));
    dfs.addCachePool(new CachePoolInfo("pool2").setMode(new FsPermission((short)0777)));

    // === ORIGINAL CODE: Create directives ===
    CacheDirectiveInfo alpha = new CacheDirectiveInfo.Builder()
        .setPath(new Path("/alpha"))
        .setPool("pool1")
        .build();
    CacheDirectiveInfo beta = new CacheDirectiveInfo.Builder()
        .setPath(new Path("/beta"))
        .setPool("pool2")
        .build();

    long alphaId = dfs.addCacheDirective(alpha);
    long betaId = dfs.addCacheDirective(beta);

    // Verify directives were added
    RemoteIterator<CacheDirectiveEntry> iter = dfs.listCacheDirectives(null);
    assertTrue("Should have directives", iter.hasNext());

    // === RESTART INJECTION POINT: After directives add ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    dfs = cluster.getFileSystem();
    namenode = cluster.getNameNode();
    verifyClusterHealth(cluster, dfs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify directives persisted ===
    iter = dfs.listCacheDirectives(null);
    assertTrue("Directives should persist after restart", iter.hasNext());

    // Count directives
    int count = 0;
    boolean foundAlpha = false, foundBeta = false;
    while (iter.hasNext()) {
      CacheDirectiveInfo info = iter.next().getInfo();
      count++;
      if (info.getId() == alphaId) {
        assertEquals("/alpha", info.getPath().toUri().getPath());
        assertEquals("pool1", info.getPool());
        foundAlpha = true;
      } else if (info.getId() == betaId) {
        assertEquals("/beta", info.getPath().toUri().getPath());
        assertEquals("pool2", info.getPool());
        foundBeta = true;
      }
    }
    assertEquals("Should have 2 directives after restart", 2, count);
    assertTrue("Alpha directive should persist", foundAlpha);
    assertTrue("Beta directive should persist", foundBeta);

    // Modify directive after restart
    dfs.modifyCacheDirective(new CacheDirectiveInfo.Builder()
        .setId(alphaId)
        .setReplication((short)555)
        .build());

    iter = dfs.listCacheDirectives(new CacheDirectiveInfo.Builder().setId(alphaId).build());
    assertTrue(iter.hasNext());
    CacheDirectiveInfo modified = iter.next().getInfo();
    assertEquals(alphaId, modified.getId().longValue());
    assertEquals((short)555, modified.getReplication().shortValue());

    // Remove directives
    dfs.removeCacheDirective(alphaId);
    dfs.removeCacheDirective(betaId);
    iter = dfs.listCacheDirectives(null);
    assertFalse("All directives should be removed", iter.hasNext());
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_NN_Graceful() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_NN_Crash() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_SingleDN_Graceful() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_SingleDN_Crash() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_AllDN_Graceful() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_AllDN_Crash() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_NNDN_Graceful() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testDirectiveOperations_AfterDirectiveAdd_NNDN_Crash() throws Exception {
    testDirectiveOperationsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

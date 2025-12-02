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

package org.apache.hadoop.hdfs.security.token.block;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.Test;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestBlockToken.
 * Tests block token operations survive component restarts.
 *
 * Original test: testLastLocatedBlockTokenExpiry (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestBlockToken_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestBlockToken_RestartInjected.class);

  @Before
  public void disableKerberos() {
    Configuration conf = new Configuration();
    conf.set(HADOOP_SECURITY_AUTHENTICATION, "simple");
    UserGroupInformation.setConfiguration(conf);
  }

  /**
   * Log detailed state about file, blocks, leases, and replicas.
   */
  private void logFileAndBlockState(MiniDFSCluster cluster, DistributedFileSystem fs,
      Path p, String context) throws Exception {
    LOG.info("=== {} ===", context);

    try {
      // File status
      LOG.info("File exists: {}", fs.exists(p));
      if (!fs.exists(p)) {
        LOG.warn("File does not exist!");
        return;
      }

      LOG.info("File status: {}", fs.getFileStatus(p));

      // Get FSNamesystem and check if file is under construction
      FSNamesystem namesystem = cluster.getNamesystem();
      namesystem.readLock();
      try {
        // Get INodeFile to check blocks
        INodeFile inode = namesystem.getFSDirectory().getINode(p.toString()).asFile();
        if (inode == null) {
          LOG.warn("INode not found for path: {}", p);
          return;
        }

        boolean isUC = inode.isUnderConstruction();
        LOG.info("Is file under construction: {}", isUC);

        BlockInfo[] blocks = inode.getBlocks();
        LOG.info("Number of blocks in INode: {}", blocks.length);

        for (int i = 0; i < blocks.length; i++) {
          BlockInfo block = blocks[i];
          LOG.info("Block[{}]: {}", i, block);
          LOG.info("  Block ID: {}", block.getBlockId());
          LOG.info("  Block length: {}", block.getNumBytes());
          LOG.info("  Is complete: {}", block.isComplete());
          LOG.info("  Num replicas: {}", block.numNodes());
        }

      } finally {
        namesystem.readUnlock();
      }

      // Get located blocks (what client sees)
      try {
        LocatedBlocks locatedBlocks = fs.getClient().getLocatedBlocks(p.toString(), 0);
        LOG.info("Located blocks: {}", locatedBlocks);
        LOG.info("  File length: {}", locatedBlocks.getFileLength());
        LOG.info("  Num located blocks: {}", locatedBlocks.getLocatedBlocks().size());
        LOG.info("  Is under construction: {}", locatedBlocks.isUnderConstruction());
        LOG.info("  Last located block: {}", locatedBlocks.getLastLocatedBlock());

        if (locatedBlocks.getLastLocatedBlock() != null) {
          LocatedBlock lastBlock = locatedBlocks.getLastLocatedBlock();
          LOG.info("  Last block size: {}", lastBlock.getBlockSize());
          LOG.info("  Last block offset: {}", lastBlock.getStartOffset());
          LOG.info("  Last block num locations: {}", lastBlock.getLocations().length);
        }
      } catch (Exception e) {
        LOG.error("Failed to get located blocks: {}", e.getMessage(), e);
      }

      // Check DataNode replica visible length
      if (cluster.getDataNodes().size() > 0) {
        DataNode dn = cluster.getDataNodes().get(0);
        LOG.info("DataNode: {}", dn.getDatanodeId());

        try {
          LocatedBlocks locatedBlocks = fs.getClient().getLocatedBlocks(p.toString(), 0);
          if (locatedBlocks.getLocatedBlocks().size() > 0) {
            LocatedBlock firstBlock = locatedBlocks.getLocatedBlocks().get(0);
            ExtendedBlock extBlock = firstBlock.getBlock();

            FsDatasetSpi<?> dataset = dn.getFSDataset();
            long visibleLength = dataset.getReplicaVisibleLength(extBlock);
            LOG.info("DataNode replica visible length: {}", visibleLength);
          }
        } catch (Exception e) {
          LOG.error("Failed to get replica visible length: {}", e.getMessage(), e);
        }
      }

    } catch (Exception e) {
      LOG.error("Error logging file/block state: ", e);
    }

    LOG.info("=== END {} ===", context);
  }

  /**
   * Core test logic for last in-progress block token expiry with restart injection.
   * 1. Write file with one block which is in-progress.
   * 2. hflush the data.
   * 3. Inject restart.
   * 4. Open input stream and close the output stream.
   * 5. Wait for block token expiration and read the data.
   * 6. Read should be success.
   */
  private void testLastLocatedBlockTokenExpiryWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    Configuration conf = new Configuration();
    conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(1).build()) {
      cluster.waitClusterUp();
      final NameNode nn = cluster.getNameNode();
      final BlockManager bm = nn.getNamesystem().getBlockManager();
      final BlockTokenSecretManager sm = bm.getBlockTokenSecretManager();

      // set a short token lifetime (1 second)
      SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);

      DistributedFileSystem fs = cluster.getFileSystem();
      Path p = new Path("/tmp/abc.log");
      FSDataOutputStream out = fs.create(p);
      byte[] data = "hello\n".getBytes(StandardCharsets.UTF_8);
      out.write(data);
      out.hflush();

      // === DIAGNOSTIC: Before restart ===
      LOG.info("=== BEFORE RESTART DIAGNOSTICS ===");
      logFileAndBlockState(cluster, fs, p, "BEFORE RESTART");

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // === DIAGNOSTIC: After restart ===
      LOG.info("=== AFTER RESTART DIAGNOSTICS ===");
      logFileAndBlockState(cluster, fs, p, "AFTER RESTART");

      // === TEST: Wait for automatic lease recovery ===
      LOG.info("=== WAITING FOR AUTOMATIC LEASE RECOVERY ===");
      LOG.info("Lease soft limit: {}", cluster.getConfiguration(0).getLong("dfs.namenode.lease-soft-limit-millis", 60000));
      LOG.info("Lease hard limit: {}", cluster.getConfiguration(0).getLong("dfs.namenode.lease-hard-limit-millis", 60000));

      // Wait to see if automatic lease recovery happens
      // Note: Default lease soft limit is 60 seconds, hard limit is 60 seconds
      LOG.info("Waiting 10 seconds to see if automatic recovery happens...");
      Thread.sleep(10000);

      LOG.info("=== AFTER WAITING DIAGNOSTICS ===");
      logFileAndBlockState(cluster, fs, p, "AFTER WAITING");

      // === TEST: Now try explicit lease recovery ===
      LOG.info("=== TESTING EXPLICIT LEASE RECOVERY ===");
      boolean recovered = cluster.getFileSystem().recoverLease(p);
      LOG.info("Lease recovery initiated: {}", recovered);

      // Wait for lease recovery to complete
      Thread.sleep(5000);

      LOG.info("=== AFTER LEASE RECOVERY DIAGNOSTICS ===");
      logFileAndBlockState(cluster, fs, p, "AFTER LEASE RECOVERY");

      // Re-set block token lifetime after potential NN restart
      if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
        final NameNode nnAfter = cluster.getNameNode();
        final BlockManager bmAfter = nnAfter.getNamesystem().getBlockManager();
        final BlockTokenSecretManager smAfter = bmAfter.getBlockTokenSecretManager();
        SecurityTestUtil.setBlockTokenLifetime(smAfter, 1000L);
      }

      FSDataInputStream in = fs.open(p);
      out.close();

      // wait for last block token to expire
      Thread.sleep(2000L);

      byte[] readData = new byte[data.length];
      long startTime = System.currentTimeMillis();
      in.read(readData);
      // DFSInputStream#refetchLocations() minimum wait for 1sec to refetch
      // complete located blocks.
      assertTrue("Should not wait for refetch complete located blocks",
          1000L > (System.currentTimeMillis() - startTime));
    }
  }

  // ============================================================
  // Test variants: testLastLocatedBlockTokenExpiry with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NN_Crash() throws Exception {
    LOG.info("=== TESTING WITH NAMENODE-ONLY RESTART (NO DN RESTART) ===");
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Graceful() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Crash() throws Exception {
    testLastLocatedBlockTokenExpiryWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

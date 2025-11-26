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

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.IPC_CLIENT_CONNECT_MAX_RETRIES_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManager;
import org.apache.hadoop.hdfs.security.token.block.SecurityTestUtil;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.security.token.Token;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestBlockTokenWithDFS.
 * Tests block token operations with DFS survive component restarts.
 *
 * Original tests: testAppend, testWrite (both have hflush)
 *
 * Generated variants:
 * - 2 tests x AfterHflush x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestBlockTokenWithDFS_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockTokenWithDFS_RestartInjected.class);

  protected static int BLOCK_SIZE = 1024;
  protected static int FILE_SIZE = 2 * BLOCK_SIZE;
  private static final String FILE_TO_WRITE = "/fileToWrite.dat";
  private static final String FILE_TO_APPEND = "/fileToAppend.dat";

  public static byte[] generateBytes(int fileSize) {
    Random r = new Random();
    byte[] rawData = new byte[fileSize];
    r.nextBytes(rawData);
    return rawData;
  }

  // read a file using blockSeekTo()
  private boolean checkFile1(FSDataInputStream in, byte[] expected) {
    byte[] toRead = new byte[expected.length];
    int totalRead = 0;
    int nRead = 0;
    try {
      while ((nRead = in.read(toRead, totalRead, toRead.length - totalRead)) > 0) {
        totalRead += nRead;
      }
    } catch (IOException e) {
      return false;
    }
    assertEquals("Cannot read file.", toRead.length, totalRead);
    return checkFile(toRead, expected);
  }

  private boolean checkFile(byte[] fileToCheck, byte[] expected) {
    if (fileToCheck.length != expected.length) {
      return false;
    }
    for (int i = 0; i < fileToCheck.length; i++) {
      if (fileToCheck[i] != expected[i]) {
        return false;
      }
    }
    return true;
  }

  // creates a file and returns a descriptor for writing to it
  private static FSDataOutputStream writeFile(FileSystem fileSys, Path name,
      short repl, long blockSize) throws IOException {
    FSDataOutputStream stm = fileSys.create(name, true, fileSys.getConf()
        .getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096), repl, blockSize);
    return stm;
  }

  // get a conf for testing
  protected Configuration getConf(int numDataNodes) {
    Configuration conf = new Configuration();
    conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setInt("io.bytes.per.checksum", BLOCK_SIZE);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, numDataNodes);
    conf.setInt(IPC_CLIENT_CONNECT_MAX_RETRIES_KEY, 0);
    // Set short retry timeouts so this test runs faster
    conf.setInt(HdfsClientConfigKeys.Retry.WINDOW_BASE_KEY, 10);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    return conf;
  }

  /**
   * Core test logic for APPEND operation with restart injection.
   */
  private void testAppendWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    MiniDFSCluster cluster = null;
    int numDataNodes = 2;
    Configuration conf = getConf(numDataNodes);

    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(numDataNodes).build();
      cluster.waitActive();
      assertEquals(numDataNodes, cluster.getDataNodes().size());

      final NameNode nn = cluster.getNameNode();
      final BlockManager bm = nn.getNamesystem().getBlockManager();
      final BlockTokenSecretManager sm = bm.getBlockTokenSecretManager();

      // set a short token lifetime (1 second)
      SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);
      Path fileToAppend = new Path(FILE_TO_APPEND);
      FileSystem fs = cluster.getFileSystem();
      byte[] expected = generateBytes(FILE_SIZE);
      // write a one-byte file
      FSDataOutputStream stm = writeFile(fs, fileToAppend,
          (short) numDataNodes, BLOCK_SIZE);
      stm.write(expected, 0, 1);
      stm.close();
      // open the file again for append
      stm = fs.append(fileToAppend);
      int mid = expected.length - 1;
      stm.write(expected, 1, mid - 1);
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Re-set block token lifetime after potential NN restart
      if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
        final NameNode nnAfter = cluster.getNameNode();
        final BlockManager bmAfter = nnAfter.getNamesystem().getBlockManager();
        final BlockTokenSecretManager smAfter = bmAfter.getBlockTokenSecretManager();
        SecurityTestUtil.setBlockTokenLifetime(smAfter, 1000L);
      }

      /*
       * wait till token used in stm expires
       */
      Token<BlockTokenIdentifier> token = DFSTestUtil.getBlockToken(stm);
      while (!SecurityTestUtil.isBlockTokenExpired(token)) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
      }

      // For restart test, we skip stopping datanode since we already did restart
      // Just append the rest of the file
      stm.write(expected, mid, expected.length - mid);
      stm.close();
      // check if append is successful
      FSDataInputStream in5 = fs.open(fileToAppend);
      assertTrue(checkFile1(in5, expected));
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  /**
   * Core test logic for WRITE operation with restart injection.
   */
  private void testWriteWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    MiniDFSCluster cluster = null;
    int numDataNodes = 2;
    Configuration conf = getConf(numDataNodes);

    try {
      cluster = new MiniDFSCluster.Builder(conf).numDataNodes(numDataNodes).build();
      cluster.waitActive();
      assertEquals(numDataNodes, cluster.getDataNodes().size());

      final NameNode nn = cluster.getNameNode();
      final BlockManager bm = nn.getNamesystem().getBlockManager();
      final BlockTokenSecretManager sm = bm.getBlockTokenSecretManager();

      // set a short token lifetime (1 second)
      SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);
      Path fileToWrite = new Path(FILE_TO_WRITE);
      FileSystem fs = cluster.getFileSystem();

      byte[] expected = generateBytes(FILE_SIZE);
      FSDataOutputStream stm = writeFile(fs, fileToWrite, (short) numDataNodes,
          BLOCK_SIZE);
      // write a partial block
      int mid = expected.length - 1;
      stm.write(expected, 0, mid);
      stm.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Re-set block token lifetime after potential NN restart
      if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
        final NameNode nnAfter = cluster.getNameNode();
        final BlockManager bmAfter = nnAfter.getNamesystem().getBlockManager();
        final BlockTokenSecretManager smAfter = bmAfter.getBlockTokenSecretManager();
        SecurityTestUtil.setBlockTokenLifetime(smAfter, 1000L);
      }

      /*
       * wait till token used in stm expires
       */
      Token<BlockTokenIdentifier> token = DFSTestUtil.getBlockToken(stm);
      while (!SecurityTestUtil.isBlockTokenExpired(token)) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
      }

      // For restart test, we skip stopping datanode since we already did restart
      // Just write the rest of the file
      stm.write(expected, mid, expected.length - mid);
      stm.close();
      // check if write is successful
      FSDataInputStream in4 = fs.open(fileToWrite);
      assertTrue(checkFile1(in4, expected));
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testAppend with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_NN_Graceful() throws Exception {
    testAppendWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_NN_Crash() throws Exception {
    testAppendWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_SingleDN_Graceful() throws Exception {
    testAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_SingleDN_Crash() throws Exception {
    testAppendWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_AllDN_Graceful() throws Exception {
    testAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_AllDN_Crash() throws Exception {
    testAppendWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_NNDN_Graceful() throws Exception {
    testAppendWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppend_AfterHflush_NNDN_Crash() throws Exception {
    testAppendWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testWrite with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_NN_Graceful() throws Exception {
    testWriteWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_NN_Crash() throws Exception {
    testWriteWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_SingleDN_Graceful() throws Exception {
    testWriteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_SingleDN_Crash() throws Exception {
    testWriteWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_AllDN_Graceful() throws Exception {
    testWriteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_AllDN_Crash() throws Exception {
    testWriteWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_NNDN_Graceful() throws Exception {
    testWriteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testWrite_AfterHflush_NNDN_Crash() throws Exception {
    testWriteWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

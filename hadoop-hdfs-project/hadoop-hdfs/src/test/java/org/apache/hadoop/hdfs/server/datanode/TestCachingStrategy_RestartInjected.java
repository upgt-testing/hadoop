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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.server.namenode.EditLogFileOutputStream;
import org.apache.hadoop.io.nativeio.NativeIO;
import org.apache.hadoop.io.nativeio.NativeIO.POSIX.NoMlockCacheManipulator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injected version of TestCachingStrategy.
 *
 * Tests caching strategy operations survive component restarts.
 * The original tests focus on posix_fadvise cache management during
 * file creation and reading. This restart-injected version tests
 * that files created with specific caching strategies are preserved
 * across restarts.
 *
 * Original tests transformed:
 * - testFadviseAfterWriteThenRead: Write file with drop-behind, restart, read
 * - testNoFadviseAfterWriteThenRead: Write file without drop-behind, restart, read
 *
 * Restart points:
 * - AfterWrite: After file creation (before read)
 *
 * Generated variants:
 * - 2 tests x AfterWrite x 4 RestartTargets x 2 RestartModes = 16 variants
 */
public class TestCachingStrategy_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestCachingStrategy_RestartInjected.class);
  private static final int MAX_TEST_FILE_LEN = 1024 * 1024;

  private Configuration conf;
  private MiniDFSCluster cluster;
  private DistributedFileSystem dfs;

  @BeforeClass
  public static void setupTest() {
    EditLogFileOutputStream.setShouldSkipFsyncForTesting(true);
    // Use NoMlockCacheManipulator to avoid native IO issues
    NativeIO.POSIX.setCacheManipulator(new NoMlockCacheManipulator());
    // Set smaller intervals for testing
    BlockSender.CACHE_DROP_INTERVAL_BYTES = 4096;
    BlockReceiver.CACHE_DROP_LAG_BYTES = 4096;
  }

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitActive();
    dfs = cluster.getFileSystem();
  }

  @After
  public void teardown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  /**
   * Create a HDFS file with specified length and drop-behind setting.
   * Based on TestCachingStrategy#createHdfsFile
   */
  static void createHdfsFile(FileSystem fs, Path p, long length,
      Boolean dropBehind) throws Exception {
    FSDataOutputStream fos = null;
    try {
      // create file with replication factor of 1
      fos = fs.create(p, (short)1);
      if (dropBehind != null) {
        fos.setDropBehind(dropBehind);
      }
      byte buf[] = new byte[8196];
      while (length > 0) {
        int amt = (length > buf.length) ? buf.length : (int)length;
        fos.write(buf, 0, amt);
        length -= amt;
      }
    } catch (IOException e) {
      LOG.error("ioexception", e);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Read a HDFS file with specified drop-behind setting.
   * Based on TestCachingStrategy#readHdfsFile
   */
  static long readHdfsFile(FileSystem fs, Path p, long length,
      Boolean dropBehind) throws Exception {
    FSDataInputStream fis = null;
    long totalRead = 0;
    try {
      fis = fs.open(p);
      if (dropBehind != null) {
        fis.setDropBehind(dropBehind);
      }
      byte buf[] = new byte[8196];
      while (length > 0) {
        int amt = (length > buf.length) ? buf.length : (int)length;
        int ret = fis.read(buf, 0, amt);
        if (ret == -1) {
          return totalRead;
        }
        totalRead += ret;
        length -= ret;
      }
    } catch (IOException e) {
      LOG.error("ioexception", e);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
    return totalRead;
  }

  // ============================================================
  // Test: testFadviseAfterWriteThenRead with restart injection
  // AfterWrite x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file with drop-behind, restart, read file.
   * Based on TestCachingStrategy#testFadviseAfterWriteThenRead
   */
  private void testFadviseAfterWriteThenReadWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    LOG.info("testFadviseAfterWriteThenRead with restart");
    String TEST_PATH = "/test";
    int TEST_PATH_LEN = MAX_TEST_FILE_LEN;

    // === ORIGINAL CODE: Create file with drop-behind ===
    createHdfsFile(dfs, new Path(TEST_PATH), TEST_PATH_LEN, true);

    // Verify file was created and get block info
    ExtendedBlock block = cluster.getNameNode().getRpcServer().getBlockLocations(
        TEST_PATH, 0, Long.MAX_VALUE).get(0).getBlock();
    assertNotNull("Block should exist after create", block);

    // === RESTART INJECTION POINT: After write, before read ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    dfs = cluster.getFileSystem();
    verifyClusterHealth(cluster, dfs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file exists and read it ===
    assertTrue("File should exist after restart", dfs.exists(new Path(TEST_PATH)));
    assertEquals("File length should be preserved", TEST_PATH_LEN,
        dfs.getFileStatus(new Path(TEST_PATH)).getLen());

    // Read file
    long bytesRead = readHdfsFile(dfs, new Path(TEST_PATH), Long.MAX_VALUE, true);
    assertEquals("Should read entire file", TEST_PATH_LEN, bytesRead);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_NN_Graceful() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_NN_Crash() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_SingleDN_Graceful() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_SingleDN_Crash() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_AllDN_Graceful() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_AllDN_Crash() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_NNDN_Graceful() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testFadviseAfterWriteThenRead_AfterWrite_NNDN_Crash() throws Exception {
    testFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test: testNoFadviseAfterWriteThenRead with restart injection
  // AfterWrite x 4 targets x 2 modes = 8 variants
  // ============================================================

  /**
   * Core test: Create file without drop-behind, restart, read file.
   * Based on TestCachingStrategy#testNoFadviseAfterWriteThenRead
   */
  private void testNoFadviseAfterWriteThenReadWithRestart(RestartTarget target, RestartMode mode)
      throws Exception {
    LOG.info("testNoFadviseAfterWriteThenRead with restart");
    String TEST_PATH = "/test";
    int TEST_PATH_LEN = MAX_TEST_FILE_LEN;

    // === ORIGINAL CODE: Create file without drop-behind ===
    createHdfsFile(dfs, new Path(TEST_PATH), TEST_PATH_LEN, false);

    // Verify file was created
    ExtendedBlock block = cluster.getNameNode().getRpcServer().getBlockLocations(
        TEST_PATH, 0, Long.MAX_VALUE).get(0).getBlock();
    assertNotNull("Block should exist after create", block);

    // === RESTART INJECTION POINT: After write, before read ===
    LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
    executeRestart(cluster, target, mode, true);

    // Refresh references after restart
    dfs = cluster.getFileSystem();
    verifyClusterHealth(cluster, dfs);
    LOG.info("=== RESTART COMPLETE ===");

    // === ORIGINAL CODE RESUME: Verify file exists and read it ===
    assertTrue("File should exist after restart", dfs.exists(new Path(TEST_PATH)));
    assertEquals("File length should be preserved", TEST_PATH_LEN,
        dfs.getFileStatus(new Path(TEST_PATH)).getLen());

    // Read file
    long bytesRead = readHdfsFile(dfs, new Path(TEST_PATH), Long.MAX_VALUE, false);
    assertEquals("Should read entire file", TEST_PATH_LEN, bytesRead);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_NN_Graceful() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_NN_Crash() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_SingleDN_Graceful() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_SingleDN_Crash() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_AllDN_Graceful() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_AllDN_Crash() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_NNDN_Graceful() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testNoFadviseAfterWriteThenRead_AfterWrite_NNDN_Crash() throws Exception {
    testNoFadviseAfterWriteThenReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestFileConcurrentReader.
 * Tests concurrent read/write operations survive component restarts.
 *
 * Original tests with hflush:
 * - testUnfinishedBlockRead
 * - testUnfinishedBlockPacketBufferOverrun
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants per test
 * Total: 16 variants (2 tests x 8 variants)
 */
public class TestFileConcurrentReader_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileConcurrentReader_RestartInjected.class);
  static final int blockSize = 8192;

  private void writeFileAndSync(FSDataOutputStream stm, int size) throws Exception {
    byte[] buffer = DFSTestUtil.generateSequentialBytes(0, size);
    stm.write(buffer, 0, size);
    stm.hflush();
  }

  private void checkCanRead(FileSystem fileSys, Path path, int numBytes) throws Exception {
    waitForBlocks(fileSys, path);
    assertBytesAvailable(fileSys, path, numBytes);
  }

  private void assertBytesAvailable(FileSystem fileSystem, Path path, int numBytes) throws Exception {
    byte[] buffer = new byte[numBytes];
    FSDataInputStream inputStream = fileSystem.open(path);
    IOUtils.readFully(inputStream, buffer, 0, numBytes);
    inputStream.close();
    assertTrue("unable to validate bytes", validateSequentialBytes(buffer, 0, numBytes));
  }

  private void waitForBlocks(FileSystem fileSys, Path name) throws Exception {
    boolean done = false;
    while (!done) {
      Thread.sleep(1000);
      done = true;
      BlockLocation[] locations = fileSys.getFileBlockLocations(
          fileSys.getFileStatus(name), 0, blockSize);
      if (locations.length < 1) {
        done = false;
      }
    }
  }

  private boolean validateSequentialBytes(byte[] buf, int startPos, int len) {
    for (int i = 0; i < len; i++) {
      int expected = (i + startPos) % 127;
      if (buf[i] % 127 != expected) {
        return false;
      }
    }
    return true;
  }

  /**
   * Core test logic for testUnfinishedBlockRead with restart injection.
   */
  private void testUnfinishedBlockReadWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitClusterUp();
      FileSystem fileSystem = cluster.getFileSystem();

      // create a new file in the root, write data, do not close
      Path file1 = new Path("/unfinished-block");
      FSDataOutputStream stm = TestFileCreation.createFile(fileSystem, file1, 1);

      // write partial block and sync
      int partialBlockSize = blockSize / 2;
      writeFileAndSync(stm, partialBlockSize);

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fileSystem);
      LOG.info("=== RESTART COMPLETE ===");

      // Make sure a client can read it before it is closed
      checkCanRead(fileSystem, file1, partialBlockSize);

      stm.close();
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Core test logic for testUnfinishedBlockPacketBufferOverrun with restart injection.
   */
  private void testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitClusterUp();
      FileSystem fileSystem = cluster.getFileSystem();

      // create a new file in the root, write data, do not close
      Path file1 = new Path("/unfinished-block-buffer");
      FSDataOutputStream stm = TestFileCreation.createFile(fileSystem, file1, 1);

      // write partial block and sync
      int bytesPerChecksum = conf.getInt("io.bytes.per.checksum", 512);
      int partialBlockSize = bytesPerChecksum - 1;

      writeFileAndSync(stm, partialBlockSize);

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, (DistributedFileSystem) fileSystem);
      LOG.info("=== RESTART COMPLETE ===");

      // Make sure a client can read it before it is closed
      checkCanRead(fileSystem, file1, partialBlockSize);

      stm.close();
    } finally {
      cluster.shutdown();
    }
  }

  // ============================================================
  // Test variants: testUnfinishedBlockRead
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_NN_Graceful() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_NN_Crash() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_SingleDN_Graceful() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_SingleDN_Crash() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_AllDN_Graceful() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_AllDN_Crash() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_NNDN_Graceful() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockRead_AfterHflush_NNDN_Crash() throws Exception {
    testUnfinishedBlockReadWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }

  // ============================================================
  // Test variants: testUnfinishedBlockPacketBufferOverrun
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_NN_Graceful() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_NN_Crash() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_SingleDN_Graceful() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_SingleDN_Crash() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_AllDN_Graceful() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_AllDN_Crash() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_NNDN_Graceful() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testUnfinishedBlockPacketBufferOverrun_AfterHflush_NNDN_Crash() throws Exception {
    testUnfinishedBlockPacketBufferOverrunWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

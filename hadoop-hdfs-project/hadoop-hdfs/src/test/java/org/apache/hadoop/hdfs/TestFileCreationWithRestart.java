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
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Demo Test 1: Simple file write with NameNode restart.
 *
 * This test demonstrates the restart injection framework by testing
 * file creation and writing across a NameNode restart. It validates that:
 * 1. Files under construction survive NameNode restart
 * 2. Data written before restart is persisted
 * 3. Writing can continue after restart
 * 4. Final file is complete and readable
 *
 * Test Flow:
 * - Create file
 * - Write partial data (50% of file)
 * - Flush data to pipeline
 * - [RESTART NAMENODE]
 * - Continue writing (remaining 50%)
 * - Close file
 * - Verify complete file integrity
 */
public class TestFileCreationWithRestart {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestFileCreationWithRestart.class);

  private static final int BLOCK_SIZE = 8192;
  private static final int NUM_BLOCKS = 4;
  private static final int FILE_SIZE = BLOCK_SIZE * NUM_BLOCKS;

  private MiniDFSCluster cluster;
  private FileSystem fs;
  private Configuration conf;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    // Allow DFSClient to survive NameNode restart
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);

    cluster = new MiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .build();
    cluster.waitActive();
    fs = cluster.getFileSystem();

    LOG.info("Test cluster started with {} DataNodes",
        cluster.getDataNodes().size());
  }

  @After
  public void teardown() {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  /**
   * Test basic file creation with NameNode restart during write.
   * This is the simplest restart scenario.
   */
  @Test(timeout = 60000)
  public void testSimpleFileWriteWithNameNodeRestart() throws Exception {
    LOG.info("=== Starting testSimpleFileWriteWithNameNodeRestart ===");

    Path testFile = new Path("/test-file-nn-restart");

    // Phase 1: Create file and write first half
    LOG.info("Phase 1: Creating file and writing first half");
    FSDataOutputStream out = fs.create(testFile, true, 4096, (short) 3, BLOCK_SIZE);

    int halfSize = FILE_SIZE / 2;
    AppendTestUtil.write(out, 0, halfSize);
    out.hflush(); // Ensure data is in pipeline
    LOG.info("Wrote {} bytes and flushed", halfSize);

    long lengthBeforeRestart = fs.getFileStatus(testFile).getLen();
    LOG.info("File length before restart: {}", lengthBeforeRestart);

    // Phase 2: RESTART NAMENODE while file is still open
    LOG.info("Phase 2: Restarting NameNode (GRACEFUL mode)");
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    LOG.info("NameNode restarted successfully");

    // Verify cluster health after restart
    verifyClusterHealth(cluster, fs);

    // Phase 3: Continue writing second half
    LOG.info("Phase 3: Continuing write after restart");
    AppendTestUtil.write(out, halfSize, halfSize);
    out.close();
    LOG.info("Wrote remaining {} bytes and closed file", halfSize);

    // Phase 4: Verify complete file
    LOG.info("Phase 4: Verifying file integrity");
    long finalLength = fs.getFileStatus(testFile).getLen();
    assertEquals("File length mismatch after restart and close",
        FILE_SIZE, finalLength);

    // Verify file content with sequential byte pattern
    verifyFileIntegrity(fs, testFile, FILE_SIZE, new byte[1]);

    LOG.info("=== Test PASSED: File creation with NN restart successful ===");
  }

  /**
   * Test file creation with NameNode CRASH (ungraceful shutdown).
   * This simulates a more severe failure scenario.
   */
  @Test(timeout = 60000)
  public void testFileWriteWithNameNodeCrash() throws Exception {
    LOG.info("=== Starting testFileWriteWithNameNodeCrash ===");

    Path testFile = new Path("/test-file-nn-crash");

    // Phase 1: Create file and write first half
    FSDataOutputStream out = fs.create(testFile, true, 4096, (short) 3, BLOCK_SIZE);
    int halfSize = FILE_SIZE / 2;
    AppendTestUtil.write(out, 0, halfSize);
    out.hsync(); // Use hsync for durability guarantee
    LOG.info("Wrote {} bytes and synced", halfSize);

    // Phase 2: CRASH NAMENODE (simulated power failure)
    LOG.info("Phase 2: Simulating NameNode CRASH");
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    LOG.info("NameNode crashed and restarted");

    // Verify cluster health
    verifyClusterHealth(cluster, fs);

    // Phase 3: Continue writing
    LOG.info("Phase 3: Continuing write after crash");
    AppendTestUtil.write(out, halfSize, halfSize);
    out.close();

    // Phase 4: Verify
    verifyFileIntegrity(fs, testFile, FILE_SIZE, new byte[1]);

    LOG.info("=== Test PASSED: File survived NN crash ===");
  }

  /**
   * Test multiple blocks written with restart between blocks.
   * This tests block boundary handling.
   */
  @Test(timeout = 60000)
  public void testMultiBlockWriteWithRestart() throws Exception {
    LOG.info("=== Starting testMultiBlockWriteWithRestart ===");

    Path testFile = new Path("/test-multiblock-restart");

    FSDataOutputStream out = fs.create(testFile, true, 4096, (short) 3, BLOCK_SIZE);

    // Write 2 complete blocks
    int twoBlocks = BLOCK_SIZE * 2;
    AppendTestUtil.write(out, 0, twoBlocks);
    out.hflush();
    LOG.info("Wrote 2 blocks ({} bytes)", twoBlocks);

    // Restart after complete blocks written
    LOG.info("Restarting NameNode after writing 2 complete blocks");
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);

    // Write remaining 2 blocks
    AppendTestUtil.write(out, twoBlocks, FILE_SIZE - twoBlocks);
    out.close();
    LOG.info("Wrote remaining 2 blocks");

    // Verify
    verifyFileIntegrity(fs, testFile, FILE_SIZE, new byte[1]);

    LOG.info("=== Test PASSED: Multi-block file with restart successful ===");
  }

  /**
   * Test writing a small amount, restarting, then closing immediately.
   * This tests the minimal restart scenario.
   */
  @Test(timeout = 60000)
  public void testMinimalWriteRestart() throws Exception {
    LOG.info("=== Starting testMinimalWriteRestart ===");

    Path testFile = new Path("/test-minimal-restart");
    int dataSize = 1024; // Just 1KB

    FSDataOutputStream out = fs.create(testFile, true, 4096, (short) 2, BLOCK_SIZE);
    AppendTestUtil.write(out, 0, dataSize);
    out.hflush();
    LOG.info("Wrote {} bytes", dataSize);

    // Restart immediately
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);

    // Close immediately after restart
    out.close();
    LOG.info("Closed file after restart");

    // Verify
    verifyFileIntegrity(fs, testFile, dataSize, new byte[1]);

    LOG.info("=== Test PASSED: Minimal write-restart-close successful ===");
  }

  /**
   * Test that data written and synced before NN restart is NOT lost.
   * Creates a file, writes data, syncs, restarts NN, then closes and verifies.
   * This tests that hsync() durability survives NN restart.
   */
  @Test(timeout = 60000)
  public void testLeaseRecoveryAfterRestart() throws Exception {
    LOG.info("=== Starting testLeaseRecoveryAfterRestart ===");

    Path testFile = new Path("/test-lease-recovery");
    int dataSize = BLOCK_SIZE * 2;

    // Create file and write data
    FSDataOutputStream out = fs.create(testFile, true, 4096, (short) 3, BLOCK_SIZE);
    AppendTestUtil.write(out, 0, dataSize);
    out.hsync(); // Ensure data is durable - THIS IS THE KEY TEST
    LOG.info("Wrote {} bytes and synced to DN", dataSize);

    // Restart NameNode (file stream still open but data is on DNs)
    LOG.info("Restarting NameNode with open file");
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    LOG.info("NameNode restarted, file still has active lease");

    // Now close the stream - NN should be able to finalize the file
    // even though it restarted (because data was hsync'd to DNs)
    LOG.info("Closing file stream after NN restart");
    out.close();
    LOG.info("File closed successfully");

    // Verify file is readable and has correct data
    // This proves that hsync() made data durable across NN restart
    LOG.info("Verifying file data integrity after restart");
    verifyNoDataLoss(fs, testFile, dataSize);

    LOG.info("=== Test PASSED: Data survived NN restart via hsync() ===");
  }
}

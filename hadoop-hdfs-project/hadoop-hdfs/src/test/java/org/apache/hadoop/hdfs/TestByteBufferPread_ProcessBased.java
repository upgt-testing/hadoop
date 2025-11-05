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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestByteBufferPread}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests DFS positional read functionality with ByteBuffers (heap and direct).
 *
 * @see TestByteBufferPread Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestByteBufferPread_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final long SEED = 0xDEADBEEFL;
  private static final int BLOCK_SIZE = 4096;
  private static final int FILE_SIZE = 12 * BLOCK_SIZE;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * Creates a test file with random content.
   */
  private byte[] createTestFile(Path testFile) throws IOException {
    byte[] fileContents = new byte[FILE_SIZE];
    Random rand = new Random(SEED);
    rand.nextBytes(fileContents);
    try (FSDataOutputStream out = fs.create(testFile, (short) 3)) {
      out.write(fileContents);
    }
    return fileContents;
  }

  /**
   * Test preads with {@link java.nio.HeapByteBuffer}s.
   */
  @Test
  public void testPreadWithHeapByteBuffer() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path testFile = new Path("/byte-buffer-pread-heap-test.dat");
    byte[] fileContents = createTestFile(testFile);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    Random rand = new Random(SEED);
    testPreadWithByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE));
    testPreadWithFullByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE), rand);
    testPreadWithPositionedByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE));
    testPreadWithLimitedByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE));
    testPositionedPreadWithByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE));
    testPreadFullyWithByteBuffer(testFile, fileContents, ByteBuffer.allocate(FILE_SIZE));

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }

  /**
   * Test preads with {@link java.nio.DirectByteBuffer}s.
   */
  @Test
  public void testPreadWithDirectByteBuffer() throws Exception {
    Configuration testConf = new HdfsConfiguration(conf);
    testConf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf).numDataNodes(3).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path testFile = new Path("/byte-buffer-pread-direct-test.dat");
    byte[] fileContents = createTestFile(testFile);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    Random rand = new Random(SEED);
    testPreadWithByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE));
    testPreadWithFullByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE), rand);
    testPreadWithPositionedByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE));
    testPreadWithLimitedByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE));
    testPositionedPreadWithByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE));
    testPreadFullyWithByteBuffer(testFile, fileContents, ByteBuffer.allocateDirect(FILE_SIZE));

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }

  /**
   * Reads the entire testFile using the pread API and validates that its
   * contents are properly loaded into the supplied {@link ByteBuffer}.
   */
  private void testPreadWithByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer)
      throws IOException {
    int bytesRead;
    int totalBytesRead = 0;
    try (FSDataInputStream in = fs.open(testFile)) {
      while ((bytesRead = in.read(totalBytesRead, buffer)) > 0) {
        totalBytesRead += bytesRead;
        // Check that each call to read changes the position of the ByteBuffer
        // correctly
        assertEquals(totalBytesRead, buffer.position());
      }

      // Make sure the buffer is full
      assertFalse(buffer.hasRemaining());
      // Make sure the contents of the read buffer equal the contents of the
      // file
      buffer.position(0);
      byte[] bufferContents = new byte[FILE_SIZE];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents, fileContents);
    }
  }

  /**
   * Attempts to read the testFile into a {@link ByteBuffer} that is already
   * full, and validates that doing so does not change the contents of the
   * supplied {@link ByteBuffer}.
   */
  private void testPreadWithFullByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer, Random rand)
          throws IOException {
    // Load some dummy data into the buffer
    byte[] existingBufferBytes = new byte[FILE_SIZE];
    rand.nextBytes(existingBufferBytes);
    buffer.put(existingBufferBytes);
    // Make sure the buffer is full
    assertFalse(buffer.hasRemaining());

    try (FSDataInputStream in = fs.open(testFile)) {
      // Attempt to read into the buffer, 0 bytes should be read since the
      // buffer is full
      assertEquals(0, in.read(buffer));

      // Double check the buffer is still full and its contents have not
      // changed
      assertFalse(buffer.hasRemaining());
      buffer.position(0);
      byte[] bufferContents = new byte[FILE_SIZE];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents, existingBufferBytes);
    }
  }

  /**
   * Reads half of the testFile into the {@link ByteBuffer} by setting a
   * {@link ByteBuffer#limit()} on the buffer. Validates that only half of the
   * testFile is loaded into the buffer.
   */
  private void testPreadWithLimitedByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer)
      throws IOException {
    int bytesRead;
    int totalBytesRead = 0;
    // Set the buffer limit to half the size of the file
    buffer.limit(FILE_SIZE / 2);

    try (FSDataInputStream in = fs.open(testFile)) {
      while ((bytesRead = in.read(totalBytesRead, buffer)) > 0) {
        totalBytesRead += bytesRead;
        // Check that each call to read changes the position of the ByteBuffer
        // correctly
        assertEquals(totalBytesRead, buffer.position());
      }

      // Since we set the buffer limit to half the size of the file, we should
      // have only read half of the file into the buffer
      assertEquals(totalBytesRead, FILE_SIZE / 2);
      // Check that the buffer is full and the contents equal the first half of
      // the file
      assertFalse(buffer.hasRemaining());
      buffer.position(0);
      byte[] bufferContents = new byte[FILE_SIZE / 2];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents,
              Arrays.copyOfRange(fileContents, 0, FILE_SIZE / 2));
    }
  }

  /**
   * Reads half of the testFile into the {@link ByteBuffer} by setting the
   * {@link ByteBuffer#position()} the half the size of the file. Validates that
   * only half of the testFile is loaded into the buffer.
   */
  private void testPreadWithPositionedByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer)
      throws IOException {
    int bytesRead;
    int totalBytesRead = 0;
    // Set the buffer position to half the size of the file
    buffer.position(FILE_SIZE / 2);

    try (FSDataInputStream in = fs.open(testFile)) {
      while ((bytesRead = in.read(totalBytesRead, buffer)) > 0) {
        totalBytesRead += bytesRead;
        // Check that each call to read changes the position of the ByteBuffer
        // correctly
        assertEquals(totalBytesRead + FILE_SIZE / 2, buffer.position());
      }

      // Since we set the buffer position to half the size of the file, we
      // should have only read half of the file into the buffer
      assertEquals(totalBytesRead, FILE_SIZE / 2);
      // Check that the buffer is full and the contents equal the first half of
      // the file
      assertFalse(buffer.hasRemaining());
      buffer.position(FILE_SIZE / 2);
      byte[] bufferContents = new byte[FILE_SIZE / 2];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents,
              Arrays.copyOfRange(fileContents, 0, FILE_SIZE / 2));
    }
  }

  /**
   * Reads half of the testFile into the {@link ByteBuffer} by specifying a
   * position for the pread API that is half of the file size. Validates that
   * only half of the testFile is loaded into the buffer.
   */
  private void testPositionedPreadWithByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer)
      throws IOException {
    int bytesRead;
    int totalBytesRead = 0;

    try (FSDataInputStream in = fs.open(testFile)) {
      // Start reading from halfway through the file
      while ((bytesRead = in.read(totalBytesRead + FILE_SIZE / 2,
              buffer)) > 0) {
        totalBytesRead += bytesRead;
        // Check that each call to read changes the position of the ByteBuffer
        // correctly
        assertEquals(totalBytesRead, buffer.position());
      }

      // Since we starting reading halfway through the file, the buffer should
      // only be half full
      assertEquals(totalBytesRead, FILE_SIZE / 2);
      assertEquals(buffer.position(), FILE_SIZE / 2);
      assertTrue(buffer.hasRemaining());
      // Check that the buffer contents equal the second half of the file
      buffer.position(0);
      byte[] bufferContents = new byte[FILE_SIZE / 2];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents,
              Arrays.copyOfRange(fileContents, FILE_SIZE / 2, FILE_SIZE));
    }
  }

  /**
   * Reads the entire testFile using the preadFully API and validates that its
   * contents are properly loaded into the supplied {@link ByteBuffer}.
   */
  private void testPreadFullyWithByteBuffer(Path testFile, byte[] fileContents, ByteBuffer buffer)
          throws IOException {
    int totalBytesRead = 0;
    try (FSDataInputStream in = fs.open(testFile)) {
      in.readFully(totalBytesRead, buffer);
      // Make sure the buffer is full
      assertFalse(buffer.hasRemaining());
      // Make sure the contents of the read buffer equal the contents of the
      // file
      buffer.position(0);
      byte[] bufferContents = new byte[FILE_SIZE];
      buffer.get(bufferContents);
      assertArrayEquals(bufferContents, fileContents);
    }
  }
}

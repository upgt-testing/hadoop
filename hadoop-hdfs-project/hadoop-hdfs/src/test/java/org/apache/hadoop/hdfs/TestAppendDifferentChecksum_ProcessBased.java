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
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Time;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestAppendDifferentChecksum}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests cases for trying to append to a file with a different
 * checksum than the file was originally written with.
 *
 * @see TestAppendDifferentChecksum Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestAppendDifferentChecksum_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final int SEGMENT_LENGTH = 1500;

  // run the randomized test for 5 seconds
  private static final long RANDOM_TEST_RUNTIME = 5000;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_FIRST_WRITE",
      "AFTER_FIRST_CLOSE",
      UpgradeCheckpoints.AFTER_APPEND_REOPEN,
      "AFTER_SECOND_WRITE",
      "BEFORE_VERIFICATION"
    );
  }

  /**
   * This test does not run, since switching chunksize with append
   * is not implemented. Please see HDFS-2130 for a discussion of the
   * difficulties in doing so.
   */
  @Test
  @Ignore("this is not implemented! See HDFS-2130")
  public void testSwitchChunkSize() throws Exception {
    Configuration testConf = new HdfsConfiguration();
    testConf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 4096);
    testConf.set("fs.hdfs.impl.disable.cache", "true");

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
      .numDataNodes(1)
      .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem fsWithSmallChunk = createFsWithChecksum("CRC32", 512);
    FileSystem fsWithBigChunk = createFsWithChecksum("CRC32", 1024);
    Path p = new Path("/testSwitchChunkSize");
    appendWithTwoFs(p, fsWithSmallChunk, fsWithBigChunk);

    checkpoint("BEFORE_VERIFICATION");

    AppendTestUtil.check(fsWithSmallChunk, p, SEGMENT_LENGTH * 2);
    AppendTestUtil.check(fsWithBigChunk, p, SEGMENT_LENGTH * 2);
  }

  /**
   * Simple unit test which writes some data with one algorithm,
   * then appends with another.
   */
  @Test
  public void testSwitchAlgorithms() throws Exception {
    Configuration testConf = new HdfsConfiguration();
    testConf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 4096);
    testConf.set("fs.hdfs.impl.disable.cache", "true");

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
      .numDataNodes(1)
      .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem fsWithCrc32 = createFsWithChecksum("CRC32", 512);
    FileSystem fsWithCrc32C = createFsWithChecksum("CRC32C", 512);

    Path p = new Path("/testSwitchAlgorithms");
    appendWithTwoFs(p, fsWithCrc32, fsWithCrc32C);

    checkpoint("BEFORE_VERIFICATION");

    // Regardless of which FS is used to read, it should pick up
    // the on-disk checksum!
    AppendTestUtil.check(fsWithCrc32C, p, SEGMENT_LENGTH * 2);
    AppendTestUtil.check(fsWithCrc32, p, SEGMENT_LENGTH * 2);
  }

  /**
   * Test which randomly alternates between appending with
   * CRC32 and with CRC32C, crossing several block boundaries.
   * Then, checks that all of the data can be read back correct.
   */
  @Test(timeout=RANDOM_TEST_RUNTIME*2)
  public void testAlgoSwitchRandomized() throws Exception {
    Configuration testConf = new HdfsConfiguration();
    testConf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 4096);
    testConf.set("fs.hdfs.impl.disable.cache", "true");

    cluster = new ProcessBasedMiniDFSCluster.Builder(testConf)
      .numDataNodes(1)
      .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem fsWithCrc32 = createFsWithChecksum("CRC32", 512);
    FileSystem fsWithCrc32C = createFsWithChecksum("CRC32C", 512);

    Path p = new Path("/testAlgoSwitchRandomized");
    long seed = Time.now();
    System.out.println("seed: " + seed);
    Random r = new Random(seed);

    // Create empty to start
    IOUtils.closeStream(fsWithCrc32.create(p));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    long st = Time.now();
    int len = 0;
    int iterationCount = 0;
    while (Time.now() - st < RANDOM_TEST_RUNTIME) {
      int thisLen = r.nextInt(500);
      FileSystem selectedFs = (r.nextBoolean() ? fsWithCrc32 : fsWithCrc32C);
      FSDataOutputStream stm = selectedFs.append(p);
      try {
        AppendTestUtil.write(stm, len, thisLen);
      } finally {
        stm.close();
      }
      len += thisLen;
      iterationCount++;

      // Insert checkpoints periodically (every 10 iterations) to avoid too many checkpoints
      if (iterationCount % 10 == 0) {
        checkpoint("AFTER_APPEND_ITERATION_" + iterationCount);
      }
    }

    checkpoint("BEFORE_VERIFICATION");

    AppendTestUtil.check(fsWithCrc32, p, len);
    AppendTestUtil.check(fsWithCrc32C, p, len);
  }

  private FileSystem createFsWithChecksum(String type, int bytes)
      throws IOException {
    Configuration conf = new Configuration(fs.getConf());
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, type);
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, bytes);
    return FileSystem.get(conf);
  }

  private void appendWithTwoFs(Path p, FileSystem fs1, FileSystem fs2)
      throws Exception {
    FSDataOutputStream stm = fs1.create(p);
    try {
      AppendTestUtil.write(stm, 0, SEGMENT_LENGTH);
    } finally {
      stm.close();
    }

    checkpoint("AFTER_FIRST_CLOSE");

    stm = fs2.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    try {
      AppendTestUtil.write(stm, SEGMENT_LENGTH, SEGMENT_LENGTH);
    } finally {
      stm.close();
    }

    checkpoint("AFTER_SECOND_WRITE");
  }
}

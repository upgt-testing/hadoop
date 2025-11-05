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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StreamCapabilities.StreamCapability;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.erasurecode.CodecUtil;
import org.apache.hadoop.io.erasurecode.ErasureCodeNative;
import org.apache.hadoop.io.erasurecode.rawcoder.NativeRSRawErasureCoderFactory;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSStripedOutputStream}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSStripedOutputStream Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSStripedOutputStream_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG = LoggerFactory.getLogger(
      TestDFSStripedOutputStream_ProcessBased.class);

  static {
    GenericTestUtils.setLogLevel(DFSOutputStream.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(DataStreamer.LOG, Level.ALL);
  }

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

  private ErasureCodingPolicy ecPolicy;
  private int dataBlocks;
  private int parityBlocks;
  private DistributedFileSystem fs;
  private int cellSize;
  private final int stripesPerBlock = 4;
  private int blockSize;

  private void setupCluster() throws Exception {
    /*
     * Initialize erasure coding policy.
     */
    ecPolicy = getEcPolicy();
    dataBlocks = (short) ecPolicy.getNumDataUnits();
    parityBlocks = (short) ecPolicy.getNumParityUnits();
    cellSize = ecPolicy.getCellSize();
    blockSize = stripesPerBlock * cellSize;
    System.out.println("EC policy = " + ecPolicy);

    int numDNs = dataBlocks + parityBlocks + 2;
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_MAX_STREAMS_KEY, 0);
    if (ErasureCodeNative.isNativeCodeLoaded()) {
      conf.set(
          CodecUtil.IO_ERASURECODE_CODEC_RS_RAWCODERS_KEY,
          NativeRSRawErasureCoderFactory.CODER_NAME);
    }
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(numDNs).build();
    cluster.waitClusterUp();
    fs = (DistributedFileSystem) cluster.getFileSystem();
    DFSTestUtil.enableAllECPolicies(fs);
    fs.getClient().setErasureCodingPolicy("/", ecPolicy.getName());
  }

  public ErasureCodingPolicy getEcPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  @Test
  public void testFileEmpty() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/EmptyFile", 0);
  }

  @Test
  public void testFileSmallerThanOneCell1() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/SmallerThanOneCell", 1);
  }

  @Test
  public void testFileSmallerThanOneCell2() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/SmallerThanOneCell", cellSize - 1);
  }

  @Test
  public void testFileEqualsWithOneCell() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/EqualsWithOneCell", cellSize);
  }

  @Test
  public void testFileSmallerThanOneStripe1() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/SmallerThanOneStripe", cellSize * dataBlocks - 1);
  }

  @Test
  public void testFileSmallerThanOneStripe2() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/SmallerThanOneStripe", cellSize + 123);
  }

  @Test
  public void testFileEqualsWithOneStripe() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/EqualsWithOneStripe", cellSize * dataBlocks);
  }

  @Test
  public void testFileMoreThanOneStripe1() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/MoreThanOneStripe1", cellSize * dataBlocks + 123);
  }

  @Test
  public void testFileMoreThanOneStripe2() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/MoreThanOneStripe2", cellSize * dataBlocks
            + cellSize * dataBlocks + 123);
  }

  @Test
  public void testFileLessThanFullBlockGroup() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/LessThanFullBlockGroup",
        cellSize * dataBlocks * (stripesPerBlock - 1) + cellSize);
  }

  @Test
  public void testFileFullBlockGroup() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/FullBlockGroup", blockSize * dataBlocks);
  }

  @Test
  public void testFileMoreThanABlockGroup1() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/MoreThanABlockGroup1", blockSize * dataBlocks + 123);
  }

  @Test
  public void testFileMoreThanABlockGroup2() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/MoreThanABlockGroup2",
        blockSize * dataBlocks + cellSize+ 123);
  }

  @Test
  public void testFileMoreThanABlockGroup3() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
    testOneFile("/MoreThanABlockGroup3",
        blockSize * dataBlocks * 3 + cellSize * dataBlocks
        + cellSize + 123);
  }

  /**
   * {@link DFSStripedOutputStream} doesn't support hflush() or hsync() yet.
   * This test is to make sure that DFSStripedOutputStream doesn't throw any
   * {@link UnsupportedOperationException} on hflush() or hsync() so as to
   * comply with output stream spec.
   *
   * @throws Exception
   */
  @Test
  public void testStreamFlush() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final byte[] bytes = StripedFileTestUtil.generateBytes(blockSize *
        dataBlocks * 3 + cellSize * dataBlocks + cellSize + 123);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    try (FSDataOutputStream os = fs.create(new Path("/ec-file-1"))) {
      assertFalse(
          "DFSStripedOutputStream should not have hflush() capability yet!",
          os.hasCapability(StreamCapability.HFLUSH.getValue()));
      assertFalse(
          "DFSStripedOutputStream should not have hsync() capability yet!",
          os.hasCapability(StreamCapability.HSYNC.getValue()));
      try (InputStream is = new ByteArrayInputStream(bytes)) {
        IOUtils.copyBytes(is, os, bytes.length);
        os.hflush();
        IOUtils.copyBytes(is, os, bytes.length);
        os.hsync();
        IOUtils.copyBytes(is, os, bytes.length);
      }
      assertTrue("stream is not a DFSStripedOutputStream",
          os.getWrappedStream() instanceof DFSStripedOutputStream);
      final DFSStripedOutputStream dfssos =
          (DFSStripedOutputStream) os.getWrappedStream();
      dfssos.hsync(EnumSet.of(SyncFlag.UPDATE_LENGTH));
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }

  private void testOneFile(String src, int writeBytes) throws Exception {
    src += "_" + writeBytes;
    Path testPath = new Path(src);

    byte[] bytes = StripedFileTestUtil.generateBytes(writeBytes);
    DFSTestUtil.writeFile(fs, testPath, new String(bytes));

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    StripedFileTestUtil.waitBlockGroupsReported(fs, src);

    StripedFileTestUtil.checkData(fs, testPath, writeBytes,
        new ArrayList<DatanodeInfo>(), null, blockSize * dataBlocks);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }

  @Test
  public void testFileBlockSizeSmallerThanCellSize() throws Exception {
    setupCluster();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final Path path = new Path("testFileBlockSizeSmallerThanCellSize");
    final byte[] bytes = StripedFileTestUtil.generateBytes(cellSize * 2);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    try {
      DFSTestUtil.writeFile(fs, path, bytes, cellSize / 2);
      fail("Creating a file with block size smaller than "
          + "ec policy's cell size should fail");
    } catch (IOException expected) {
      LOG.info("Caught expected exception", expected);
      GenericTestUtils
          .assertExceptionContains("less than the cell size", expected);
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

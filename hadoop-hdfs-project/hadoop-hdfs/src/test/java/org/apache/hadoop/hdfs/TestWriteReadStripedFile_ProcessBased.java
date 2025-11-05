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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.SystemErasureCodingPolicies;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.web.WebHdfsConstants;
import org.apache.hadoop.hdfs.web.WebHdfsTestUtil;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestWriteReadStripedFile}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestWriteReadStripedFile Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestWriteReadStripedFile_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestWriteReadStripedFile_ProcessBased.class);
  private final ErasureCodingPolicy ecPolicy =
      SystemErasureCodingPolicies.getByID(
          SystemErasureCodingPolicies.RS_3_2_POLICY_ID);
  private final int cellSize = ecPolicy.getCellSize();
  private final short dataBlocks = (short) ecPolicy.getNumDataUnits();
  private final short parityBlocks = (short) ecPolicy.getNumParityUnits();
  private final int numDNs = dataBlocks + parityBlocks;
  private final int stripesPerBlock = 2;
  private final int blockSize = stripesPerBlock * cellSize;
  private final int blockGroupSize = blockSize * dataBlocks;

  static {
    GenericTestUtils.setLogLevel(DFSOutputStream.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(DataStreamer.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(DFSClient.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(BlockPlacementPolicy.LOG, Level.TRACE);
  }

  @Rule
  public Timeout globalTimeout = new Timeout(300000);

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        UpgradeCheckpoints.NO_UPGRADE,
        UpgradeCheckpoints.AFTER_CLUSTER_START,
        UpgradeCheckpoints.AFTER_FILE_CREATE,
        "AFTER_EC_POLICY_SET",
        "AFTER_WRITE",
        "BEFORE_READ",
        "AFTER_VERIFICATION"
    );
  }

  public void setup() throws Exception {
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numDNs)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs.enableErasureCodingPolicy(ecPolicy.getName());
    fs.mkdirs(new Path("/ec"));
    cluster.getFileSystem().getClient().setErasureCodingPolicy("/ec",
        ecPolicy.getName());

    checkpoint("AFTER_EC_POLICY_SET");
  }

  @Test
  public void testFileEmpty() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/EmptyFile", 0);
    testOneFileUsingDFSStripedInputStream("/ec/EmptyFile2", 0, true);
  }

  @Test
  public void testFileSmallerThanOneCell1() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneCell", 1);
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneCell2", 1, true);
  }

  @Test
  public void testFileSmallerThanOneCell2() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneCell",
        cellSize - 1);
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneCell2",
        cellSize - 1, true);
  }

  @Test
  public void testFileEqualsWithOneCell() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/EqualsWithOneCell", cellSize);
    testOneFileUsingDFSStripedInputStream("/ec/EqualsWithOneCell2",
        cellSize, true);
  }

  @Test
  public void testFileSmallerThanOneStripe1() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneStripe",
        cellSize * dataBlocks - 1);
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneStripe2",
        cellSize * dataBlocks - 1, true);
  }

  @Test
  public void testFileSmallerThanOneStripe2() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneStripe",
        cellSize + 123);
    testOneFileUsingDFSStripedInputStream("/ec/SmallerThanOneStripe2",
        cellSize + 123, true);
  }

  @Test
  public void testFileEqualsWithOneStripe() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/EqualsWithOneStripe",
        cellSize * dataBlocks);
    testOneFileUsingDFSStripedInputStream("/ec/EqualsWithOneStripe2",
        cellSize * dataBlocks, true);
  }

  @Test
  public void testFileMoreThanOneStripe1() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanOneStripe1",
        cellSize * dataBlocks + 123);
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanOneStripe12",
        cellSize * dataBlocks + 123, true);
  }

  @Test
  public void testFileMoreThanOneStripe2() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanOneStripe2",
        cellSize * dataBlocks + cellSize * dataBlocks + 123);
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanOneStripe22",
        cellSize * dataBlocks + cellSize * dataBlocks + 123, true);
  }

  @Test
  public void testLessThanFullBlockGroup() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/LessThanFullBlockGroup",
        cellSize * dataBlocks * (stripesPerBlock - 1) + cellSize);
    testOneFileUsingDFSStripedInputStream("/ec/LessThanFullBlockGroup2",
        cellSize * dataBlocks * (stripesPerBlock - 1) + cellSize, true);
  }

  @Test
  public void testFileFullBlockGroup() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/FullBlockGroup",
        blockSize * dataBlocks);
    testOneFileUsingDFSStripedInputStream("/ec/FullBlockGroup2",
        blockSize * dataBlocks, true);
  }

  @Test
  public void testFileMoreThanABlockGroup1() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup1",
        blockSize * dataBlocks + 123);
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup12",
        blockSize * dataBlocks + 123, true);
  }

  @Test
  public void testFileMoreThanABlockGroup2() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup2",
        blockSize * dataBlocks + cellSize + 123);
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup22",
        blockSize * dataBlocks + cellSize + 123, true);
  }


  @Test
  public void testFileMoreThanABlockGroup3() throws Exception {
    setup();
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup3",
        blockSize * dataBlocks * 3 + cellSize * dataBlocks
            + cellSize + 123);
    testOneFileUsingDFSStripedInputStream("/ec/MoreThanABlockGroup32",
        blockSize * dataBlocks * 3 + cellSize * dataBlocks
            + cellSize + 123, true);
  }

  private void testOneFileUsingDFSStripedInputStream(String src, int fileLength)
      throws Exception {
    testOneFileUsingDFSStripedInputStream(src, fileLength, false);
  }

  private void testOneFileUsingDFSStripedInputStream(String src, int fileLength,
      boolean withDataNodeFailure) throws Exception {
    final byte[] expected = StripedFileTestUtil.generateBytes(fileLength);
    Path srcPath = new Path(src);
    DFSTestUtil.writeFile(fs, srcPath, new String(expected));

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    StripedFileTestUtil.waitBlockGroupsReported(fs, src);

    checkpoint("AFTER_WRITE");

    StripedFileTestUtil.verifyLength(fs, srcPath, fileLength);

    if (withDataNodeFailure) {
      int dnIndex = 1; // TODO: StripedFileTestUtil.random.nextInt(dataBlocks);
      LOG.info("stop DataNode " + dnIndex);
      stopDataNode(srcPath, dnIndex);
    }

    checkpoint("BEFORE_READ");

    byte[] smallBuf = new byte[1024];
    byte[] largeBuf = new byte[fileLength + 100];
    StripedFileTestUtil.verifyPread(fs, srcPath, fileLength, expected,
        largeBuf);

    StripedFileTestUtil.verifyStatefulRead(fs, srcPath, fileLength, expected,
        largeBuf);
    StripedFileTestUtil.verifySeek(fs, srcPath, fileLength, ecPolicy,
        blockGroupSize);
    StripedFileTestUtil.verifyStatefulRead(fs, srcPath, fileLength, expected,
        ByteBuffer.allocate(fileLength + 100));
    StripedFileTestUtil.verifyStatefulRead(fs, srcPath, fileLength, expected,
        smallBuf);
    StripedFileTestUtil.verifyStatefulRead(fs, srcPath, fileLength, expected,
        ByteBuffer.allocate(1024));

    checkpoint("AFTER_VERIFICATION");
  }

  private void stopDataNode(Path path, int failedDNIdx) throws IOException {
    // Use ProcessBasedMiniDFSCluster's shutdownDataNode method
    // The index from block locations corresponds to the DN index
    BlockLocation[] locs = fs.getFileBlockLocations(path, 0, cellSize);
    if (locs != null && locs.length > 0 && failedDNIdx < locs[0].getNames().length) {
      // Shutdown the DataNode at the specified index
      cluster.shutdownDataNode(failedDNIdx);
      LOG.info("Shutdown DataNode at index " + failedDNIdx);
    }
  }

  @Test
  public void testWriteReadUsingWebHdfs() throws Exception {
    setup();
    int fileLength = blockSize * dataBlocks + cellSize + 123;

    final byte[] expected = StripedFileTestUtil.generateBytes(fileLength);
    FileSystem webhdfs = WebHdfsTestUtil.getWebHdfsFileSystem(conf,
        WebHdfsConstants.WEBHDFS_SCHEME);
    Path srcPath = new Path("/testWriteReadUsingWebHdfs");
    DFSTestUtil.writeFile(webhdfs, srcPath, new String(expected));

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);
    checkpoint("AFTER_WRITE");

    StripedFileTestUtil.verifyLength(webhdfs, srcPath, fileLength);

    checkpoint("BEFORE_READ");

    byte[] smallBuf = new byte[1024];
    byte[] largeBuf = new byte[fileLength + 100];
    StripedFileTestUtil
        .verifyPread(webhdfs, srcPath, fileLength, expected, largeBuf, ecPolicy);

    StripedFileTestUtil
        .verifyStatefulRead(webhdfs, srcPath, fileLength, expected, largeBuf);
    StripedFileTestUtil.verifySeek(webhdfs, srcPath, fileLength, ecPolicy,
        blockGroupSize);
    StripedFileTestUtil
        .verifyStatefulRead(webhdfs, srcPath, fileLength, expected, smallBuf);
    // webhdfs doesn't support bytebuffer read

    checkpoint("AFTER_VERIFICATION");

    webhdfs.close();
  }

  @Test
  public void testConcat() throws Exception {
    setup();
    final byte[] data =
        StripedFileTestUtil.generateBytes(blockSize * dataBlocks * 10 + 234);
    int totalLength = 0;

    Random r = new Random();
    Path target = new Path("/ec/testConcat_target");
    DFSTestUtil.writeFile(fs, target, Arrays.copyOfRange(data, 0, 123));
    totalLength += 123;

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    int numFiles = 5;
    Path[] srcs = new Path[numFiles];
    for (int i = 0; i < numFiles; i++) {
      srcs[i] = new Path("/ec/testConcat_src_file_" + i);
      int srcLength = r.nextInt(blockSize * dataBlocks * 2) + 1;
      DFSTestUtil.writeFile(fs, srcs[i],
          Arrays.copyOfRange(data, totalLength, totalLength + srcLength));
      totalLength += srcLength;
    }

    checkpoint("AFTER_WRITE");

    fs.concat(target, srcs);

    checkpoint("BEFORE_READ");

    StripedFileTestUtil.verifyStatefulRead(fs, target, totalLength,
        Arrays.copyOfRange(data, 0, totalLength), new byte[1024]);

    checkpoint("AFTER_VERIFICATION");
  }

  @Test
  public void testConcatWithDifferentECPolicy() throws Exception {
    setup();
    final byte[] data =
        StripedFileTestUtil.generateBytes(blockSize * dataBlocks);
    Path nonECFile = new Path("/non_ec_file");
    DFSTestUtil.writeFile(fs, nonECFile, data);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    Path target = new Path("/ec/non_ec_file");
    fs.rename(nonECFile, target);

    int numFiles = 2;
    Path[] srcs = new Path[numFiles];
    for (int i = 0; i < numFiles; i++) {
      srcs[i] = new Path("/ec/testConcat_src_file_"+i);
      DFSTestUtil.writeFile(fs, srcs[i], data);
    }

    checkpoint("AFTER_WRITE");

    try {
      fs.concat(target, srcs);
      Assert.fail("non-ec file shouldn't concat with ec file");
    } catch (RemoteException e){
      Assert.assertTrue(e.getMessage()
          .contains("have different erasure coding policy"));
    }

    checkpoint("AFTER_VERIFICATION");
  }
}

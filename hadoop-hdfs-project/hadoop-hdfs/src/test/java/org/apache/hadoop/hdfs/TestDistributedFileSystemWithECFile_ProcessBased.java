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

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDistributedFileSystemWithECFile}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests correctness of FileSystem.getFileBlockLocations and FileSystem.listFiles
 * for erasure coded files.
 *
 * Note: testReplayEditLogsForReplicatedFile from original test is not included
 * because it requires HA topology (MiniDFSNNTopology.simpleHATopology()) which
 * ProcessBasedMiniDFSCluster does not support.
 *
 * @see TestDistributedFileSystemWithECFile Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDistributedFileSystemWithECFile_ProcessBased extends ProcessBasedUpgradeTestBase {
  private ErasureCodingPolicy ecPolicy;
  private int cellSize;
  private short dataBlocks;
  private short parityBlocks;
  private int numDNs;
  private int stripesPerBlock;
  private int blockSize;
  private int blockGroupSize;
  private FileContext fileContext;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_EC_POLICY_SETUP",
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_FILE_VERIFY",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  public ErasureCodingPolicy getEcPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  private void setupCluster() throws Exception {
    ecPolicy = getEcPolicy();
    cellSize = ecPolicy.getCellSize();
    dataBlocks = (short) ecPolicy.getNumDataUnits();
    parityBlocks = (short) ecPolicy.getNumParityUnits();
    numDNs = dataBlocks + parityBlocks;
    stripesPerBlock = 4;
    blockSize = stripesPerBlock * cellSize;
    blockGroupSize = blockSize * dataBlocks;

    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY, false);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numDNs)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    fileContext = FileContext.getFileContext(cluster.getURI(), conf);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Setup EC policy
    fs.enableErasureCodingPolicy(ecPolicy.getName());
    fs.mkdirs(new Path("/ec"));
    ((DistributedFileSystem) fs).getClient().setErasureCodingPolicy("/ec", ecPolicy.getName());

    checkpoint("AFTER_EC_POLICY_SETUP");
  }

  private void createFile(String path, int size) throws Exception {
    byte[] expected = StripedFileTestUtil.generateBytes(size);
    Path src = new Path(path);
    DFSTestUtil.writeFile(fs, src, new String(expected));

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    StripedFileTestUtil.waitBlockGroupsReported(fs, src.toString());
    StripedFileTestUtil.verifyLength(fs, src, size);

    checkpoint("AFTER_FILE_VERIFY");
  }

  @Test(timeout=180000)
  public void testListECFilesSmallerThanOneCell() throws Exception {
    setupCluster();

    createFile("/ec/smallcell", 1);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    final List<LocatedFileStatus> retVal = new ArrayList<>();
    final RemoteIterator<LocatedFileStatus> iter =
        fs.listFiles(new Path("/ec"), true);
    while (iter.hasNext()) {
      retVal.add(iter.next());
    }
    assertTrue(retVal.size() == 1);
    LocatedFileStatus fileStatus = retVal.get(0);
    assertSmallerThanOneCell(fileStatus.getBlockLocations());

    BlockLocation[] locations = fs.getFileBlockLocations(
        fileStatus, 0, fileStatus.getLen());
    assertSmallerThanOneCell(locations);

    //Test FileContext
    fileStatus = fileContext.listLocatedStatus(new Path("/ec")).next();
    assertSmallerThanOneCell(fileStatus.getBlockLocations());
    locations = fileContext.getFileBlockLocations(new Path("/ec/smallcell"),
        0, fileStatus.getLen());
    assertSmallerThanOneCell(locations);
  }

  private void assertSmallerThanOneCell(BlockLocation[] locations)
      throws Exception {
    assertTrue(locations.length == 1);
    BlockLocation blockLocation = locations[0];
    assertTrue(blockLocation.getOffset() == 0);
    assertTrue(blockLocation.getLength() == 1);
    assertTrue(blockLocation.getHosts().length == 1 + parityBlocks);
  }

  @Test(timeout=180000)
  public void testListECFilesSmallerThanOneStripe() throws Exception {
    setupCluster();

    int dataBlocksNum = dataBlocks;
    createFile("/ec/smallstripe", cellSize * dataBlocksNum);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    RemoteIterator<LocatedFileStatus> iter =
        fs.listFiles(new Path("/ec"), true);
    LocatedFileStatus fileStatus = iter.next();
    assertSmallerThanOneStripe(fileStatus.getBlockLocations(), dataBlocksNum);

    BlockLocation[] locations = fs.getFileBlockLocations(
        fileStatus, 0, fileStatus.getLen());
    assertSmallerThanOneStripe(locations, dataBlocksNum);

    //Test FileContext
    fileStatus = fileContext.listLocatedStatus(new Path("/ec")).next();
    assertSmallerThanOneStripe(fileStatus.getBlockLocations(), dataBlocksNum);
    locations = fileContext.getFileBlockLocations(new Path("/ec/smallstripe"),
        0, fileStatus.getLen());
    assertSmallerThanOneStripe(locations, dataBlocksNum);
  }

  private void assertSmallerThanOneStripe(BlockLocation[] locations,
      int dataBlocksNum) throws Exception {
    int expectedHostNum = dataBlocksNum + parityBlocks;
    assertTrue(locations.length == 1);
    BlockLocation blockLocation = locations[0];
    assertTrue(blockLocation.getHosts().length == expectedHostNum);
    assertTrue(blockLocation.getOffset() == 0);
    assertTrue(blockLocation.getLength() == dataBlocksNum * cellSize);
  }

  @Test(timeout=180000)
  public void testListECFilesMoreThanOneBlockGroup() throws Exception {
    setupCluster();

    createFile("/ec/group", blockGroupSize + 123);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    RemoteIterator<LocatedFileStatus> iter =
        fs.listFiles(new Path("/ec"), true);
    LocatedFileStatus fileStatus = iter.next();
    assertMoreThanOneBlockGroup(fileStatus.getBlockLocations(), 123);

    BlockLocation[] locations = fs.getFileBlockLocations(
        fileStatus, 0, fileStatus.getLen());
    assertMoreThanOneBlockGroup(locations, 123);

    //Test FileContext
    iter = fileContext.listLocatedStatus(new Path("/ec"));
    fileStatus = iter.next();
    assertMoreThanOneBlockGroup(fileStatus.getBlockLocations(), 123);
    locations = fileContext.getFileBlockLocations(new Path("/ec/group"),
        0, fileStatus.getLen());
    assertMoreThanOneBlockGroup(locations, 123);
  }

  private void assertMoreThanOneBlockGroup(BlockLocation[] locations,
      int lastBlockSize) throws Exception {
    assertTrue(locations.length == 2);
    BlockLocation fistBlockGroup = locations[0];
    assertTrue(fistBlockGroup.getHosts().length == numDNs);
    assertTrue(fistBlockGroup.getOffset() == 0);
    assertTrue(fistBlockGroup.getLength() == blockGroupSize);
    BlockLocation lastBlock = locations[1];
    assertTrue(lastBlock.getHosts().length == 1 + parityBlocks);
    assertTrue(lastBlock.getOffset() == blockGroupSize);
    assertTrue(lastBlock.getLength() == lastBlockSize);
  }

  // TRANSFORMATION NOTE: testReplayEditLogsForReplicatedFile omitted
  // Original test (lines 210-259) requires HA topology via
  // MiniDFSNNTopology.simpleHATopology() which creates multiple NameNodes
  // with shared state and failover capability. ProcessBasedMiniDFSCluster
  // is designed for single-NameNode, multi-DataNode scenarios and does not
  // support HA (High Availability) topology.
  //
  // Test uses HA-specific methods:
  // - cluster.transitionToActive(0/1) (lines 227, 253)
  // - cluster.transitionToStandby(0) (line 252)
  // - cluster.getFileSystem(namenodeIndex) (lines 228, 255)
  //
  // Test validates that EC policy information persists correctly when
  // replaying edit logs on standby NameNode after failover - HA-specific
  // functionality not applicable to single-NN upgrade testing.

  @SuppressWarnings("deprecation")
  @Test(timeout=180000)
  public void testStatistics() throws Exception {
    setupCluster();

    final String fileName = "/ec/file";
    final int size = 3200;
    createFile(fileName, size);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    InputStream in = null;
    try {
      in = fs.open(new Path(fileName));
      IOUtils.copyBytes(in, System.out, 4096, false);
    } finally {
      IOUtils.closeStream(in);
    }

    // verify stats are correct
    Long totalBytesRead = 0L;
    Long ecBytesRead = 0L;
    for (FileSystem.Statistics stat : FileSystem.getAllStatistics()) {
      totalBytesRead += stat.getBytesRead();
      ecBytesRead += stat.getBytesReadErasureCoded();
    }
    assertEquals(Long.valueOf(size), totalBytesRead);
    assertEquals(Long.valueOf(size), ecBytesRead);

    // verify thread local stats are correct
    Long totalBytesReadThread = 0L;
    Long ecBytesReadThread = 0L;
    for (FileSystem.Statistics stat : FileSystem.getAllStatistics()) {
      FileSystem.Statistics.StatisticsData data = stat.getThreadStatistics();
      totalBytesReadThread += data.getBytesRead();
      ecBytesReadThread += data.getBytesReadErasureCoded();
    }
    assertEquals(Long.valueOf(size), totalBytesReadThread);
    assertEquals(Long.valueOf(size), ecBytesReadThread);
  }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.test.GenericTestUtils;
import org.mockito.invocation.InvocationOnMock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.server.protocol.InterDatanodeProtocol;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppend3}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * NOTE: Tests TC7 and TC11 are skipped because they require direct DataNode
 * internal access (getFSDataset(), truncateData()) which is not available
 * via client APIs in ProcessBasedMiniDFSCluster.
 *
 * @see TestFileAppend3 Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFileAppend3_ProcessBased extends ProcessBasedUpgradeTestBase {
  {
    DFSTestUtil.setNameNodeLogLevel(Level.ALL);
    GenericTestUtils.setLogLevel(DataNode.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(DFSClient.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(InterDatanodeProtocol.LOG, org.slf4j
        .event.Level.TRACE);
  }

  static final long BLOCK_SIZE = 64 * 1024;
  static final short REPLICATION = 3;
  static final int DATANODE_NUM = 5;

  private int buffersize;

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_FIRST_WRITE,
      "AFTER_FIRST_CLOSE",
      UpgradeCheckpoints.AFTER_APPEND_REOPEN,
      UpgradeCheckpoints.AFTER_SECOND_WRITE,
      "AFTER_SECOND_CLOSE",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  /**
   * TC1: Append on block boundary.
   * @throws IOException an exception might be thrown
   */
  @Test
  public void testTC1() throws Exception {
    final Path p = new Path("/TC1/foo");
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file and write one block of data. Close file.
    final int len1 = (int)BLOCK_SIZE;
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    //   Reopen file to append. Append half block of data. Close file.
    final int len2 = (int)BLOCK_SIZE/2;
    out = fs.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    //b. Reopen file and read 1.5 blocks worth of data. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);
  }

  @Test
  public void testTC1ForAppend2() throws Exception {
    final Path p = new Path("/TC1/foo2");

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file and write one block of data. Close file.
    final int len1 = (int) BLOCK_SIZE;
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    // Reopen file to append. Append half block of data. Close file.
    final int len2 = (int) BLOCK_SIZE / 2;
    out = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    // b. Reopen file and read 1.5 blocks worth of data. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);
  }

  /**
   * TC2: Append on non-block boundary.
   * @throws IOException an exception might be thrown
   */
  @Test
  public void testTC2() throws Exception {
    final Path p = new Path("/TC2/foo");
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file with one and a half block of data. Close file.
    final int len1 = (int)(BLOCK_SIZE + BLOCK_SIZE/2);
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    AppendTestUtil.check(fs, p, len1);

    //   Reopen file to append quarter block of data. Close file.
    final int len2 = (int)BLOCK_SIZE/4;
    out = fs.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    //b. Reopen file and read 1.75 blocks of data. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);
  }

  @Test
  public void testTC2ForAppend2() throws Exception {
    final Path p = new Path("/TC2/foo2");

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file with one and a half block of data. Close file.
    final int len1 = (int) (BLOCK_SIZE + BLOCK_SIZE / 2);
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    AppendTestUtil.check(fs, p, len1);

    //   Reopen file to append quarter block of data. Close file.
    final int len2 = (int) BLOCK_SIZE / 4;
    out = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    // b. Reopen file and read 1.75 blocks of data. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);

    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    LocatedBlocks blocks = dfs.getClient().getLocatedBlocks(p.toString(), 0L);
    Assert.assertEquals(3, blocks.getLocatedBlocks().size());
    Assert.assertEquals(BLOCK_SIZE, blocks.getLocatedBlocks().get(0).getBlockSize());
    Assert.assertEquals(BLOCK_SIZE / 2, blocks.getLocatedBlocks().get(1).getBlockSize());
    Assert.assertEquals(BLOCK_SIZE / 4, blocks.getLocatedBlocks().get(2).getBlockSize());
  }

  /**
   * TC5: Only one simultaneous append.
   * @throws IOException an exception might be thrown
   */
  @Test
  public void testTC5() throws Exception {
    final Path p = new Path("/TC5/foo");
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file on Machine M1. Write half block to it. Close file.
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, (int)(BLOCK_SIZE/2));
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    //b. Reopen file in "append" mode on Machine M1.
    out = fs.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    //c. On Machine M2, reopen file in "append" mode. This should fail.
    try {
      AppendTestUtil.createHdfsWithDifferentUsername(conf).append(p);
      fail("This should fail.");
    } catch(IOException ioe) {
      AppendTestUtil.LOG.info("GOOD: got an exception", ioe);
    }

    try {
      ((DistributedFileSystem) AppendTestUtil
          .createHdfsWithDifferentUsername(conf)).append(p,
          EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
      fail("This should fail.");
    } catch(IOException ioe) {
      AppendTestUtil.LOG.info("GOOD: got an exception", ioe);
    }

    //d. On Machine M1, close file.
    out.close();
  }

  @Test
  public void testTC5ForAppend2() throws Exception {
    final Path p = new Path("/TC5/foo2");

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // a. Create file on Machine M1. Write half block to it. Close file.
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, (int)(BLOCK_SIZE/2));
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    // b. Reopen file in "append" mode on Machine M1.
    out = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    // c. On Machine M2, reopen file in "append" mode. This should fail.
    try {
      ((DistributedFileSystem) AppendTestUtil
          .createHdfsWithDifferentUsername(conf)).append(p,
          EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
      fail("This should fail.");
    } catch(IOException ioe) {
      AppendTestUtil.LOG.info("GOOD: got an exception", ioe);
    }

    try {
      AppendTestUtil.createHdfsWithDifferentUsername(conf).append(p);
      fail("This should fail.");
    } catch(IOException ioe) {
      AppendTestUtil.LOG.info("GOOD: got an exception", ioe);
    }

    // d. On Machine M1, close file.
    out.close();
  }

  // TRANSFORMATION NOTE: TC7 tests skipped
  // Tests testTC7() and testTC7ForAppend2() require direct DataNode access
  // to truncate replica data (cluster.getDataNode(), getMaterializedReplica(),
  // truncateData()). These operations are not available via client APIs
  // in ProcessBasedMiniDFSCluster as they require direct access to DataNode
  // internal storage.

  // TRANSFORMATION NOTE: TC11 tests skipped
  // Tests testTC11() and testTC11ForAppend2() require direct DataNode access
  // to verify block metadata (cluster.getDataNode(), DataNodeTestUtils.getFSDataset(),
  // getStoredBlock()). While the main test logic (create, append, rename) could
  // be transformed, the block-level verification on each DataNode is not possible
  // via client APIs.

  /**
   * TC12: Append to partial CRC chunk
   */
  @Test
  public void testTC12() throws Exception {
    final Path p = new Path("/TC12/foo0");
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file with a block size of 64KB
    //   and a default io.bytes.per.checksum of 512 bytes.
    //   Write 25687 bytes of data. Close file.
    final int len1 = 25687;
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    //b. Reopen file in "append" mode. Append another 5877 bytes of data. Close file.
    final int len2 = 5877;
    out = fs.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    //c. Reopen file and read 25687+5877 bytes of data from file. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);
  }

  @Test
  public void testTC12ForAppend2() throws Exception {
    final Path p = new Path("/TC12/foo1");
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    //a. Create file with a block size of 64KB
    //   and a default io.bytes.per.checksum of 512 bytes.
    //   Write 25687 bytes of data. Close file.
    final int len1 = 25687;
    FSDataOutputStream out = fs.create(p, false, buffersize, REPLICATION, BLOCK_SIZE);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(out, 0, len1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    out.close();
    checkpoint("AFTER_FIRST_CLOSE");

    //b. Reopen file in "append" mode. Append another 5877 bytes of data. Close file.
    final int len2 = 5877;
    out = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    AppendTestUtil.write(out, len1, len2);
    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    out.close();
    checkpoint("AFTER_SECOND_CLOSE");

    //c. Reopen file and read 25687+5877 bytes of data from file. Close file.
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.check(fs, p, len1 + len2);

    DistributedFileSystem dfs = (DistributedFileSystem) fs;
    LocatedBlocks blks = dfs.getClient().getLocatedBlocks(p.toString(), 0);
    Assert.assertEquals(2, blks.getLocatedBlocks().size());
    Assert.assertEquals(len1, blks.getLocatedBlocks().get(0).getBlockSize());
    Assert.assertEquals(len2, blks.getLocatedBlocks().get(1).getBlockSize());
    AppendTestUtil.check(fs, p, 0, len1);
    AppendTestUtil.check(fs, p, len1, len2);
  }

  /**
   * Append to a partial CRC chunk and the first write does not fill up the
   * partial CRC trunk
   */
  @Test
  public void testAppendToPartialChunk() throws Exception {
    final Path p = new Path("/partialChunk/foo0");
    final int fileLen = 513;
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    byte[] fileContents = AppendTestUtil.initBuffer(fileLen);

    // create a new file.
    FSDataOutputStream stm = AppendTestUtil.createFile(fs, p, 1);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // create 1 byte file
    stm.write(fileContents, 0, 1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    stm.close();
    checkpoint("AFTER_FIRST_CLOSE");
    System.out.println("Wrote 1 byte and closed the file " + p);

    // append to file
    stm = fs.append(p);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    // Append to a partial CRC trunk
    stm.write(fileContents, 1, 1);
    stm.hflush();
    checkpoint("AFTER_SECOND_WRITE_FLUSH");

    // The partial CRC trunk is not full yet and close the file
    stm.close();
    checkpoint("AFTER_SECOND_CLOSE");
    System.out.println("Append 1 byte and closed the file " + p);

    // write the remainder of the file
    stm = fs.append(p);
    checkpoint("AFTER_THIRD_REOPEN");

    // ensure getPos is set to reflect existing size of the file
    assertEquals(2, stm.getPos());

    // append to a partial CRC trunk
    stm.write(fileContents, 2, 1);
    // The partial chunk is not full yet, force to send a packet to DN
    stm.hflush();
    checkpoint("AFTER_THIRD_WRITE_FLUSH");
    System.out.println("Append and flush 1 byte");

    // The partial chunk is not full yet, force to send another packet to DN
    stm.write(fileContents, 3, 2);
    stm.hflush();
    checkpoint("AFTER_FOURTH_WRITE_FLUSH");
    System.out.println("Append and flush 2 byte");

    // fill up the partial chunk and close the file
    stm.write(fileContents, 5, fileLen-5);
    checkpoint("AFTER_FIFTH_WRITE");

    stm.close();
    checkpoint("AFTER_FINAL_CLOSE");
    System.out.println("Flush 508 byte and closed the file " + p);

    // verify that entire file is good
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.checkFullFile(fs, p, fileLen,
        fileContents, "Failed to append to a partial chunk");
  }

  @Test
  public void testAppendToPartialChunkforAppend2() throws Exception {
    final Path p = new Path("/partialChunk/foo1");
    final int fileLen = 513;
    System.out.println("p=" + p);

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    byte[] fileContents = AppendTestUtil.initBuffer(fileLen);

    // create a new file.
    FSDataOutputStream stm = AppendTestUtil.createFile(fs, p, 1);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // create 1 byte file
    stm.write(fileContents, 0, 1);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    stm.close();
    checkpoint("AFTER_FIRST_CLOSE");
    System.out.println("Wrote 1 byte and closed the file " + p);

    // append to file
    stm = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    // Append to a partial CRC trunk
    stm.write(fileContents, 1, 1);
    stm.hflush();
    checkpoint("AFTER_SECOND_WRITE_FLUSH");

    // The partial CRC trunk is not full yet and close the file
    stm.close();
    checkpoint("AFTER_SECOND_CLOSE");
    System.out.println("Append 1 byte and closed the file " + p);

    // write the remainder of the file
    stm = fs.append(p, EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
    checkpoint("AFTER_THIRD_REOPEN");

    // ensure getPos is set to reflect existing size of the file
    assertEquals(2, stm.getPos());

    // append to a partial CRC trunk
    stm.write(fileContents, 2, 1);
    // The partial chunk is not full yet, force to send a packet to DN
    stm.hflush();
    checkpoint("AFTER_THIRD_WRITE_FLUSH");
    System.out.println("Append and flush 1 byte");

    // The partial chunk is not full yet, force to send another packet to DN
    stm.write(fileContents, 3, 2);
    stm.hflush();
    checkpoint("AFTER_FOURTH_WRITE_FLUSH");
    System.out.println("Append and flush 2 byte");

    // fill up the partial chunk and close the file
    stm.write(fileContents, 5, fileLen-5);
    checkpoint("AFTER_FIFTH_WRITE");

    stm.close();
    checkpoint("AFTER_FINAL_CLOSE");
    System.out.println("Flush 508 byte and closed the file " + p);

    // verify that entire file is good
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
    AppendTestUtil.checkFullFile(fs, p, fileLen,
        fileContents, "Failed to append to a partial chunk");
  }

  // Do small appends.
  void doSmallAppends(Path file, DistributedFileSystem dfs, int iterations)
    throws Exception {
    for (int i = 0; i < iterations; i++) {
      FSDataOutputStream stm;
      try {
        stm = dfs.append(file);
      } catch (IOException e) {
        // If another thread is already appending, skip this time.
        continue;
      }
      // Failure in write or close will be terminal.
      AppendTestUtil.write(stm, 0, 123);
      stm.close();
    }
  }

  @Test
  public void testSmallAppendRace() throws Exception {
    final Path file = new Path("/testSmallAppendRace");
    final String fName = file.toUri().getPath();

    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);
    buffersize = conf.getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(DATANODE_NUM)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create the file and write a small amount of data.
    FSDataOutputStream stm = fs.create(file);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    AppendTestUtil.write(stm, 0, 123);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    stm.close();
    checkpoint("AFTER_FIRST_CLOSE");

    // Introduce a delay between getFileInfo and calling append() against NN.
    final DFSClient client = DFSClientAdapter.getDFSClient(fs);
    DFSClient spyClient = spy(client);
    when(spyClient.getFileInfo(fName)).thenAnswer(new Answer<HdfsFileStatus>() {
      @Override
      public HdfsFileStatus answer(InvocationOnMock invocation){
        try {
          HdfsFileStatus stat = client.getFileInfo(fName);
          Thread.sleep(100);
          return stat;
        } catch (Exception e) {
          return null;
        }
      }
    });

    DFSClientAdapter.setDFSClient(fs, spyClient);

    // Create two threads for doing appends to the same file.
    Thread worker1 = new Thread() {
      @Override
      public void run() {
        try {
          doSmallAppends(file, fs, 20);
        } catch (Exception e) {
        }
      }
    };

    Thread worker2 = new Thread() {
      @Override
      public void run() {
        try {
          doSmallAppends(file, fs, 20);
        } catch (Exception e) {
        }
      }
    };

    worker1.start();
    worker2.start();

    // append will fail when the file size crosses the checksum chunk boundary,
    // if append was called with a stale file stat.
    doSmallAppends(file, fs, 20);
  }
}

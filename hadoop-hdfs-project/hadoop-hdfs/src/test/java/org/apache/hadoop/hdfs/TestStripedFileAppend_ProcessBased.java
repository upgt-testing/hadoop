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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.OpenFileEntry;
import org.apache.hadoop.hdfs.protocol.OpenFilesIterator.OpenFilesType;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestStripedFileAppend}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests append on erasure coded file.
 *
 * @see TestStripedFileAppend Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestStripedFileAppend_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Log LOG = LogFactory.getLog(TestStripedFileAppend_ProcessBased.class);

  static {
    DFSTestUtil.setNameNodeLogLevel(Level.ALL);
  }

  private static final int NUM_DATA_BLOCKS =
      StripedFileTestUtil.getDefaultECPolicy().getNumDataUnits();
  private static final int CELL_SIZE =
      StripedFileTestUtil.getDefaultECPolicy().getCellSize();
  private static final int NUM_DN = 9;
  private static final int STRIPES_PER_BLOCK = 4;
  private static final int BLOCK_SIZE = CELL_SIZE * STRIPES_PER_BLOCK;
  private static final int BLOCK_GROUP_SIZE = BLOCK_SIZE * NUM_DATA_BLOCKS;
  private static final Random RANDOM = new Random();

  private DistributedFileSystem dfs;
  private Path dir = new Path("/TestFileAppendStriped");

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_DIR_SETUP",
      "AFTER_APPEND_OPERATION",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Before
  @Override
  public void setupTest() throws Exception {
    super.setupTest();
    conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(NUM_DN).build();
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    fs = dfs;
    dfs.mkdirs(dir);
    dfs.setErasureCodingPolicy(dir, null);
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
  }

  /**
   * test simple append to a closed striped file, with NEW_BLOCK flag enabled.
   */
  @Test
  public void testAppendToNewBlock() throws Exception {
    int fileLength = 0;
    int totalSplit = 6;
    byte[] expected =
        StripedFileTestUtil.generateBytes(BLOCK_GROUP_SIZE * totalSplit);

    Path file = new Path(dir, "testAppendToNewBlock");
    checkpoint("AFTER_DIR_SETUP");

    FSDataOutputStream out;
    for (int split = 0; split < totalSplit; split++) {
      if (split == 0) {
        out = dfs.create(file);
      } else {
        out = dfs.append(file,
            EnumSet.of(CreateFlag.APPEND, CreateFlag.NEW_BLOCK), 4096, null);
      }
      int splitLength = RANDOM.nextInt(BLOCK_GROUP_SIZE);
      out.write(expected, fileLength, splitLength);
      fileLength += splitLength;
      out.close();
    }
    checkpoint("AFTER_APPEND_OPERATION");

    expected = Arrays.copyOf(expected, fileLength);
    LocatedBlocks lbs =
        dfs.getClient().getLocatedBlocks(file.toString(), 0L, Long.MAX_VALUE);
    assertEquals(totalSplit, lbs.getLocatedBlocks().size());
    StripedFileTestUtil.verifyStatefulRead(dfs, file, fileLength, expected,
        new byte[4096]);
    StripedFileTestUtil.verifySeek(dfs, file, fileLength,
        StripedFileTestUtil.getDefaultECPolicy(), totalSplit);
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }

  @Test
  public void testAppendWithoutNewBlock() throws Exception {
    Path file = new Path(dir, "testAppendWithoutNewBlock");
    checkpoint("AFTER_DIR_SETUP");

    // Create file
    FSDataOutputStream out = dfs.create(file);
    out.write("testAppendWithoutNewBlock".getBytes());
    out.close();
    checkpoint("AFTER_APPEND_OPERATION");

    // Append file
    try {
      out = dfs.append(file, EnumSet.of(CreateFlag.APPEND), 4096, null);
      out.write("testAppendWithoutNewBlock".getBytes());
      fail("Should throw unsupported operation");
    } catch (Exception e) {
      assertTrue(e.getMessage()
          .contains("Append on EC file without new block is not supported"));
    }

    List<OpenFilesType> types = new ArrayList<>();
    types.add(OpenFilesType.ALL_OPEN_FILES);

    RemoteIterator<OpenFileEntry> listOpenFiles = dfs
        .listOpenFiles(EnumSet.copyOf(types), file.toString());
    assertFalse("No file should be open after append failure",
        listOpenFiles.hasNext());
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

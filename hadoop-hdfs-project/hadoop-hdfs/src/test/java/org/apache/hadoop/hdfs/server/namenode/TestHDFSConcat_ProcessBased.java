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
package org.apache.hadoop.hdfs.server.namenode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestHDFSConcat}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios. Tests HDFS
 * concat operation (merging multiple files).
 *
 * Note: This is a focused transformation containing the most upgrade-relevant
 * test methods (testConcat and testConcatInEditLog). Additional test methods
 * from the original test can be added as needed.
 *
 * @see TestHDFSConcat Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestHDFSConcat_ProcessBased extends ProcessBasedUpgradeTestBase {

  public static final Logger LOG =
      LoggerFactory.getLogger(TestHDFSConcat_ProcessBased.class);

  private static final short REPL_FACTOR = 2;
  private static final long blockSize = 512;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_CREATE",
      "AFTER_CONCAT",
      "AFTER_VERIFICATION"
    );
  }

  /**
   * Concatenates 10 files into one.
   * Verifies the final size, deletion of source files, number of blocks.
   */
  @Test
  public void testConcat() throws Exception {
    final int numFiles = 10;
    long fileLen = blockSize * 3;
    HdfsFileStatus fStatus;
    FSDataInputStream stm;

    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPL_FACTOR)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem dfs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    String trg = "/trg";
    Path trgPath = new Path(trg);
    DFSTestUtil.createFile(dfs, trgPath, fileLen, REPL_FACTOR, 1);

    // Use client-side API instead of nn.getFileInfo()
    fStatus = dfs.getClient().getFileInfo(trg);
    long trgLen = fStatus.getLen();

    // Use client-side API instead of nn.getBlockLocations()
    LocatedBlocks trgLocatedBlocks = dfs.getClient().getLocatedBlocks(trg, 0, trgLen);
    long trgBlocks = trgLocatedBlocks.locatedBlockCount();

    Path[] files = new Path[numFiles];
    byte[][] bytes = new byte[numFiles + 1][(int) fileLen];
    LocatedBlocks[] lblocks = new LocatedBlocks[numFiles];
    long[] lens = new long[numFiles];

    stm = dfs.open(trgPath);
    stm.readFully(0, bytes[0]);
    stm.close();

    int i;
    for (i = 0; i < files.length; i++) {
      files[i] = new Path("/file" + i);
      Path path = files[i];
      System.out.println("Creating file " + path);

      // make files with different content
      DFSTestUtil.createFile(dfs, path, fileLen, REPL_FACTOR, i);

      // Use client-side API
      fStatus = dfs.getClient().getFileInfo(path.toUri().getPath());
      lens[i] = fStatus.getLen();
      assertEquals(trgLen, lens[i]); // file of the same length.

      // Use client-side API
      lblocks[i] = dfs.getClient().getLocatedBlocks(path.toUri().getPath(), 0, lens[i]);

      //read the file
      stm = dfs.open(path);
      stm.readFully(0, bytes[i + 1]);
      stm.close();
    }
    checkpoint("AFTER_FILE_CREATE");

    // check count update
    ContentSummary cBefore = dfs.getContentSummary(trgPath.getParent());

    // now concatenate
    dfs.concat(trgPath, files);
    checkpoint("AFTER_CONCAT");

    // verify count
    ContentSummary cAfter = dfs.getContentSummary(trgPath.getParent());
    assertEquals(cBefore.getFileCount(), cAfter.getFileCount() + files.length);

    // verify other stuff
    long totalLen = trgLen;
    long totalBlocks = trgBlocks;
    for (i = 0; i < files.length; i++) {
      totalLen += lens[i];
      totalBlocks += lblocks[i].locatedBlockCount();
    }
    System.out.println("total len=" + totalLen + "; totalBlocks=" + totalBlocks);

    // Use client-side API
    fStatus = dfs.getClient().getFileInfo(trg);
    trgLen = fStatus.getLen(); // new length

    // read the resulting file
    stm = dfs.open(trgPath);
    byte[] byteFileConcat = new byte[(int) trgLen];
    stm.readFully(0, byteFileConcat);
    stm.close();

    // Use client-side API
    trgBlocks = dfs.getClient().getLocatedBlocks(trg, 0, trgLen).locatedBlockCount();

    //verifications
    // 1. number of blocks
    assertEquals(trgBlocks, totalBlocks);

    // 2. file lengths
    assertEquals(trgLen, totalLen);

    // 3. removal of the src files
    for (Path p : files) {
      assertFalse("File " + p + " still exists", dfs.exists(p));
      // try to create file with the same name
      DFSTestUtil.createFile(dfs, p, fileLen, REPL_FACTOR, 1);
    }

    // 4. content
    checkFileContent(byteFileConcat, bytes);
    checkpoint("AFTER_VERIFICATION");

    // add a small file (less than a block)
    Path smallFile = new Path("/sfile");
    int sFileLen = 10;
    DFSTestUtil.createFile(dfs, smallFile, sFileLen, REPL_FACTOR, 1);
    dfs.concat(trgPath, new Path[]{smallFile});

    // Use client-side API
    fStatus = dfs.getClient().getFileInfo(trg);
    trgLen = fStatus.getLen(); // new length

    // check number of blocks
    trgBlocks = dfs.getClient().getLocatedBlocks(trg, 0, trgLen).locatedBlockCount();
    assertEquals(trgBlocks, totalBlocks + 1);

    // and length
    assertEquals(trgLen, totalLen + sFileLen);
  }

  /**
   * Test that the concat operation is properly persisted in the
   * edit log, and properly replayed on restart.
   */
  @Test
  public void testConcatInEditLog() throws Exception {
    final Path TEST_DIR = new Path("/testConcatInEditLog");
    final long FILE_LEN = blockSize;

    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPL_FACTOR)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem dfs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // 1. Concat some files
    Path[] srcFiles = new Path[3];
    for (int i = 0; i < srcFiles.length; i++) {
      Path path = new Path(TEST_DIR, "src-" + i);
      DFSTestUtil.createFile(dfs, path, FILE_LEN, REPL_FACTOR, 1);
      srcFiles[i] = path;
    }
    Path targetFile = new Path(TEST_DIR, "target");
    DFSTestUtil.createFile(dfs, targetFile, FILE_LEN, REPL_FACTOR, 1);
    checkpoint("AFTER_FILE_CREATE");

    dfs.concat(targetFile, srcFiles);
    checkpoint("AFTER_CONCAT");

    // 2. Verify the concat operation basically worked, and record
    // file status.
    assertTrue(dfs.exists(targetFile));
    FileStatus origStatus = dfs.getFileStatus(targetFile);

    // 3. Restart NN to force replay from edit log
    // Use cluster.restartNameNode(0) instead of cluster.restartNameNode(true)
    cluster.restartNameNode(0);
    cluster.waitClusterUp();
    checkpoint("AFTER_VERIFICATION");

    // Refresh dfs reference after restart
    dfs = cluster.getFileSystem();

    // 4. Verify concat operation was replayed correctly and file status
    // did not change.
    assertTrue(dfs.exists(targetFile));
    assertFalse(dfs.exists(srcFiles[0]));

    FileStatus statusAfterRestart = dfs.getFileStatus(targetFile);

    assertEquals(origStatus.getModificationTime(),
        statusAfterRestart.getModificationTime());
  }

  // compare content
  private void checkFileContent(byte[] concat, byte[][] bytes) {
    int idx = 0;
    boolean mismatch = false;

    for (byte[] bb : bytes) {
      for (byte b : bb) {
        if (b != concat[idx++]) {
          mismatch = true;
          break;
        }
      }
      if (mismatch)
        break;
    }
    assertFalse("File content of concatenated file is different", mismatch);
  }
}

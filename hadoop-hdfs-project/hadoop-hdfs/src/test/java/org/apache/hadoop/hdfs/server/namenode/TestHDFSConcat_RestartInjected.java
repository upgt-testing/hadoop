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

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestHDFSConcat_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestHDFSConcat_RestartInjected.class);

  private static final short REPL_FACTOR = 2;

  private MiniDFSCluster cluster;
  private NamenodeProtocols nn;
  private DistributedFileSystem dfs;

  private static final long blockSize = 512;


  private static final Configuration conf;

  static {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, blockSize);
  }

  @Before
  public void startUpCluster() throws IOException {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPL_FACTOR).build();
    assertNotNull("Failed Cluster Creation", cluster);
    cluster.waitClusterUp();
    dfs = cluster.getFileSystem();
    assertNotNull("Failed to get FileSystem", dfs);
    nn = cluster.getNameNodeRpc();
    assertNotNull("Failed to get NameNode", nn);
  }

  @After
  public void shutDownCluster() throws IOException {
    if(dfs != null) {
      dfs.close();
      dfs = null;
    }
    if(cluster != null) {
      cluster.shutdownDataNodes();
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testConcat_AfterConcat_NN_Graceful() throws Exception {
    LOG.info("=== testConcat_AfterConcat_NN_Graceful ===");
    final int numFiles = 10;
    long fileLen = blockSize*3;
    HdfsFileStatus fStatus;
    FSDataInputStream stm;

    String trg = "/trg";
    Path trgPath = new Path(trg);
    DFSTestUtil.createFile(dfs, trgPath, fileLen, REPL_FACTOR, 1);
    fStatus  = nn.getFileInfo(trg);
    long trgLen = fStatus.getLen();
    long trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();

    Path [] files = new Path[numFiles];
    byte[][] bytes = new byte[numFiles + 1][(int) fileLen];
    LocatedBlocks [] lblocks = new LocatedBlocks[numFiles];
    long [] lens = new long [numFiles];

    stm = dfs.open(trgPath);
    stm.readFully(0, bytes[0]);
    stm.close();
    int i;
    for(i=0; i<files.length; i++) {
      files[i] = new Path("/file"+i);
      Path path = files[i];
      System.out.println("Creating file " + path);

      DFSTestUtil.createFile(dfs, path, fileLen, REPL_FACTOR, i);
      fStatus = nn.getFileInfo(path.toUri().getPath());
      lens[i] = fStatus.getLen();
      assertEquals(trgLen, lens[i]);

      lblocks[i] = nn.getBlockLocations(path.toUri().getPath(), 0, lens[i]);

      stm = dfs.open(path);
      stm.readFully(0, bytes[i + 1]);
      stm.close();
    }

    ContentSummary cBefore = dfs.getContentSummary(trgPath.getParent());

    for (int j = 0; j < files.length / 2; j++) {
      Path tempPath = files[j];
      files[j] = files[files.length - 1 - j];
      files[files.length - 1 - j] = tempPath;

      byte[] tempBytes = bytes[1 + j];
      bytes[1 + j] = bytes[files.length - 1 - j + 1];
      bytes[files.length - 1 - j + 1] = tempBytes;
    }

    dfs.concat(trgPath, files);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    ContentSummary cAfter = dfs.getContentSummary(trgPath.getParent());
    assertEquals(cBefore.getFileCount(), cAfter.getFileCount()+files.length);

    long totalLen = trgLen;
    long totalBlocks = trgBlocks;
    for(i=0; i<files.length; i++) {
      totalLen += lens[i];
      totalBlocks += lblocks[i].locatedBlockCount();
    }
    System.out.println("total len=" + totalLen + "; totalBlocks=" + totalBlocks);


    fStatus = nn.getFileInfo(trg);
    trgLen  = fStatus.getLen();

    stm = dfs.open(trgPath);
    byte[] byteFileConcat = new byte[(int)trgLen];
    stm.readFully(0, byteFileConcat);
    stm.close();

    trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();

    assertEquals(trgBlocks, totalBlocks);

    assertEquals(trgLen, totalLen);

    for(Path p: files) {
      fStatus = nn.getFileInfo(p.toUri().getPath());
      assertNull("File " + p + " still exists", fStatus);
      DFSTestUtil.createFile(dfs, p, fileLen, REPL_FACTOR, 1);
    }

    checkFileContent(byteFileConcat, bytes);

    Path smallFile = new Path("/sfile");
    int sFileLen = 10;
    DFSTestUtil.createFile(dfs, smallFile, sFileLen, REPL_FACTOR, 1);
    dfs.concat(trgPath, new Path [] {smallFile});

    fStatus = nn.getFileInfo(trg);
    trgLen  = fStatus.getLen();

    trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();
    assertEquals(trgBlocks, totalBlocks+1);

    assertEquals(trgLen, totalLen+sFileLen);
  }

  @Test(timeout = 120000)
  public void testConcat_AfterConcat_NN_Crash() throws Exception {
    LOG.info("=== testConcat_AfterConcat_NN_Crash ===");
    final int numFiles = 10;
    long fileLen = blockSize*3;
    HdfsFileStatus fStatus;
    FSDataInputStream stm;

    String trg = "/trg";
    Path trgPath = new Path(trg);
    DFSTestUtil.createFile(dfs, trgPath, fileLen, REPL_FACTOR, 1);
    fStatus  = nn.getFileInfo(trg);
    long trgLen = fStatus.getLen();
    long trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();

    Path [] files = new Path[numFiles];
    byte[][] bytes = new byte[numFiles + 1][(int) fileLen];
    LocatedBlocks [] lblocks = new LocatedBlocks[numFiles];
    long [] lens = new long [numFiles];

    stm = dfs.open(trgPath);
    stm.readFully(0, bytes[0]);
    stm.close();
    int i;
    for(i=0; i<files.length; i++) {
      files[i] = new Path("/file"+i);
      Path path = files[i];
      System.out.println("Creating file " + path);

      DFSTestUtil.createFile(dfs, path, fileLen, REPL_FACTOR, i);
      fStatus = nn.getFileInfo(path.toUri().getPath());
      lens[i] = fStatus.getLen();
      assertEquals(trgLen, lens[i]);

      lblocks[i] = nn.getBlockLocations(path.toUri().getPath(), 0, lens[i]);

      stm = dfs.open(path);
      stm.readFully(0, bytes[i + 1]);
      stm.close();
    }

    ContentSummary cBefore = dfs.getContentSummary(trgPath.getParent());

    for (int j = 0; j < files.length / 2; j++) {
      Path tempPath = files[j];
      files[j] = files[files.length - 1 - j];
      files[files.length - 1 - j] = tempPath;

      byte[] tempBytes = bytes[1 + j];
      bytes[1 + j] = bytes[files.length - 1 - j + 1];
      bytes[files.length - 1 - j + 1] = tempBytes;
    }

    dfs.concat(trgPath, files);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    ContentSummary cAfter = dfs.getContentSummary(trgPath.getParent());
    assertEquals(cBefore.getFileCount(), cAfter.getFileCount()+files.length);

    long totalLen = trgLen;
    long totalBlocks = trgBlocks;
    for(i=0; i<files.length; i++) {
      totalLen += lens[i];
      totalBlocks += lblocks[i].locatedBlockCount();
    }
    System.out.println("total len=" + totalLen + "; totalBlocks=" + totalBlocks);


    fStatus = nn.getFileInfo(trg);
    trgLen  = fStatus.getLen();

    stm = dfs.open(trgPath);
    byte[] byteFileConcat = new byte[(int)trgLen];
    stm.readFully(0, byteFileConcat);
    stm.close();

    trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();

    assertEquals(trgBlocks, totalBlocks);

    assertEquals(trgLen, totalLen);

    for(Path p: files) {
      fStatus = nn.getFileInfo(p.toUri().getPath());
      assertNull("File " + p + " still exists", fStatus);
      DFSTestUtil.createFile(dfs, p, fileLen, REPL_FACTOR, 1);
    }

    checkFileContent(byteFileConcat, bytes);

    Path smallFile = new Path("/sfile");
    int sFileLen = 10;
    DFSTestUtil.createFile(dfs, smallFile, sFileLen, REPL_FACTOR, 1);
    dfs.concat(trgPath, new Path [] {smallFile});

    fStatus = nn.getFileInfo(trg);
    trgLen  = fStatus.getLen();

    trgBlocks = nn.getBlockLocations(trg, 0, trgLen).locatedBlockCount();
    assertEquals(trgBlocks, totalBlocks+1);

    assertEquals(trgLen, totalLen+sFileLen);
  }

  @Test(timeout = 120000)
  public void testConcatNotCompleteBlock_AfterConcat_NN_Graceful() throws Exception {
    LOG.info("=== testConcatNotCompleteBlock_AfterConcat_NN_Graceful ===");
    long trgFileLen = blockSize*3;
    long srcFileLen = blockSize*3+20;


    String name1="/trg", name2="/src";
    Path filePath1 = new Path(name1);
    DFSTestUtil.createFile(dfs, filePath1, trgFileLen, REPL_FACTOR, 1);

    HdfsFileStatus fStatus = nn.getFileInfo(name1);
    long fileLen = fStatus.getLen();
    assertEquals(fileLen, trgFileLen);

    FSDataInputStream stm = dfs.open(filePath1);
    byte[] byteFile1 = new byte[(int)trgFileLen];
    stm.readFully(0, byteFile1);
    stm.close();

    LocatedBlocks lb1 = nn.getBlockLocations(name1, 0, trgFileLen);

    Path filePath2 = new Path(name2);
    DFSTestUtil.createFile(dfs, filePath2, srcFileLen, REPL_FACTOR, 1);
    fStatus = nn.getFileInfo(name2);
    fileLen = fStatus.getLen();
    assertEquals(srcFileLen, fileLen);

    stm = dfs.open(filePath2);
    byte[] byteFile2 = new byte[(int)srcFileLen];
    stm.readFully(0, byteFile2);
    stm.close();

    LocatedBlocks lb2 = nn.getBlockLocations(name2, 0, srcFileLen);


    System.out.println("trg len="+trgFileLen+"; src len="+srcFileLen);

    dfs.concat(filePath1, new Path [] {filePath2});

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    long totalLen = trgFileLen + srcFileLen;
    fStatus = nn.getFileInfo(name1);
    fileLen = fStatus.getLen();

    stm = dfs.open(filePath1);
    byte[] byteFileConcat = new byte[(int)fileLen];
    stm.readFully(0, byteFileConcat);
    stm.close();

    LocatedBlocks lbConcat = nn.getBlockLocations(name1, 0, fileLen);

    assertEquals(lbConcat.locatedBlockCount(),
        lb1.locatedBlockCount() + lb2.locatedBlockCount());

    System.out.println("file1 len="+fileLen+"; total len="+totalLen);
    assertEquals(fileLen, totalLen);

    fStatus = nn.getFileInfo(name2);
    assertNull("File "+name2+ "still exists", fStatus);

    checkFileContent(byteFileConcat, new byte [] [] {byteFile1, byteFile2});
  }

  @Test(timeout = 120000)
  public void testConcatNotCompleteBlock_AfterConcat_NN_Crash() throws Exception {
    LOG.info("=== testConcatNotCompleteBlock_AfterConcat_NN_Crash ===");
    long trgFileLen = blockSize*3;
    long srcFileLen = blockSize*3+20;


    String name1="/trg", name2="/src";
    Path filePath1 = new Path(name1);
    DFSTestUtil.createFile(dfs, filePath1, trgFileLen, REPL_FACTOR, 1);

    HdfsFileStatus fStatus = nn.getFileInfo(name1);
    long fileLen = fStatus.getLen();
    assertEquals(fileLen, trgFileLen);

    FSDataInputStream stm = dfs.open(filePath1);
    byte[] byteFile1 = new byte[(int)trgFileLen];
    stm.readFully(0, byteFile1);
    stm.close();

    LocatedBlocks lb1 = nn.getBlockLocations(name1, 0, trgFileLen);

    Path filePath2 = new Path(name2);
    DFSTestUtil.createFile(dfs, filePath2, srcFileLen, REPL_FACTOR, 1);
    fStatus = nn.getFileInfo(name2);
    fileLen = fStatus.getLen();
    assertEquals(srcFileLen, fileLen);

    stm = dfs.open(filePath2);
    byte[] byteFile2 = new byte[(int)srcFileLen];
    stm.readFully(0, byteFile2);
    stm.close();

    LocatedBlocks lb2 = nn.getBlockLocations(name2, 0, srcFileLen);


    System.out.println("trg len="+trgFileLen+"; src len="+srcFileLen);

    dfs.concat(filePath1, new Path [] {filePath2});

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    long totalLen = trgFileLen + srcFileLen;
    fStatus = nn.getFileInfo(name1);
    fileLen = fStatus.getLen();

    stm = dfs.open(filePath1);
    byte[] byteFileConcat = new byte[(int)fileLen];
    stm.readFully(0, byteFileConcat);
    stm.close();

    LocatedBlocks lbConcat = nn.getBlockLocations(name1, 0, fileLen);

    assertEquals(lbConcat.locatedBlockCount(),
        lb1.locatedBlockCount() + lb2.locatedBlockCount());

    System.out.println("file1 len="+fileLen+"; total len="+totalLen);
    assertEquals(fileLen, totalLen);

    fStatus = nn.getFileInfo(name2);
    assertNull("File "+name2+ "still exists", fStatus);

    checkFileContent(byteFileConcat, new byte [] [] {byteFile1, byteFile2});
  }

  private void checkFileContent(byte[] concat, byte[][] bytes ) {
    int idx=0;
    boolean mismatch = false;

    for(byte [] bb: bytes) {
      for(byte b: bb) {
        if(b != concat[idx++]) {
          mismatch=true;
          break;
        }
      }
      if(mismatch)
        break;
    }
    assertFalse("File content of concatenated file is different", mismatch);
  }
}

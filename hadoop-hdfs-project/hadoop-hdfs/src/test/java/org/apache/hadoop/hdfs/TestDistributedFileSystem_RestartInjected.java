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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * Restart-injection variant of TestDistributedFileSystem.
 * Tests that DFS operations persist correctly across restarts.
 */
public class TestDistributedFileSystem_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestDistributedFileSystem_RestartInjected.class);
  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  /**
   * Test basic DFS file operations after restart.
   */
  private void testDFSFileOpsWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create a file with specific content
      Path filePath = new Path("/testDFSFileOps.dat");
      byte[] testData = "TestDistributedFileSystem restart test data".getBytes();
      FSDataOutputStream out = fs.create(filePath, REPLICATION);
      out.write(testData);
      out.close();

      // Create a directory
      Path dirPath = new Path("/testDir");
      assertTrue("mkdir should succeed", fs.mkdirs(dirPath));

      // Verify initial state
      assertTrue("File should exist before restart", fs.exists(filePath));
      assertTrue("Directory should exist before restart", fs.exists(dirPath));
      assertEquals("File length should be correct", testData.length, fs.getFileStatus(filePath).getLen());

      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      fs = cluster.getFileSystem();
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Verify state after restart
      assertTrue("File should exist after restart", fs.exists(filePath));
      assertTrue("Directory should exist after restart", fs.exists(dirPath));
      assertEquals("File length should be correct after restart", testData.length, fs.getFileStatus(filePath).getLen());

      // Read back and verify content
      FSDataInputStream in = fs.open(filePath);
      byte[] readData = new byte[testData.length];
      in.readFully(readData);
      in.close();
      assertArrayEquals("File content should match after restart", testData, readData);
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Test file rename operation persistence across restart.
   */
  private void testDFSRenameWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();

      // Create file and rename
      Path srcPath = new Path("/srcFile.dat");
      Path dstPath = new Path("/dstFile.dat");
      DFSTestUtil.createFile(fs, srcPath, BLOCKSIZE, REPLICATION, 0);
      assertTrue("Rename should succeed", fs.rename(srcPath, dstPath));

      // Verify before restart
      assertFalse("Source should not exist after rename", fs.exists(srcPath));
      assertTrue("Destination should exist after rename", fs.exists(dstPath));

      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      fs = cluster.getFileSystem();
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      // Verify after restart
      assertFalse("Source should not exist after restart", fs.exists(srcPath));
      assertTrue("Destination should exist after restart", fs.exists(dstPath));
      assertEquals("File length should be correct", BLOCKSIZE, fs.getFileStatus(dstPath).getLen());
    } finally {
      cluster.shutdown();
    }
  }

  // RestartPoint: AfterFileOps - 8 variants
  @Test(timeout = 180000) public void testDFSFileOps_NN_Graceful() throws Exception { testDFSFileOpsWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSFileOps_NN_Crash() throws Exception { testDFSFileOpsWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSFileOps_SingleDN_Graceful() throws Exception { testDFSFileOpsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSFileOps_SingleDN_Crash() throws Exception { testDFSFileOpsWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSFileOps_AllDN_Graceful() throws Exception { testDFSFileOpsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSFileOps_AllDN_Crash() throws Exception { testDFSFileOpsWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSFileOps_NNDN_Graceful() throws Exception { testDFSFileOpsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSFileOps_NNDN_Crash() throws Exception { testDFSFileOpsWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH); }

  // RestartPoint: AfterRename - 8 variants
  @Test(timeout = 180000) public void testDFSRename_NN_Graceful() throws Exception { testDFSRenameWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSRename_NN_Crash() throws Exception { testDFSRenameWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSRename_SingleDN_Graceful() throws Exception { testDFSRenameWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSRename_SingleDN_Crash() throws Exception { testDFSRenameWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSRename_AllDN_Graceful() throws Exception { testDFSRenameWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSRename_AllDN_Crash() throws Exception { testDFSRenameWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testDFSRename_NNDN_Graceful() throws Exception { testDFSRenameWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testDFSRename_NNDN_Crash() throws Exception { testDFSRenameWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH); }
}

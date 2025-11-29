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

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestGetFileChecksum_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestGetFileChecksum_RestartInjected.class);
  private static final int BLOCKSIZE = 1024;
  private static final short REPLICATION = 3;

  private Configuration conf;
  private MiniDFSCluster cluster;
  private DistributedFileSystem dfs;

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION)
        .build();
    cluster.waitActive();
    dfs = cluster.getFileSystem();
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_NN_Graceful() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_NN_Graceful ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_NN_Crash() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_NN_Crash ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_DN_Graceful() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_DN_Graceful ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_DN_Crash() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_DN_Crash ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_AllDN_Graceful() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_AllDN_Graceful ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_AllDN_Crash() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_AllDN_Crash ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_RandomDN_Graceful() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_RandomDN_Graceful ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test(timeout = 120000)
  public void testGetFileChecksum_AfterCreate_RandomDN_Crash() throws Exception {
    LOG.info("=== testGetFileChecksum_AfterCreate_RandomDN_Crash ===");
    final Path foo = new Path("/foo");
    final int appendLength = BLOCKSIZE / 4;
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, dfs);

    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }
}

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

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_KEY;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFSOutputSummer_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestFSOutputSummer_RestartInjected.class);
  private static final long seed = 0xDEADBEEFL;
  private static final int BYTES_PER_CHECKSUM = 10;
  private static final int BLOCK_SIZE = 2*BYTES_PER_CHECKSUM;
  private static final int HALF_CHUNK_SIZE = BYTES_PER_CHECKSUM/2;
  private static final int FILE_SIZE = 2*BLOCK_SIZE-1;
  private static final short NUM_OF_DATANODES = 2;
  private final byte[] expected = new byte[FILE_SIZE];
  private final byte[] actual = new byte[FILE_SIZE];
  private MiniDFSCluster cluster;
  private FileSystem fileSys;
  private Configuration conf;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, BYTES_PER_CHECKSUM);
  }

  @After
  public void teardown() {
    if (fileSys != null) {
      try {
        fileSys.close();
      } catch (IOException e) {
      }
    }
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  private void checkAndEraseData(byte[] actual, int from, byte[] expected,
      String message) throws Exception {
    for (int idx = 0; idx < actual.length; idx++) {
      assertEquals(message+" byte "+(from+idx)+" differs. expected "+
                        expected[from+idx]+" actual "+actual[idx],
                        actual[idx], expected[from+idx]);
      actual[idx] = 0;
    }
  }

  private void checkFile(Path name) throws Exception {
    FSDataInputStream stm = fileSys.open(name);
    stm.readFully(0, actual);
    checkAndEraseData(actual, 0, expected, "Read Sanity Test");
    stm.close();
    fileSys.getFileChecksum(name);
  }

  private void cleanupFile(Path name) throws IOException {
    assertTrue(fileSys.exists(name));
    fileSys.delete(name, true);
    assertTrue(!fileSys.exists(name));
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_NN_Graceful() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_NN_Crash() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_DN_Graceful() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_DN_Crash() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_AllDN_Graceful() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_AllDN_Crash() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_RandomDN_Graceful() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }

  @Test(timeout = 120000)
  public void testFSOutputSummer_AfterClose_RandomDN_Crash() throws Exception {
    conf.set(DFSConfigKeys.DFS_CHECKSUM_TYPE_KEY, "CRC32");
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_OF_DATANODES).build();
    fileSys = cluster.getFileSystem();
    Path file = new Path("try.dat");
    Random rand = new Random(seed);
    rand.nextBytes(expected);
    FSDataOutputStream stm = fileSys.create(file, true,
               fileSys.getConf().getInt(IO_FILE_BUFFER_SIZE_KEY, 4096),
               NUM_OF_DATANODES, BLOCK_SIZE);
    stm.write(expected);
    stm.close();
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fileSys);
    checkFile(file);
    cleanupFile(file);
  }
}

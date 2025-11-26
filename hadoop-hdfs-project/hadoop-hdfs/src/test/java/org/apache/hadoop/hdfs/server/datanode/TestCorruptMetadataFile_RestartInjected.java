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
package org.apache.hadoop.hdfs.server.datanode;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.function.Supplier;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.BlockMissingException;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.LambdaTestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;
import static org.junit.Assert.assertEquals;

/**
 * Restart-injection variant of TestCorruptMetadataFile.
 * Tests that corrupt metadata file detection survives component restarts.
 *
 * Original test: testReadBlockFailsWhenMetaIsCorrupt (has hflush)
 *
 * Generated variants:
 * - AfterHflush x 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestCorruptMetadataFile_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestCorruptMetadataFile_RestartInjected.class);

  /**
   * Core test logic for reading block failures when meta is corrupt with restart injection.
   */
  private void testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    // Reduce block acquire retries as we only have 1 DN and it allows the
    // test to run faster
    conf.setInt(
        HdfsClientConfigKeys.DFS_CLIENT_MAX_BLOCK_ACQUIRE_FAILURES_KEY, 1);
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    try {
      cluster.waitActive();
      FileSystem fs = cluster.getFileSystem();
      DataNode dn0 = cluster.getDataNodes().get(0);
      Path filePath = new Path("test.dat");
      FSDataOutputStream out = fs.create(filePath, (short) 1);
      out.write(1);
      out.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      out.close();

      ExtendedBlock block = DFSTestUtil.getFirstBlock(fs, filePath);
      File metadataFile = cluster.getBlockMetadataFile(0, block);

      // First ensure we can read the file OK
      FSDataInputStream in = fs.open(filePath);
      in.readByte();
      in.close();

      // Now truncate the meta file, and ensure the data is not read OK
      RandomAccessFile raFile = new RandomAccessFile(metadataFile, "rw");
      raFile.setLength(0);

      FSDataInputStream intrunc = fs.open(filePath);
      LambdaTestUtils.intercept(BlockMissingException.class,
          () -> intrunc.readByte());
      intrunc.close();

      // Write 11 bytes to the file, but an invalid header
      raFile.write("12345678901".getBytes());
      assertEquals(11, raFile.length());

      FSDataInputStream ininvalid = fs.open(filePath);
      LambdaTestUtils.intercept(BlockMissingException.class,
          () -> ininvalid.readByte());
      ininvalid.close();

      GenericTestUtils.waitFor(new Supplier<Boolean>() {
        @Override
        public Boolean get() {
          return cluster.getNameNode().getNamesystem()
              .getBlockManager().getCorruptBlocks() == 1;
        }
      }, 100, 5000);

      raFile.close();
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  // ============================================================
  // Test variants: testReadBlockFailsWhenMetaIsCorrupt with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_NN_Graceful() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_NN_Crash() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_SingleDN_Graceful() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_SingleDN_Crash() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_AllDN_Graceful() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_AllDN_Crash() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_NNDN_Graceful() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testReadBlockFailsWhenMetaIsCorrupt_AfterHflush_NNDN_Crash() throws Exception {
    testReadBlockFailsWhenMetaIsCorruptWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

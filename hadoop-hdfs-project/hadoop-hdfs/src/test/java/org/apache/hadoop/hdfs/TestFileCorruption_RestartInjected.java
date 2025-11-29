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

import java.util.function.Supplier;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo.DatanodeInfoBuilder;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ChecksumException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongs;
import org.apache.hadoop.hdfs.protocol.BlockListAsLongs.BlockReportReplica;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.InternalDataNodeTestUtils;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.PathUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileCorruption_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestFileCorruption_RestartInjected.class);
  {
    DFSTestUtil.setNameNodeLogLevel(Level.ALL);
    GenericTestUtils.setLogLevel(DataNode.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(DFSClient.LOG, Level.ALL);
  }

  private MiniDFSCluster cluster;
  private Configuration conf;
  private FileSystem fs;

  @Before
  public void setup() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
  }

  @After
  public void teardown() {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  private static ExtendedBlock getFirstBlock(DataNode dn, String bpid) {
    Map<DatanodeStorage, BlockListAsLongs> blockReports =
        dn.getFSDataset().getBlockReports(bpid);
    for (BlockListAsLongs blockLongs : blockReports.values()) {
      for (BlockReportReplica block : blockLongs) {
        return new ExtendedBlock(bpid, block);
      }
    }
    return null;
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_NN_Graceful() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_NN_Crash() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_DN_Graceful() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_DN_Crash() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_AllDN_Graceful() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_AllDN_Crash() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_RandomDN_Graceful() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }

  @Test(timeout = 120000)
  public void testArrayOutOfBoundsException_AfterCreate_RandomDN_Crash() throws Exception {
    final Path FILE_PATH = new Path("/tmp.txt");
    final long FILE_LEN = 1L;
    DFSTestUtil.createFile(fs, FILE_PATH, FILE_LEN, (short)2, 1L);
    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);
    final String bpid = cluster.getNamesystem().getBlockPoolId();
    ExtendedBlock blk = getFirstBlock(cluster.getDataNodes().get(0), bpid);
    assertFalse("Data directory does not contain any blocks or there was an "
        + "IO error", blk==null);
    cluster.startDataNodes(conf, 1, true, null, null);
    ArrayList<DataNode> datanodes = cluster.getDataNodes();
    assertEquals(datanodes.size(), 3);
    DataNode dataNode = datanodes.get(2);
    DatanodeRegistration dnR = InternalDataNodeTestUtils.
      getDNRegistrationForBP(dataNode, blk.getBlockPoolId());
    FSNamesystem ns = cluster.getNamesystem();
    ns.writeLock();
    try {
      cluster.getNamesystem().getBlockManager().findAndMarkBlockAsCorrupt(blk,
          new DatanodeInfoBuilder().setNodeID(dnR).build(), "TEST",
          "STORAGE_ID");
    } finally {
      ns.writeUnlock();
    }
    fs.open(FILE_PATH);
    fs.delete(FILE_PATH, false);
  }
}

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

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorageReport;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;

public class TestNameNodeRpcServerMethods_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestNameNodeRpcServerMethods_RestartInjected.class);

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_NN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      nnRpc = cluster.getNameNode().getRpcServer();
      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_NN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      nnRpc = cluster.getNameNode().getRpcServer();
      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_DN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_DN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_AllDN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_AllDN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_RandomDN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  @Test(timeout = 120000)
  public void testGetDatanodeStorageReportWithNumBLocksNotZero_AfterClose_RandomDN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).build();
    try {
      cluster.waitActive();
      NamenodeProtocols nnRpc = cluster.getNameNode().getRpcServer();
      int buffSize = 1024;
      long blockSize = 1024 * 1024;
      String file = "/testFile";
      DistributedFileSystem dfs = cluster.getFileSystem();
      FSDataOutputStream outputStream = dfs.create(
          new Path(file), true, buffSize, (short)1, blockSize);
      byte[] outBuffer = new byte[buffSize];
      for (int i = 0; i < buffSize; i++) {
        outBuffer[i] = (byte) (i & 0x00ff);
      }
      outputStream.write(outBuffer);
      outputStream.close();

      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, dfs);

      int numBlocks = 0;
      DatanodeStorageReport[] reports
          = nnRpc.getDatanodeStorageReport(HdfsConstants.DatanodeReportType.ALL);
      for (DatanodeStorageReport r : reports) {
        numBlocks += r.getDatanodeInfo().getNumBlocks();
      }
      assertEquals(1, numBlocks);
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}

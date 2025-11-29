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
package org.apache.hadoop.hdfs.server.namenode.ha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

/**
 * HA test - only DN restart targets.
 */
public class TestFailureOfSharedDir_RestartInjected {
  private static final Logger LOG = LoggerFactory.getLogger(TestFailureOfSharedDir_RestartInjected.class);
  private static final short REPLICATION = 3;
  private static final int BLOCKSIZE = 1024;

  private void testFailureOfSharedDirWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
    try {
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      Path filePath = new Path("/testFailureOfSharedDir.dat");
      DFSTestUtil.createFile(fs, filePath, BLOCKSIZE * 2, REPLICATION, 0);

      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(cluster, target, mode, true);
      verifyClusterHealth(cluster, fs);
      LOG.info("=== RESTART COMPLETE ===");

      assertTrue("File should exist", fs.exists(filePath));
      assertEquals("File should have correct length", BLOCKSIZE * 2, fs.getFileStatus(filePath).getLen());
    } finally {
      cluster.shutdown();
    }
  }

  // HA test - only DN restart targets
  @Test(timeout = 180000) public void testFailureOfSharedDir_SingleDN_Graceful() throws Exception { testFailureOfSharedDirWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testFailureOfSharedDir_SingleDN_Crash() throws Exception { testFailureOfSharedDirWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH); }
  @Test(timeout = 180000) public void testFailureOfSharedDir_AllDN_Graceful() throws Exception { testFailureOfSharedDirWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL); }
  @Test(timeout = 180000) public void testFailureOfSharedDir_AllDN_Crash() throws Exception { testFailureOfSharedDirWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH); }
}

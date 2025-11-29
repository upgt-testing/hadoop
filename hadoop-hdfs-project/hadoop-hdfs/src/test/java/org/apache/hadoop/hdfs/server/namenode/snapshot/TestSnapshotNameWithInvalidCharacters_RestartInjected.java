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

package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.ipc.RemoteException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSnapshotNameWithInvalidCharacters_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestSnapshotNameWithInvalidCharacters_RestartInjected.class);

  private static final long SEED = 0;
  private static final short REPLICATION = 1;
  private static final int BLOCKSIZE = 1024;

  private Configuration conf;
  private MiniDFSCluster cluster;
  private DistributedFileSystem hdfs;

  private final Path dir1 = new Path("/");
  private final String file1Name = "file1";
  private final String snapshot1 = "a:b:c";
  private final String snapshot2 = "a/b/c";

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION)
                                              .build();
    cluster.waitActive();
    hdfs = cluster.getFileSystem();
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_NN_Graceful ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_NN_Crash ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_DN_Graceful ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_DN_Crash ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_AllDN_Graceful ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_AllDN_Crash ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_RandomDN_Graceful ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_RandomDN_Crash ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_NNAndDN_Graceful ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName_AfterAllowSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName_AfterAllowSnapshot_NNAndDN_Crash ===");

    Path file1 = new Path(dir1,file1Name);
    DFSTestUtil.createFile(hdfs,file1, BLOCKSIZE,REPLICATION,SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot1);
    } catch (RemoteException e) {
    }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_NN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_NN_Graceful ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_NN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_NN_Crash ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_DN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_DN_Graceful ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_DN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_DN_Crash ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_AllDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_AllDN_Graceful ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_AllDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_AllDN_Crash ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_RandomDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_RandomDN_Graceful ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_RandomDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_RandomDN_Crash ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_NNAndDN_Graceful() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_NNAndDN_Graceful ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }

  @Test(timeout = 120000)
  public void TestSnapshotWithInvalidName1_AfterAllowSnapshot_NNAndDN_Crash() throws Exception {
    LOG.info("=== Starting TestSnapshotWithInvalidName1_AfterAllowSnapshot_NNAndDN_Crash ===");

    Path file1 = new Path(dir1, file1Name);
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);

    hdfs.allowSnapshot(dir1);

    executeRestart(cluster, RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    try {
        hdfs.createSnapshot(dir1, snapshot2);
    } catch (RemoteException e) {
   }
  }
}

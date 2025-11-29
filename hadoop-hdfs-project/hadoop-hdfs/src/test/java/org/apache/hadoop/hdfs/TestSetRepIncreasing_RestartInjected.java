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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.server.datanode.SimulatedFSDataset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSetRepIncreasing_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestSetRepIncreasing_RestartInjected.class);

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_NN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_NN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_DN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_DN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_AllDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_AllDN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_RandomDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterCreate_RandomDN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_NN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_NN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_DN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_DN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_AllDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_AllDN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_RandomDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing_AfterSetrep_RandomDN_Crash() throws Exception {
    setrepWithRestart(3, 7, false, RestartPoint.AFTER_SETREP, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_NN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_NN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_DN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_DN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_AllDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_AllDN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_RandomDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterCreate_RandomDN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_NN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_NN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_DN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_DN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_AllDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_AllDN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_RandomDN_Graceful() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage_AfterSetrep_RandomDN_Crash() throws Exception {
    setrepWithRestart(3, 7, true, RestartPoint.AFTER_SETREP, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_NN_Graceful() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_NN_Crash() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_DN_Graceful() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_DN_Crash() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_AllDN_Graceful() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_AllDN_Crash() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_RandomDN_Graceful() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterCreate_RandomDN_Crash() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_CREATE, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterSetReplication_NN_Graceful() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_SETREPLICATION, RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout=120000)
  public void testSetRepWithStoragePolicyOnEmptyFile_AfterSetReplication_NN_Crash() throws Exception {
    testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint.AFTER_SETREPLICATION, RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  static void setrepWithRestart(int fromREP, int toREP, boolean simulatedStorage,
      RestartPoint restartPoint, RestartTarget restartTarget, RestartMode restartMode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    if (simulatedStorage) {
      SimulatedFSDataset.setFactory(conf);
    }
    conf.set(DFSConfigKeys.DFS_REPLICATION_KEY, "" + fromREP);
    conf.setLong(DFSConfigKeys.DFS_BLOCKREPORT_INTERVAL_MSEC_KEY, 1000L);
    conf.set(DFSConfigKeys.DFS_NAMENODE_RECONSTRUCTION_PENDING_TIMEOUT_SEC_KEY, Integer.toString(2));
    MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(10).build();
    FileSystem fs = cluster.getFileSystem();
    assertTrue("Not a HDFS: "+fs.getUri(), fs instanceof DistributedFileSystem);

    try {
      Path root = TestDFSShell.mkdir(fs,
          new Path("/test/setrep" + fromREP + "-" + toREP));
      Path f = TestDFSShell.writeFile(fs, new Path(root, "foo"));

      if (restartPoint == RestartPoint.AFTER_CREATE) {
        executeRestart(cluster, restartTarget, restartMode, true);
        verifyClusterHealth(cluster, fs);
      }

      {
        String[] args = {"-setrep", "-w", "" + toREP, "" + f};
        FsShell shell = new FsShell();
        shell.setConf(conf);
        try {
          assertEquals(0, shell.run(args));
        } catch (Exception e) {
          assertTrue("-setrep " + e, false);
        }
      }

      if (restartPoint == RestartPoint.AFTER_SETREP) {
        executeRestart(cluster, restartTarget, restartMode, true);
        verifyClusterHealth(cluster, fs);
      }

      fs = cluster.getFileSystem();
      FileStatus file = fs.getFileStatus(f);
      long len = file.getLen();
      for(BlockLocation locations : fs.getFileBlockLocations(file, 0, len)) {
        assertTrue(locations.getHosts().length == toREP);
      }
      TestDFSShell.show("done setrep waiting: " + root);
    } finally {
      try {fs.close();} catch (Exception e) {}
      cluster.shutdown();
    }
  }

  static void testSetRepWithStoragePolicyOnEmptyFileWithRestart(RestartPoint restartPoint,
      RestartTarget restartTarget, RestartMode restartMode) throws Exception {
    Configuration conf = new HdfsConfiguration();
    MiniDFSCluster cluster =
        new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    DistributedFileSystem dfs = cluster.getFileSystem();
    try {
      Path d = new Path("/tmp");
      dfs.mkdirs(d);
      dfs.setStoragePolicy(d, "HOT");
      Path f = new Path(d, "foo");
      dfs.createNewFile(f);

      if (restartPoint == RestartPoint.AFTER_CREATE) {
        executeRestart(cluster, restartTarget, restartMode, true);
        verifyClusterHealth(cluster, dfs);
      }

      dfs.setReplication(f, (short) 4);

      if (restartPoint == RestartPoint.AFTER_SETREPLICATION) {
        executeRestart(cluster, restartTarget, restartMode, true);
        verifyClusterHealth(cluster, dfs);
      }
    } finally {
      dfs.close();
      cluster.shutdown();
    }
  }

  enum RestartPoint {
    AFTER_CREATE,
    AFTER_SETREP,
    AFTER_SETREPLICATION
  }
}

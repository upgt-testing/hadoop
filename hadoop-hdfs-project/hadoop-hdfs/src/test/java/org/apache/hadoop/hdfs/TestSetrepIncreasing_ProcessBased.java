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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.server.datanode.SimulatedFSDataset;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSetrepIncreasing}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests replication factor changes via FsShell and FileSystem APIs.
 *
 * @see TestSetrepIncreasing Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSetrepIncreasing_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_SET_REPLICATION
    );
  }
  void setrep(int fromREP, int toREP, boolean simulatedStorage) throws Exception {
    conf = new HdfsConfiguration();
    if (simulatedStorage) {
      SimulatedFSDataset.setFactory(conf);
    }
    conf.set(DFSConfigKeys.DFS_REPLICATION_KEY, "" + fromREP);
    conf.setLong(DFSConfigKeys.DFS_BLOCKREPORT_INTERVAL_MSEC_KEY, 1000L);
    conf.set(DFSConfigKeys.DFS_NAMENODE_RECONSTRUCTION_PENDING_TIMEOUT_SEC_KEY, Integer.toString(2));
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(10).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    assertTrue("Not a HDFS: "+fs.getUri(), fs instanceof DistributedFileSystem);

    Path root = TestDFSShell.mkdir(fs,
        new Path("/test/setrep" + fromREP + "-" + toREP));
    Path f = TestDFSShell.writeFile(fs, new Path(root, "foo"));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // Verify setrep for changing replication
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
    checkpoint(UpgradeCheckpoints.AFTER_SET_REPLICATION);

    //get fs again since the old one may be closed
    fs = cluster.getFileSystem();
    FileStatus file = fs.getFileStatus(f);
    long len = file.getLen();
    for(BlockLocation locations : fs.getFileBlockLocations(file, 0, len)) {
      assertTrue(locations.getHosts().length == toREP);
    }
    TestDFSShell.show("done setrep waiting: " + root);
  }

  @Test(timeout=120000)
  public void testSetrepIncreasing() throws Exception {
    setrep(3, 7, false);
  }
  @Test(timeout=120000)
  public void testSetrepIncreasingSimulatedStorage() throws Exception {
    setrep(3, 7, true);
  }

  @Test
  public void testSetRepWithStoragePolicyOnEmptyFile() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(1).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    Path d = new Path("/tmp");
    fs.mkdirs(d);
    fs.setStoragePolicy(d, "HOT");
    Path f = new Path(d, "foo");
    fs.createNewFile(f);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    fs.setReplication(f, (short) 4);
    checkpoint(UpgradeCheckpoints.AFTER_SET_REPLICATION);
 }

  @Test
  public void testSetRepOnECFile() throws Exception {
    ClientProtocol client;
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(1)
        .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    client = NameNodeProxies.createProxy(conf,
        cluster.getFileSystem().getUri(), ClientProtocol.class).getProxy();
    client.enableErasureCodingPolicy(
        StripedFileTestUtil.getDefaultECPolicy().getName());
    client.setErasureCodingPolicy("/",
        StripedFileTestUtil.getDefaultECPolicy().getName());

    fs = cluster.getFileSystem();
    Path d = new Path("/tmp");
    fs.mkdirs(d);
    Path f = new Path(d, "foo");
    fs.createNewFile(f);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    FileStatus file = fs.getFileStatus(f);
    assertTrue(file.isErasureCoded());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));
    String[] args = {"-setrep", "2", "" + f};
    FsShell shell = new FsShell();
    shell.setConf(conf);
    assertEquals(0, shell.run(args));
    checkpoint(UpgradeCheckpoints.AFTER_SET_REPLICATION);

    assertTrue(
        out.toString().contains("Did not set replication for: /tmp/foo"));

    // verify the replication factor of the EC file
    file = fs.getFileStatus(f);
    assertEquals(1, file.getReplication());
  }
}

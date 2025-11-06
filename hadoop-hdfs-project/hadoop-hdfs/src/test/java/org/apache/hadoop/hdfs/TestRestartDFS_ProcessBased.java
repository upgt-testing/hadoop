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

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestRestartDFS}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * A JUnit test for checking if restarting DFS preserves integrity.
 *
 * @see TestRestartDFS Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestRestartDFS_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_DATA_SETUP,
      "AFTER_METADATA_CHANGE",
      UpgradeCheckpoints.AFTER_NAMENODE_RESTART
    );
  }

  public void runTests(boolean serviceTest) throws Exception {
    DFSTestUtil files = new DFSTestUtil.Builder().setName("TestRestartDFS").
        setNumFiles(20).build();

    final String dir = "/srcdat";
    final Path rootpath = new Path("/");
    final Path dirpath = new Path(dir);

    long rootmtime;
    FileStatus rootstatus;
    FileStatus dirstatus;

    conf = new HdfsConfiguration();
    if (serviceTest) {
      conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
               "localhost:0");
    }
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(4).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    files.createFiles(fs, dir);
    checkpoint(UpgradeCheckpoints.AFTER_DATA_SETUP);

    rootmtime = fs.getFileStatus(rootpath).getModificationTime();
    rootstatus = fs.getFileStatus(dirpath);
    dirstatus = fs.getFileStatus(dirpath);

    fs.setOwner(rootpath, rootstatus.getOwner() + "_XXX", null);
    fs.setOwner(dirpath, null, dirstatus.getGroup() + "_XXX");
    checkpoint("AFTER_METADATA_CHANGE");

    // shutdown cluster and restart without formatting
    cluster.shutdown();
    if (serviceTest) {
      conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
               "localhost:0");
    }
    // Here we restart the MiniDFScluster without formatting namenode
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(4).format(false).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_NAMENODE_RESTART);

    fs = cluster.getFileSystem();
    assertTrue("Filesystem corrupted after restart.",
               files.checkFiles(fs, dir));

    final FileStatus newrootstatus = fs.getFileStatus(rootpath);
    assertEquals(rootmtime, newrootstatus.getModificationTime());
    assertEquals(rootstatus.getOwner() + "_XXX", newrootstatus.getOwner());
    assertEquals(rootstatus.getGroup(), newrootstatus.getGroup());

    final FileStatus newdirstatus = fs.getFileStatus(dirpath);
    assertEquals(dirstatus.getOwner(), newdirstatus.getOwner());
    assertEquals(dirstatus.getGroup() + "_XXX", newdirstatus.getGroup());
    rootmtime = fs.getFileStatus(rootpath).getModificationTime();

    // shutdown and do second restart
    cluster.shutdown();
    if (serviceTest) {
      conf.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
               "localhost:0");
    }
    // This is a second restart to check that after the first restart
    // the image written in parallel to both places did not get corrupted
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(4).format(false).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    assertTrue("Filesystem corrupted after restart.",
               files.checkFiles(fs, dir));

    final FileStatus secondrootstatus = fs.getFileStatus(rootpath);
    assertEquals(rootmtime, secondrootstatus.getModificationTime());
    assertEquals(rootstatus.getOwner() + "_XXX", secondrootstatus.getOwner());
    assertEquals(rootstatus.getGroup(), secondrootstatus.getGroup());

    final FileStatus seconddirstatus = fs.getFileStatus(dirpath);
    assertEquals(dirstatus.getOwner(), seconddirstatus.getOwner());
    assertEquals(dirstatus.getGroup() + "_XXX", seconddirstatus.getGroup());

    files.cleanup(fs, dir);
  }

  /** check if DFS remains in proper condition after a restart */
  @Test
  public void testRestartDFS() throws Exception {
    runTests(false);
  }

  /** check if DFS remains in proper condition after a restart
   * this rerun is with 2 ports enabled for RPC in the namenode
   */
  @Test
   public void testRestartDualPortDFS() throws Exception {
     runTests(true);
   }
}

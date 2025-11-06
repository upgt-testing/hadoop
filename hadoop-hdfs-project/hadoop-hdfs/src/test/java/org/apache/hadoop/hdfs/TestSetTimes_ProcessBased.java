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
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.util.Time;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSetTimes}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class tests modification time and access time operations on files.
 *
 * Note: testGetBlockLocationsOnlyUsesReadLock was excluded from this
 * transformation because it requires NameNodeAdapter and direct FSNamesystem
 * access to test internal locking behavior.
 *
 * @see TestSetTimes Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSetTimes_ProcessBased extends ProcessBasedUpgradeTestBase {
  static final long seed = 0xDEADBEEFL;
  static final int blockSize = 8192;
  static final int fileSize = 16384;
  static final int numDatanodes = 1;

  static final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd HH:mm");

  Random myrand = new Random();
  Path hostsFile;
  Path excludeFile;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_SET_TIMES",
      UpgradeCheckpoints.AFTER_NAMENODE_RESTART
    );
  }

  private FSDataOutputStream writeFile(FileSystem fileSys, Path name, int repl)
    throws IOException {
    FSDataOutputStream stm = fileSys.create(name, true, fileSys.getConf()
        .getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096),
        (short) repl, blockSize);
    byte[] buffer = new byte[fileSize];
    Random rand = new Random(seed);
    rand.nextBytes(buffer);
    stm.write(buffer);
    return stm;
  }

  private void cleanupFile(FileSystem fileSys, Path name) throws IOException {
    assertTrue(fileSys.exists(name));
    fileSys.delete(name, true);
    assertTrue(!fileSys.exists(name));
  }

  private void printDatanodeReport(DatanodeInfo[] info) {
    System.out.println("-------------------------------------------------");
    for (int i = 0; i < info.length; i++) {
      System.out.println(info[i].getDatanodeReport());
      System.out.println();
    }
  }

  /**
   * Tests mod & access time in DFS.
   */
  @Test
  public void testTimes() throws Exception {
    conf = new HdfsConfiguration();
    final int MAX_IDLE_TIME = 2000; // 2s
    conf.setInt("ipc.client.connection.maxidletime", MAX_IDLE_TIME);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);


    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
                                               .numDataNodes(numDatanodes)
                                               .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress addr = cluster.getNameNodeRpcAddress();
    DFSClient client = new DFSClient(addr, conf);
    DatanodeInfo[] info = client.datanodeReport(DatanodeReportType.LIVE);
    assertEquals("Number of Datanodes ", numDatanodes, info.length);
    fs = cluster.getFileSystem();
    int replicas = 1;
    assertTrue(fs instanceof DistributedFileSystem);

    try {
      //
      // create file and record atime/mtime
      //
      System.out.println("Creating testdir1 and testdir1/test1.dat.");
      Path dir1 = new Path("testdir1");
      Path file1 = new Path(dir1, "test1.dat");
      FSDataOutputStream stm = writeFile(fs, file1, replicas);
      FileStatus stat = fs.getFileStatus(file1);
      long atimeBeforeClose = stat.getAccessTime();
      String adate = dateForm.format(new Date(atimeBeforeClose));
      System.out.println("atime on " + file1 + " before close is " +
                         adate + " (" + atimeBeforeClose + ")");
      assertTrue(atimeBeforeClose != 0);
      stm.close();
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      stat = fs.getFileStatus(file1);
      long atime1 = stat.getAccessTime();
      long mtime1 = stat.getModificationTime();
      adate = dateForm.format(new Date(atime1));
      String mdate = dateForm.format(new Date(mtime1));
      System.out.println("atime on " + file1 + " is " + adate +
                         " (" + atime1 + ")");
      System.out.println("mtime on " + file1 + " is " + mdate +
                         " (" + mtime1 + ")");
      assertTrue(atime1 != 0);

      // check setting negative value for atime and mtime.
      fs.setTimes(file1, -2, -2);
      // The values shouldn't change.
      stat = fs.getFileStatus(file1);
      assertEquals(mtime1, stat.getModificationTime());
      assertEquals(atime1, stat.getAccessTime());

      //
      // record dir times
      //
      stat = fs.getFileStatus(dir1);
      long mdir1 = stat.getAccessTime();
      assertTrue(mdir1 == 0);

      // set the access time to be one day in the past
      long atime2 = atime1 - (24L * 3600L * 1000L);
      fs.setTimes(file1, -1, atime2);

      // check new access time on file
      stat = fs.getFileStatus(file1);
      long atime3 = stat.getAccessTime();
      String adate3 = dateForm.format(new Date(atime3));
      System.out.println("new atime on " + file1 + " is " +
                         adate3 + " (" + atime3 + ")");
      assertTrue(atime2 == atime3);
      assertTrue(mtime1 == stat.getModificationTime());

      // set the modification time to be 1 hour in the past
      long mtime2 = mtime1 - (3600L * 1000L);
      fs.setTimes(file1, mtime2, -1);

      // check new modification time on file
      stat = fs.getFileStatus(file1);
      long mtime3 = stat.getModificationTime();
      String mdate3 = dateForm.format(new Date(mtime3));
      System.out.println("new mtime on " + file1 + " is " +
                         mdate3 + " (" + mtime3 + ")");
      assertTrue(atime2 == stat.getAccessTime());
      assertTrue(mtime2 == mtime3);

      long mtime4 = Time.now() - (3600L * 1000L);
      long atime4 = Time.now();
      fs.setTimes(dir1, mtime4, atime4);
      // check new modification time on file
      stat = fs.getFileStatus(dir1);
      assertTrue("Not matching the modification times", mtime4 == stat
          .getModificationTime());
      assertTrue("Not matching the access times", atime4 == stat
          .getAccessTime());

      Path nonExistingDir = new Path(dir1, "/nonExistingDir/");
      try {
        fs.setTimes(nonExistingDir, mtime4, atime4);
        fail("Expecting FileNotFoundException");
      } catch (FileNotFoundException e) {
        assertTrue(e.getMessage().contains(
            "File/Directory " + nonExistingDir.toString() + " does not exist."));
      }
      checkpoint("AFTER_SET_TIMES");

      // shutdown cluster and restart
      cluster.shutdown();
      try {Thread.sleep(2*MAX_IDLE_TIME);} catch (InterruptedException e) {}
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
                                                .format(false)
                                                .build();
      cluster.waitClusterUp();
      checkpoint(UpgradeCheckpoints.AFTER_NAMENODE_RESTART);

      fs = cluster.getFileSystem();

      // verify that access times and modification times persist after a
      // cluster restart.
      System.out.println("Verifying times after cluster restart");
      stat = fs.getFileStatus(file1);
      assertTrue(atime2 == stat.getAccessTime());
      assertTrue(mtime3 == stat.getModificationTime());

      cleanupFile(fs, file1);
      cleanupFile(fs, dir1);
    } catch (IOException e) {
      info = client.datanodeReport(DatanodeReportType.ALL);
      printDatanodeReport(info);
      throw e;
    }
  }

  /**
   * Tests mod time change at close in DFS.
   */
  @Test
  public void testTimesAtClose() throws Exception {
    conf = new HdfsConfiguration();
    final int MAX_IDLE_TIME = 2000; // 2s
    int replicas = 1;

    // parameter initialization
    conf.setInt("ipc.client.connection.maxidletime", MAX_IDLE_TIME);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);
    conf.setInt(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_DATANODE_HANDLER_COUNT_KEY, 50);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
                                               .numDataNodes(numDatanodes)
                                               .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    InetSocketAddress addr = cluster.getNameNodeRpcAddress();
    DFSClient client = new DFSClient(addr, conf);
    DatanodeInfo[] info = client.datanodeReport(DatanodeReportType.LIVE);
    assertEquals("Number of Datanodes ", numDatanodes, info.length);
    fs = cluster.getFileSystem();
    assertTrue(fs instanceof DistributedFileSystem);

    try {
      // create a new file and write to it
      Path file1 = new Path("/simple.dat");
      FSDataOutputStream stm = writeFile(fs, file1, replicas);
      System.out.println("Created and wrote file simple.dat");
      FileStatus statBeforeClose = fs.getFileStatus(file1);
      long mtimeBeforeClose = statBeforeClose.getModificationTime();
      String mdateBeforeClose = dateForm.format(new Date(
                                                     mtimeBeforeClose));
      System.out.println("mtime on " + file1 + " before close is "
                  + mdateBeforeClose + " (" + mtimeBeforeClose + ")");
      assertTrue(mtimeBeforeClose != 0);

      //close file after writing
      stm.close();
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      System.out.println("Closed file.");
      FileStatus statAfterClose = fs.getFileStatus(file1);
      long mtimeAfterClose = statAfterClose.getModificationTime();
      String mdateAfterClose = dateForm.format(new Date(mtimeAfterClose));
      System.out.println("mtime on " + file1 + " after close is "
                  + mdateAfterClose + " (" + mtimeAfterClose + ")");
      assertTrue(mtimeAfterClose != 0);
      assertTrue(mtimeBeforeClose != mtimeAfterClose);

      cleanupFile(fs, file1);
    } catch (IOException e) {
      info = client.datanodeReport(DatanodeReportType.ALL);
      printDatanodeReport(info);
      throw e;
    }
  }

  // testGetBlockLocationsOnlyUsesReadLock excluded - requires NameNodeAdapter
  // and direct FSNamesystem access to test internal locking behavior

  /**
   * Test whether atime can be set explicitly even when the atime support is
   * disabled.
   */
  @Test
  public void testAtimeUpdate() throws Exception {
    conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_ACCESSTIME_PRECISION_KEY, 0);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(0)
        .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();

    // Create an empty file
    Path p = new Path("/testAtimeUpdate");
    DFSTestUtil.createFile(cluster.getFileSystem(), p, 0, (short)1, 0L);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    fs.setTimes(p, -1L, 123456L);
    checkpoint("AFTER_SET_TIMES");

    Assert.assertEquals(123456L, fs.getFileStatus(p).getAccessTime());
  }

  public static void main(String[] args) throws Exception {
    new TestSetTimes_ProcessBased().testTimes();
  }
}

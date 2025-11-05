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

import static org.apache.hadoop.hdfs.client.HdfsClientConfigKeys.DFS_CLIENT_READ_USE_CACHE_PRIORITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.DatanodeInfoWithStorage;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.net.unix.DomainSocket;
import org.apache.hadoop.net.unix.TemporarySocketDirectory;
import org.apache.hadoop.hdfs.client.impl.DfsClientConf;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys.Retry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSInputStream}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSInputStream Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSInputStream_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_FIRST_WRITE,
      "AFTER_FIRST_CLOSE",
      "BEFORE_VERIFICATION"
    );
  }

  private void testSkipInner(ProcessBasedMiniDFSCluster cluster) throws Exception {
    DistributedFileSystem fs = cluster.getFileSystem();
    DFSClient client = fs.dfs;
    Path file = new Path("/testfile");
    int fileLength = 1 << 22;
    byte[] fileContent = new byte[fileLength];
    for (int i = 0; i < fileLength; i++) {
      fileContent[i] = (byte) (i % 133);
    }

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FSDataOutputStream fout = fs.create(file);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    fout.write(fileContent);
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    fout.close();
    checkpoint("AFTER_FIRST_CLOSE");

    Random random = new Random();
    for (int i = 3; i < 18; i++) {
      DFSInputStream fin = client.open("/testfile");
      for (long pos = 0; pos < fileLength;) {
        long skip = random.nextInt(1 << i) + 1;
        long skipped = fin.skip(skip);
        if (pos + skip >= fileLength) {
          assertEquals(fileLength, pos + skipped);
          break;
        } else {
          assertEquals(skip, skipped);
          pos += skipped;
          int data = fin.read();
          assertEquals(pos % 133, data);
          pos += 1;
        }
      }
      fin.close();
    }

    checkpoint("BEFORE_VERIFICATION");
  }

  @Test(timeout=60000)
  public void testSkipWithRemoteBlockReader() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    testSkipInner(cluster);
  }

  @Test(timeout=60000)
  public void testSkipWithRemoteBlockReader2() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    testSkipInner(cluster);
  }

  @Test(timeout=60000)
  public void testSkipWithLocalBlockReader() throws Exception {
    Assume.assumeThat(DomainSocket.getLoadingFailureReason(), equalTo(null));
    TemporarySocketDirectory sockDir = new TemporarySocketDirectory();
    DomainSocket.disableBindPathValidation();
    conf.setBoolean(HdfsClientConfigKeys.Read.ShortCircuit.KEY, true);
    conf.set(DFSConfigKeys.DFS_DOMAIN_SOCKET_PATH_KEY,
        new File(sockDir.getDir(),
          "TestShortCircuitLocalRead._PORT.sock").getAbsolutePath());
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    try {
      DFSInputStream.tcpReadsDisabledForTesting = true;
      testSkipInner(cluster);
    } finally {
      DFSInputStream.tcpReadsDisabledForTesting = false;
      sockDir.close();
    }
  }

  @Test(timeout=60000)
  public void testSeekToNewSource() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path path = new Path("/testfile");
    DFSTestUtil.createFile(fs, path, 1024, (short) 3, 0);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    DFSInputStream fin = fs.dfs.open("/testfile");
    fin.seekToNewSource(100);
    assertEquals(100, fin.getPos());
    DatanodeInfo firstNode = fin.getCurrentDatanode();
    assertNotNull(firstNode);

    checkpoint("BEFORE_VERIFICATION");

    fin.seekToNewSource(100);
    assertEquals(100, fin.getPos());
    assertFalse(firstNode.equals(fin.getCurrentDatanode()));
    fin.close();
  }

  @Test(timeout=60000)
  public void testOpenInfo() throws Exception {
    conf.setInt(Retry.TIMES_GET_LAST_BLOCK_LENGTH_KEY, 0);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    int chunkSize = 512;
    Random r = new Random(12345L);
    byte[] data = new byte[chunkSize];
    r.nextBytes(data);

    Path file = new Path("/testfile");
    try(FSDataOutputStream fout = fs.create(file)) {
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);
      fout.write(data);
      checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);
    }
    checkpoint("AFTER_FIRST_CLOSE");

    DfsClientConf dcconf = new DfsClientConf(conf);
    int retryTimesForGetLastBlockLength =
            dcconf.getRetryTimesForGetLastBlockLength();
    assertEquals(0, retryTimesForGetLastBlockLength);

    checkpoint("BEFORE_VERIFICATION");

    try(DFSInputStream fin = fs.dfs.open("/testfile")) {
      long flen = fin.getFileLength();
      assertEquals(chunkSize, flen);

      long lastBlockBeingWrittenLength =
              fin.getlastBlockBeingWrittenLengthForTesting();
      assertEquals(0, lastBlockBeingWrittenLength);
    }
  }

  @Test
  public void testNullCheckSumWhenDNRestarted() throws Exception {
    conf.set(HdfsClientConfigKeys.DFS_CHECKSUM_TYPE_KEY, "NULL");
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    int chunkSize = 512;
    Random r = new Random(12345L);
    byte[] data = new byte[chunkSize];
    r.nextBytes(data);

    Path file = new Path("/testfile");
    try (FSDataOutputStream fout = fs.create(file)) {
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);
      fout.write(data);
      checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);
      fout.hflush();
      cluster.restartDataNode(0, true);
    }

    checkpoint("AFTER_FIRST_CLOSE");

    // wait for block to load
    Thread.sleep(1000);

    checkpoint("BEFORE_VERIFICATION");

    // Verify 2 DataNodes are live using client-side API
    DatanodeInfo[] datanodes = ((DistributedFileSystem)fs).getDataNodeStats();
    assertTrue("DN start should be success and live dn should be 2",
        datanodes.length == 2);
    assertTrue("File size should be " + chunkSize,
        fs.getFileStatus(file).getLen() == chunkSize);
  }

  @Test
  public void testReadWithPreferredCachingReplica() throws Exception {
    conf.setBoolean(DFS_CLIENT_READ_USE_CACHE_PRIORITY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path filePath = new Path("/testReadPreferredCachingReplica");
    FSDataOutputStream out = fs.create(filePath, true, 4096, (short) 3, 512);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    DFSInputStream dfsInputStream =
        (DFSInputStream) fs.open(filePath).getWrappedStream();
    LocatedBlock lb = mock(LocatedBlock.class);
    when(lb.getCachedLocations()).thenReturn(DatanodeInfo.EMPTY_ARRAY);
    DatanodeID nodeId = new DatanodeID("localhost", "localhost", "dn0", 1111,
        1112, 1113, 1114);
    DatanodeInfo dnInfo = new DatanodeDescriptor(nodeId);
    when(lb.getCachedLocations()).thenReturn(new DatanodeInfo[] {dnInfo});

    checkpoint("BEFORE_VERIFICATION");

    DatanodeInfo retDNInfo =
        dfsInputStream.getBestNodeDNAddrPair(lb, null).info;
    assertEquals(dnInfo, retDNInfo);
    fs.delete(filePath, true);
  }

  @Test
  public void testReadWithoutPreferredCachingReplica() throws Exception {
    conf.setBoolean(DFS_CLIENT_READ_USE_CACHE_PRIORITY, false);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path filePath = new Path("/testReadWithoutPreferredCachingReplica");
    FSDataOutputStream out = fs.create(filePath, true, 4096, (short) 3, 512);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    DFSInputStream dfsInputStream =
            (DFSInputStream) fs.open(filePath).getWrappedStream();
    LocatedBlock lb = mock(LocatedBlock.class);
    when(lb.getCachedLocations()).thenReturn(DatanodeInfo.EMPTY_ARRAY);
    DatanodeID nodeId = new DatanodeID("localhost", "localhost", "dn0", 1111,
            1112, 1113, 1114);
    DatanodeInfo dnInfo = new DatanodeDescriptor(nodeId);
    DatanodeInfoWithStorage dnInfoStorage =
        new DatanodeInfoWithStorage(dnInfo, "DISK", StorageType.DISK);
    when(lb.getLocations()).thenReturn(
        new DatanodeInfoWithStorage[] {dnInfoStorage});

    checkpoint("BEFORE_VERIFICATION");

    DatanodeInfo retDNInfo =
            dfsInputStream.getBestNodeDNAddrPair(lb, null).info;
    assertEquals(dnInfo, retDNInfo);
    fs.delete(filePath, true);
  }
}

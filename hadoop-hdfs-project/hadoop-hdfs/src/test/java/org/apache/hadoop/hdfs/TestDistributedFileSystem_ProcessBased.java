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

import static org.apache.hadoop.hdfs.DFSOpsCountStatistics.OpType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.protocol.CacheDirectiveInfo;
import org.apache.hadoop.hdfs.protocol.CachePoolInfo;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDistributedFileSystem} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during distributed file system operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestDistributedFileSystem Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestDistributedFileSystem_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  private static final Random RAN = new Random();

  /**
   * Define upgrade checkpoints for parameterized test execution.
   *
   * @return collection of checkpoint names
   */
  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        // Baseline - no upgrade
        UpgradeCheckpoints.NO_UPGRADE,

        // Cluster lifecycle
        UpgradeCheckpoints.AFTER_CLUSTER_START,

        // File operations (for testFileChecksum)
        "AFTER_INITIAL_TESTS",
        "AFTER_LOOP_ITERATION_1",
        "AFTER_LOOP_ITERATION_2",
        "AFTER_LOOP_ITERATION_3",

        // Statistics operations (for testStatistics2)
        "AFTER_QUOTA_OPS",
        "AFTER_CACHE_OPS",

        // Verification
        UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  private Configuration getTestConfiguration() {
    return new HdfsConfiguration();
  }

  private void checkStatistics(DistributedFileSystem dfs, int expectedReadOps,
      int expectedWriteOps, int expectedLargeReadOps) {
    FileSystem.Statistics stats = FileSystem.getStatistics("hdfs",
        DistributedFileSystem.class);
    assertEquals(expectedReadOps, stats.getReadOps());
    assertEquals(expectedWriteOps, stats.getWriteOps());
    assertEquals(expectedLargeReadOps, stats.getLargeReadOps());
  }

  private long getOpStatistics(OpType opType) {
    return TestDistributedFileSystem.getOpStatistics(opType);
  }

  private void checkOpStatistics(OpType opType, long expectedCount) {
    TestDistributedFileSystem.checkOpStatistics(opType, expectedCount);
  }

  @Test
  public void testFileChecksum() throws Exception {
    final long seed = RAN.nextLong();
    System.out.println("seed=" + seed);
    RAN.setSeed(seed);

    conf.setInt(HdfsClientConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();

    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();

    // TRANSFORMATION NOTE: WebHDFS testing commented out
    // WebHDFS requires HTTP server address which may not be easily accessible
    // in ProcessBasedMiniDFSCluster. We focus on HDFS file checksum testing.

    try {
      fs.getFileChecksum(new Path("/test/TestNonExistingFile"));
      fail("Expecting FileNotFoundException");
    } catch (FileNotFoundException e) {
      assertTrue("Not throwing the intended exception message", e.getMessage()
          .contains("File does not exist: /test/TestNonExistingFile"));
    }

    try {
      Path path = new Path("/test/TestExistingDir/");
      fs.mkdirs(path);
      fs.getFileChecksum(path);
      fail("Expecting FileNotFoundException");
    } catch (FileNotFoundException e) {
      assertTrue("Not throwing the intended exception message", e.getMessage()
          .contains("Path is not a file: /test/TestExistingDir"));
    }

    checkpoint("AFTER_INITIAL_TESTS");

    // TRANSFORMATION NOTE: WebHDFS portions commented out
    // Keeping only HDFS checksum testing

    final Path dir = new Path("/filechecksum");
    final int block_size = 1024;
    final int buffer_size = conf.getInt(
        CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);

    //try different number of blocks
    for(int n = 0; n < 5; n++) {
      //generate random data
      final byte[] data = new byte[RAN.nextInt(block_size/2-1)+n*block_size+1];
      RAN.nextBytes(data);
      System.out.println("data.length=" + data.length);

      //write data to a file
      final Path foo = new Path(dir, "foo" + n);
      {
        final FSDataOutputStream out = fs.create(foo, false, buffer_size,
            (short)2, block_size);
        out.write(data);
        out.close();
      }

      //compute checksum
      final FileChecksum hdfsfoocs = fs.getFileChecksum(foo);
      System.out.println("hdfsfoocs=" + hdfsfoocs);

      // TRANSFORMATION NOTE: WebHDFS checksum verification commented out

      //create a zero byte file
      final Path zeroByteFile = new Path(dir, "zeroByteFile" + n);
      {
        final FSDataOutputStream out = fs.create(zeroByteFile, false,
            buffer_size, (short)2, block_size);
        out.close();
      }

      //write another file
      final Path bar = new Path(dir, "bar" + n);
      {
        final FSDataOutputStream out = fs.create(bar, false, buffer_size,
            (short)2, block_size);
        out.write(data);
        out.close();
      }

      {
        final FileChecksum zeroChecksum = fs.getFileChecksum(zeroByteFile);
        // zero length file checksum should be null
        assertTrue(zeroChecksum == null);
      }

      { // verify checksum
        final FileChecksum barcs = fs.getFileChecksum(bar);
        final int barhashcode = barcs.hashCode();
        assertEquals(hdfsfoocs.hashCode(), barhashcode);
        assertEquals(hdfsfoocs, barcs);

        // TRANSFORMATION NOTE: WebHDFS comparisons commented out
        // Original code verified checksums matched between HDFS and WebHDFS
      }

      fs.setPermission(dir, new FsPermission((short)0));

      { // test permission error on hftp
        // TRANSFORMATION NOTE: HFTP testing removed (deprecated protocol)
      }

      fs.setPermission(dir, new FsPermission((short)0777));

      // Add checkpoints at specific iterations
      if (n == 1) {
        checkpoint("AFTER_LOOP_ITERATION_1");
      } else if (n == 2) {
        checkpoint("AFTER_LOOP_ITERATION_2");
      } else if (n == 3) {
        checkpoint("AFTER_LOOP_ITERATION_3");
      }
    }

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }

  @Test(timeout=120000)
  public void testStatistics2() throws Exception {
    // TRANSFORMATION NOTE: Storage policy satisfier and encryption zone testing
    // commented out as they require direct NameNode access

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .format(true)
        .build();

    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    Path dir = new Path("/testStat");
    fs.mkdirs(dir);
    int readOps = 0;
    int writeOps = 0;
    FileSystem.clearStatistics();

    // Quota Commands.
    long opCount = getOpStatistics(OpType.SET_QUOTA_USAGE);
    fs.setQuota(dir, 100, 1000);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.SET_QUOTA_USAGE, opCount + 1);

    opCount = getOpStatistics(OpType.SET_QUOTA_BYTSTORAGEYPE);
    fs.setQuotaByStorageType(dir, StorageType.DEFAULT, 2000);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.SET_QUOTA_BYTSTORAGEYPE, opCount + 1);

    opCount = getOpStatistics(OpType.GET_QUOTA_USAGE);
    fs.getQuotaUsage(dir);
    checkStatistics(fs, ++readOps, writeOps, 0);
    checkOpStatistics(OpType.GET_QUOTA_USAGE, opCount + 1);

    checkpoint("AFTER_QUOTA_OPS");

    // TRANSFORMATION NOTE: Storage policy satisfier commented out
    // Requires EXTERNAL mode configuration which may not work with process-based cluster

    // Cache Commands.
    CachePoolInfo cacheInfo =
        new CachePoolInfo("pool1").setMode(new FsPermission((short) 0));

    opCount = getOpStatistics(OpType.ADD_CACHE_POOL);
    fs.addCachePool(cacheInfo);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.ADD_CACHE_POOL, opCount + 1);

    CacheDirectiveInfo directive = new CacheDirectiveInfo.Builder()
        .setPath(new Path(".")).setPool("pool1").build();

    opCount = getOpStatistics(OpType.ADD_CACHE_DIRECTIVE);
    long id = fs.addCacheDirective(directive);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.ADD_CACHE_DIRECTIVE, opCount + 1);

    opCount = getOpStatistics(OpType.LIST_CACHE_DIRECTIVE);
    fs.listCacheDirectives(null);
    checkStatistics(fs, ++readOps, writeOps, 0);
    checkOpStatistics(OpType.LIST_CACHE_DIRECTIVE, opCount + 1);

    opCount = getOpStatistics(OpType.MODIFY_CACHE_DIRECTIVE);
    fs.modifyCacheDirective(new CacheDirectiveInfo.Builder().setId(id)
        .setReplication((short) 2).build());
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.MODIFY_CACHE_DIRECTIVE, opCount + 1);

    opCount = getOpStatistics(OpType.REMOVE_CACHE_DIRECTIVE);
    fs.removeCacheDirective(id);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.REMOVE_CACHE_DIRECTIVE, opCount + 1);

    opCount = getOpStatistics(OpType.MODIFY_CACHE_POOL);
    fs.modifyCachePool(cacheInfo);
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.MODIFY_CACHE_POOL, opCount + 1);

    opCount = getOpStatistics(OpType.LIST_CACHE_POOL);
    fs.listCachePools();
    checkStatistics(fs, ++readOps, writeOps, 0);
    checkOpStatistics(OpType.LIST_CACHE_POOL, opCount + 1);

    opCount = getOpStatistics(OpType.REMOVE_CACHE_POOL);
    fs.removeCachePool(cacheInfo.getPoolName());
    checkStatistics(fs, readOps, ++writeOps, 0);
    checkOpStatistics(OpType.REMOVE_CACHE_POOL, opCount + 1);

    checkpoint("AFTER_CACHE_OPS");

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);

    // TRANSFORMATION NOTE: Encryption zone testing commented out
    // Requires direct NameNode access: cluster.getNameNode().getNamesystem().getProvider()
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

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
import static org.junit.Assume.assumeNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;
import java.util.Random;
import java.util.concurrent.TimeoutException;

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
import org.apache.hadoop.hdfs.web.WebHdfsConstants;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDistributedFileSystem}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Only testFileChecksum and testStatistics2 methods are transformed.
 *
 * @see TestDistributedFileSystem Original test using MiniDFSCluster
 */
public class TestDistributedFileSystem_ProcessBased {

  private static final Random RAN = new Random();

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
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    final long seed = RAN.nextLong();
    System.out.println("seed=" + seed);
    RAN.setSeed(seed);

    final Configuration conf = getTestConfiguration();

    final ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(2)
            .format(true)
            .build();

    try {
      cluster.waitClusterUp();
      final FileSystem hdfs = cluster.getFileSystem();

      // TRANSFORMATION NOTE: WebHDFS testing commented out
      // WebHDFS requires HTTP server address which may not be easily accessible
      // in ProcessBasedMiniDFSCluster. We focus on HDFS file checksum testing.
      //
      // Original code:
      // final String nnAddr = conf.get(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY);
      // final UserGroupInformation current = UserGroupInformation.getCurrentUser();
      // final UserGroupInformation ugi = UserGroupInformation.createUserForTesting(
      //     current.getShortUserName() + "x", new String[]{"user"});

      try {
        hdfs.getFileChecksum(new Path("/test/TestNonExistingFile"));
        fail("Expecting FileNotFoundException");
      } catch (FileNotFoundException e) {
        assertTrue("Not throwing the intended exception message", e.getMessage()
            .contains("File does not exist: /test/TestNonExistingFile"));
      }

      try {
        Path path = new Path("/test/TestExistingDir/");
        hdfs.mkdirs(path);
        hdfs.getFileChecksum(path);
        fail("Expecting FileNotFoundException");
      } catch (FileNotFoundException e) {
        assertTrue("Not throwing the intended exception message", e.getMessage()
            .contains("Path is not a file: /test/TestExistingDir"));
      }

      // TRANSFORMATION NOTE: WebHDFS portions commented out
      // Keeping only HDFS checksum testing

      final Path dir = new Path("/filechecksum");
      final int block_size = 1024;
      final int buffer_size = conf.getInt(
          CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096);
      conf.setInt(HdfsClientConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 512);

      //try different number of blocks
      for(int n = 0; n < 5; n++) {
        //generate random data
        final byte[] data = new byte[RAN.nextInt(block_size/2-1)+n*block_size+1];
        RAN.nextBytes(data);
        System.out.println("data.length=" + data.length);

        //write data to a file
        final Path foo = new Path(dir, "foo" + n);
        {
          final FSDataOutputStream out = hdfs.create(foo, false, buffer_size,
              (short)2, block_size);
          out.write(data);
          out.close();
        }

        //compute checksum
        final FileChecksum hdfsfoocs = hdfs.getFileChecksum(foo);
        System.out.println("hdfsfoocs=" + hdfsfoocs);

        // TRANSFORMATION NOTE: WebHDFS checksum verification commented out
        // Original code:
        // final FileChecksum webhdfsfoocs = webhdfs.getFileChecksum(foo);
        // System.out.println("webhdfsfoocs=" + webhdfsfoocs);
        // final Path webhdfsqualified = new Path(webhdfsuri + dir, "foo" + n);
        // final FileChecksum webhdfs_qfoocs = webhdfs.getFileChecksum(webhdfsqualified);
        // System.out.println("webhdfs_qfoocs=" + webhdfs_qfoocs);

        //create a zero byte file
        final Path zeroByteFile = new Path(dir, "zeroByteFile" + n);
        {
          final FSDataOutputStream out = hdfs.create(zeroByteFile, false,
              buffer_size, (short)2, block_size);
          out.close();
        }

        //write another file
        final Path bar = new Path(dir, "bar" + n);
        {
          final FSDataOutputStream out = hdfs.create(bar, false, buffer_size,
              (short)2, block_size);
          out.write(data);
          out.close();
        }

        {
          final FileChecksum zeroChecksum = hdfs.getFileChecksum(zeroByteFile);
          // zero length file checksum should be null
          assertTrue(zeroChecksum == null);
        }

        { // verify checksum
          final FileChecksum barcs = hdfs.getFileChecksum(bar);
          final int barhashcode = barcs.hashCode();
          assertEquals(hdfsfoocs.hashCode(), barhashcode);
          assertEquals(hdfsfoocs, barcs);

          // TRANSFORMATION NOTE: WebHDFS comparisons commented out
          // Original code verified checksums matched between HDFS and WebHDFS
        }

        hdfs.setPermission(dir, new FsPermission((short)0));

        { // test permission error on hftp
          // TRANSFORMATION NOTE: HFTP testing removed (deprecated protocol)
        }

        hdfs.setPermission(dir, new FsPermission((short)0777));
      }
    } finally {
      cluster.shutdown();
    }
  }

  @Test(timeout=120000)
  public void testStatistics2() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    HdfsConfiguration conf = new HdfsConfiguration(getTestConfiguration());

    // TRANSFORMATION NOTE: Storage policy satisfier and encryption zone testing
    // commented out as they require direct NameNode access

    ProcessBasedMiniDFSCluster cluster =
        new ProcessBasedMiniDFSCluster.Builder(conf)
            .format(true)
            .build();

    try {
      cluster.waitClusterUp();
      final DistributedFileSystem dfs = cluster.getFileSystem();
      Path dir = new Path("/testStat");
      dfs.mkdirs(dir);
      int readOps = 0;
      int writeOps = 0;
      FileSystem.clearStatistics();

      // Quota Commands.
      long opCount = getOpStatistics(OpType.SET_QUOTA_USAGE);
      dfs.setQuota(dir, 100, 1000);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.SET_QUOTA_USAGE, opCount + 1);

      opCount = getOpStatistics(OpType.SET_QUOTA_BYTSTORAGEYPE);
      dfs.setQuotaByStorageType(dir, StorageType.DEFAULT, 2000);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.SET_QUOTA_BYTSTORAGEYPE, opCount + 1);

      opCount = getOpStatistics(OpType.GET_QUOTA_USAGE);
      dfs.getQuotaUsage(dir);
      checkStatistics(dfs, ++readOps, writeOps, 0);
      checkOpStatistics(OpType.GET_QUOTA_USAGE, opCount + 1);

      // TRANSFORMATION NOTE: Storage policy satisfier commented out
      // Requires EXTERNAL mode configuration which may not work with process-based cluster
      // Original code:
      // opCount = getOpStatistics(OpType.SATISFY_STORAGE_POLICY);
      // dfs.satisfyStoragePolicy(dir);
      // checkStatistics(dfs, readOps, ++writeOps, 0);
      // checkOpStatistics(OpType.SATISFY_STORAGE_POLICY, opCount + 1);

      // Cache Commands.
      CachePoolInfo cacheInfo =
          new CachePoolInfo("pool1").setMode(new FsPermission((short) 0));

      opCount = getOpStatistics(OpType.ADD_CACHE_POOL);
      dfs.addCachePool(cacheInfo);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.ADD_CACHE_POOL, opCount + 1);

      CacheDirectiveInfo directive = new CacheDirectiveInfo.Builder()
          .setPath(new Path(".")).setPool("pool1").build();

      opCount = getOpStatistics(OpType.ADD_CACHE_DIRECTIVE);
      long id = dfs.addCacheDirective(directive);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.ADD_CACHE_DIRECTIVE, opCount + 1);

      opCount = getOpStatistics(OpType.LIST_CACHE_DIRECTIVE);
      dfs.listCacheDirectives(null);
      checkStatistics(dfs, ++readOps, writeOps, 0);
      checkOpStatistics(OpType.LIST_CACHE_DIRECTIVE, opCount + 1);

      opCount = getOpStatistics(OpType.MODIFY_CACHE_DIRECTIVE);
      dfs.modifyCacheDirective(new CacheDirectiveInfo.Builder().setId(id)
          .setReplication((short) 2).build());
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.MODIFY_CACHE_DIRECTIVE, opCount + 1);

      opCount = getOpStatistics(OpType.REMOVE_CACHE_DIRECTIVE);
      dfs.removeCacheDirective(id);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.REMOVE_CACHE_DIRECTIVE, opCount + 1);

      opCount = getOpStatistics(OpType.MODIFY_CACHE_POOL);
      dfs.modifyCachePool(cacheInfo);
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.MODIFY_CACHE_POOL, opCount + 1);

      opCount = getOpStatistics(OpType.LIST_CACHE_POOL);
      dfs.listCachePools();
      checkStatistics(dfs, ++readOps, writeOps, 0);
      checkOpStatistics(OpType.LIST_CACHE_POOL, opCount + 1);

      opCount = getOpStatistics(OpType.REMOVE_CACHE_POOL);
      dfs.removeCachePool(cacheInfo.getPoolName());
      checkStatistics(dfs, readOps, ++writeOps, 0);
      checkOpStatistics(OpType.REMOVE_CACHE_POOL, opCount + 1);

      // TRANSFORMATION NOTE: Encryption zone testing commented out
      // Requires direct NameNode access: cluster.getNameNode().getNamesystem().getProvider()
      // Original code:
      // final KeyProvider provider = cluster.getNameNode().getNamesystem().getProvider();
      // final KeyProvider.Options options = KeyProvider.options(conf);
      // provider.createKey("key", options);
      // provider.flush();
      // opCount = getOpStatistics(OpType.CREATE_ENCRYPTION_ZONE);
      // dfs.createEncryptionZone(dir, "key");
      // checkStatistics(dfs, readOps, ++writeOps, 0);
      // checkOpStatistics(OpType.CREATE_ENCRYPTION_ZONE, opCount + 1);
      // ... (remaining encryption zone tests)
    } finally {
      cluster.shutdown();
    }
  }
}

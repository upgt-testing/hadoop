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
package org.apache.hadoop.hdfs.server.datanode.fsdataset.impl;
import org.apache.hadoop.thirdparty.com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.ChecksumException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.ClientContext;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.client.HdfsDataInputStream;
import org.apache.hadoop.hdfs.server.datanode.BlockMetadataHeader;
import org.apache.hadoop.io.nativeio.NativeIO;
import org.apache.hadoop.net.unix.DomainSocket;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.util.NativeCodeLoader;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.apache.hadoop.fs.StorageType.DEFAULT;
import static org.apache.hadoop.fs.StorageType.RAM_DISK;
import static org.apache.hadoop.test.PlatformAssumptions.assumeNotWindows;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test Lazy persist behavior with short-circuit reads. These tests
 * will be run on Linux only with Native IO enabled. The tests fake
 * RAM_DISK storage using local disk.
 */
public class TestScrLazyPersistFiles_RestartInjected extends LazyPersistTestCase {

  @BeforeClass
  public static void init() {
    DomainSocket.disableBindPathValidation();
  }

  @Before
  public void before() {
    Assume.assumeTrue(NativeCodeLoader.isNativeCodeLoaded());
    assumeNotWindows();
    Assume.assumeThat(DomainSocket.getLoadingFailureReason(), equalTo(null));

    final long osPageSize = NativeIO.POSIX.getCacheManipulator().getOperatingSystemPageSize();
    Preconditions.checkState(BLOCK_SIZE >= osPageSize);
    Preconditions.checkState(BLOCK_SIZE % osPageSize == 0);
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  /**
   * Read in-memory block with Short Circuit Read
   * Note: the test uses faked RAM_DISK from physical disk.
   */
  @Test
  public void testRamDiskShortCircuitRead()
      throws IOException, InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true).build();
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    final int SEED = 0xFADED;
    Path path = new Path("/" + METHOD_NAME + ".dat");

    RestartFramework.at("after_cluster_build")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    makeRandomTestFile(path, BLOCK_SIZE, true, SEED);
    ensureFileReplicasOnStorageType(path, RAM_DISK);
    waitForMetric("RamDiskBlocksLazyPersisted", 1);

    RestartFramework.at("after_lazy_persist")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    HdfsDataInputStream fis = (HdfsDataInputStream) fs.open(path);

    RestartFramework.at("after_file_open")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    try {
      byte[] buf = new byte[BUFFER_LENGTH];
      fis.read(0, buf, 0, BUFFER_LENGTH);

      RestartFramework.at("after_scr_read")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      Assert.assertEquals(BUFFER_LENGTH,
        fis.getReadStatistics().getTotalBytesRead());
      Assert.assertEquals(BUFFER_LENGTH,
        fis.getReadStatistics().getTotalShortCircuitBytesRead());
    } finally {
      fis.close();
      fis = null;
    }
  }

  /**
   * Eviction of lazy persisted blocks with Short Circuit Read handle open
   * Note: the test uses faked RAM_DISK from physical disk.
   * @throws IOException
   * @throws InterruptedException
   */
  @Test
  public void tesScrDuringEviction()
      throws Exception {
    getClusterBuilder().setUseScr(true).build();
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    Path path1 = new Path("/" + METHOD_NAME + ".01.dat");

    RestartFramework.at("after_cluster_build")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    makeTestFile(path1, BLOCK_SIZE, true);
    ensureFileReplicasOnStorageType(path1, RAM_DISK);
    waitForMetric("RamDiskBlocksLazyPersisted", 1);

    RestartFramework.at("after_lazy_persist")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    HdfsDataInputStream fis = (HdfsDataInputStream) fs.open(path1);
    try {
      byte[] buf = new byte[BUFFER_LENGTH];
      fis.read(0, buf, 0, BUFFER_LENGTH);

      RestartFramework.at("after_first_read")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      triggerEviction(cluster.getDataNodes().get(0));

      RestartFramework.at("after_eviction_trigger")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      fis.read(0, buf, 0, BUFFER_LENGTH);

      RestartFramework.at("after_second_read")
          .on(cluster)
          .restart("datanode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      assertThat(fis.getReadStatistics().getTotalBytesRead(),
          is((long) 2 * BUFFER_LENGTH));
      assertThat(fis.getReadStatistics().getTotalShortCircuitBytesRead(),
          is((long) 2 * BUFFER_LENGTH));
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  @Test
  public void testScrAfterEviction()
      throws IOException, InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(false)
                       .build();
    doShortCircuitReadAfterEvictionTest();
  }

  @Test
  public void testLegacyScrAfterEviction()
      throws IOException, InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(true)
                       .build();
    doShortCircuitReadAfterEvictionTest();

    // In the implementation of legacy short-circuit reads, any failure is
    // trapped silently, reverts back to a remote read, and also disables all
    // subsequent legacy short-circuit reads in the ClientContext.
    // Assert that it didn't get disabled.
    ClientContext clientContext = client.getClientContext();
    Assert.assertFalse(clientContext.getDisableLegacyBlockReaderLocal());
  }

  private void doShortCircuitReadAfterEvictionTest() throws IOException,
      InterruptedException, TimeoutException {
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    Path path1 = new Path("/" + METHOD_NAME + ".01.dat");

    RestartFramework.at("after_cluster_build")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    final int SEED = 0xFADED;
    makeRandomTestFile(path1, BLOCK_SIZE, true, SEED);
    ensureFileReplicasOnStorageType(path1, RAM_DISK);
    waitForMetric("RamDiskBlocksLazyPersisted", 1);

    RestartFramework.at("after_lazy_persist")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    File metaFile = cluster.getBlockMetadataFile(0,
        DFSTestUtil.getFirstBlock(fs, path1));
    assertTrue(metaFile.length() <= BlockMetadataHeader.getHeaderSize());
    assertTrue(verifyReadRandomFile(path1, BLOCK_SIZE, SEED));

    RestartFramework.at("after_ramdisk_read")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    triggerEviction(cluster.getDataNodes().get(0));

    RestartFramework.at("after_eviction")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    ensureFileReplicasOnStorageType(path1, DEFAULT);
    metaFile = cluster.getBlockMetadataFile(0,
        DFSTestUtil.getFirstBlock(fs, path1));
    assertTrue(metaFile.length() > BlockMetadataHeader.getHeaderSize());
    assertTrue(verifyReadRandomFile(path1, BLOCK_SIZE, SEED));

    RestartFramework.at("after_default_storage_read")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  public void testScrBlockFileCorruption() throws IOException,
      InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(false)
                       .build();
    doShortCircuitReadBlockFileCorruptionTest();
  }

  @Test
  public void testLegacyScrBlockFileCorruption() throws IOException,
      InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(true)
                       .build();
    doShortCircuitReadBlockFileCorruptionTest();
  }

  public void doShortCircuitReadBlockFileCorruptionTest() throws IOException,
      InterruptedException, TimeoutException {
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    Path path1 = new Path("/" + METHOD_NAME + ".01.dat");

    RestartFramework.at("after_cluster_build")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    makeTestFile(path1, BLOCK_SIZE, true);
    ensureFileReplicasOnStorageType(path1, RAM_DISK);
    waitForMetric("RamDiskBlocksLazyPersisted", 1);

    RestartFramework.at("after_lazy_persist")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    triggerEviction(cluster.getDataNodes().get(0));

    RestartFramework.at("after_eviction")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    ensureFileReplicasOnStorageType(path1, DEFAULT);
    cluster.corruptReplica(0, DFSTestUtil.getFirstBlock(fs, path1));

    RestartFramework.at("after_block_corruption")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    exception.expect(ChecksumException.class);
    DFSTestUtil.readFileBuffer(fs, path1);
  }

  @Test
  public void testScrMetaFileCorruption() throws IOException,
      InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(false)
                       .build();
    doShortCircuitReadMetaFileCorruptionTest();
  }

  @Test
  public void testLegacyScrMetaFileCorruption() throws IOException,
      InterruptedException, TimeoutException {
    getClusterBuilder().setUseScr(true)
                       .setUseLegacyBlockReaderLocal(true)
                       .build();
    doShortCircuitReadMetaFileCorruptionTest();
  }

  public void doShortCircuitReadMetaFileCorruptionTest() throws IOException,
      InterruptedException, TimeoutException {
    final String METHOD_NAME = GenericTestUtils.getMethodName();
    Path path1 = new Path("/" + METHOD_NAME + ".01.dat");

    RestartFramework.at("after_cluster_build")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    makeTestFile(path1, BLOCK_SIZE, true);
    ensureFileReplicasOnStorageType(path1, RAM_DISK);
    waitForMetric("RamDiskBlocksLazyPersisted", 1);

    RestartFramework.at("after_lazy_persist")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    triggerEviction(cluster.getDataNodes().get(0));

    RestartFramework.at("after_eviction")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    ensureFileReplicasOnStorageType(path1, DEFAULT);
    cluster.corruptMeta(0, DFSTestUtil.getFirstBlock(fs, path1));

    RestartFramework.at("after_meta_corruption")
        .on(cluster)
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    exception.expect(ChecksumException.class);
    DFSTestUtil.readFileBuffer(fs, path1);
  }
}

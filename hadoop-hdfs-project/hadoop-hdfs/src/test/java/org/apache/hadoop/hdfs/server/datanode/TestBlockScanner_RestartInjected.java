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
package org.apache.hadoop.hdfs.server.datanode;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_BLOCK_SCANNER_VOLUME_BYTES_PER_SECOND;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_DATANODE_SCAN_PERIOD_HOURS_KEY;
import static org.apache.hadoop.hdfs.server.datanode.BlockScanner.Conf.INTERNAL_DFS_DATANODE_SCAN_PERIOD_MS;
import static org.apache.hadoop.hdfs.server.datanode.BlockScanner.Conf.INTERNAL_VOLUME_SCANNER_SCAN_RESULT_HANDLER;
import static org.apache.hadoop.hdfs.server.datanode.BlockScanner.Conf.INTERNAL_DFS_BLOCK_SCANNER_CURSOR_SAVE_INTERVAL_MS;
import static org.junit.Assert.assertEquals;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import java.util.function.Supplier;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.server.datanode.VolumeScanner.ScanResultHandler;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsDatasetSpi;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsVolumeImpl;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.hdfs.RestartInjectionFramework;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartPoint;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.waitForLeaseRecovery;

/**
 * Restart-injection variant of TestBlockScanner.
 * Tests block scanner operations survive component restarts.
 *
 * Original test: testAppendWhileScanning (has hflush)
 *
 * Generated variants:
 * - 4 RestartTargets x 2 RestartModes = 8 variants
 */
public class TestBlockScanner_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestBlockScanner_RestartInjected.class);

  @Before
  public void before() {
    BlockScanner.Conf.allowUnitTestSettings = true;
    GenericTestUtils.setLogLevel(BlockScanner.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(VolumeScanner.LOG, Level.ALL);
    GenericTestUtils.setLogLevel(FsVolumeImpl.LOG, Level.ALL);
  }

  private static class TestContext implements Closeable {
    final int numNameServices;
    final MiniDFSCluster cluster;
    final DistributedFileSystem[] dfs;
    final String[] bpids;
    DataNode datanode;
    BlockScanner blockScanner;
    FsDatasetSpi<? extends FsVolumeSpi> data;
    FsDatasetSpi.FsVolumeReferences volumes;

    TestContext(Configuration conf, int numNameServices) throws Exception {
      this.numNameServices = numNameServices;
      File basedir = new File(GenericTestUtils.getRandomizedTempPath());
      MiniDFSCluster.Builder bld = new MiniDFSCluster.Builder(conf, basedir).
          numDataNodes(1).
          storagesPerDatanode(1);
      cluster = bld.build();
      cluster.waitActive();
      dfs = new DistributedFileSystem[numNameServices];
      for (int i = 0; i < numNameServices; i++) {
        dfs[i] = cluster.getFileSystem(i);
      }
      bpids = new String[numNameServices];
      for (int i = 0; i < numNameServices; i++) {
        bpids[i] = cluster.getNamesystem(i).getBlockPoolId();
      }
      datanode = cluster.getDataNodes().get(0);
      blockScanner = datanode.getBlockScanner();
      for (int i = 0; i < numNameServices; i++) {
        dfs[i].mkdirs(new Path("/test"));
      }
      data = datanode.getFSDataset();
      volumes = data.getFsVolumeReferences();
    }

    void refreshAfterRestart() {
      if (cluster.getDataNodes().size() > 0) {
        datanode = cluster.getDataNodes().get(0);
        blockScanner = datanode.getBlockScanner();
        data = datanode.getFSDataset();
        volumes = data.getFsVolumeReferences();
      }
    }

    @Override
    public void close() throws IOException {
      if (volumes != null) {
        volumes.close();
      }
      if (cluster != null) {
        for (int i = 0; i < numNameServices; i++) {
          dfs[i].delete(new Path("/test"), true);
          dfs[i].close();
        }
        cluster.shutdown();
      }
    }

    public void createFiles(int nsIdx, int numFiles, int length)
          throws Exception {
      for (int blockIdx = 0; blockIdx < numFiles; blockIdx++) {
        DFSTestUtil.createFile(dfs[nsIdx], getPath(blockIdx), length,
            (short)1, 123L);
      }
    }

    public Path getPath(int fileIdx) {
      return new Path("/test/" + fileIdx);
    }

    public ExtendedBlock getFileBlock(int nsIdx, int fileIdx)
          throws Exception {
      return DFSTestUtil.getFirstBlock(dfs[nsIdx], getPath(fileIdx));
    }
  }

  public static class TestScanResultHandler extends ScanResultHandler {
    static class Info {
      boolean shouldRun = false;
      final Set<ExtendedBlock> badBlocks = new HashSet<ExtendedBlock>();
      final Set<ExtendedBlock> goodBlocks = new HashSet<ExtendedBlock>();
      long blocksScanned = 0;
      Semaphore sem = null;

      @Override
      public String toString() {
        final StringBuilder bld = new StringBuilder();
        bld.append("ScanResultHandler.Info{");
        bld.append("shouldRun=").append(shouldRun).append(", ");
        bld.append("blocksScanned=").append(blocksScanned).append(", ");
        bld.append("sem#availablePermits=").append(sem != null ? sem.availablePermits() : "null").
            append(", ");
        bld.append("badBlocks=").append(badBlocks).append(", ");
        bld.append("goodBlocks=").append(goodBlocks);
        bld.append("}");
        return bld.toString();
      }
    }

    private VolumeScanner scanner;

    final static ConcurrentHashMap<String, Info> infos =
        new ConcurrentHashMap<String, Info>();

    static Info getInfo(FsVolumeSpi volume) {
      Info newInfo = new Info();
      Info prevInfo = infos.
          putIfAbsent(volume.getStorageID(), newInfo);
      return prevInfo == null ? newInfo : prevInfo;
    }

    static void clearInfos() {
      infos.clear();
    }

    @Override
    public void setup(VolumeScanner scanner) {
      this.scanner = scanner;
      Info info = getInfo(scanner.volume);
      LOG.info("about to start scanning.");
      synchronized (info) {
        while (!info.shouldRun) {
          try {
            info.wait();
          } catch (InterruptedException e) {
          }
        }
      }
      LOG.info("starting scanning.");
    }

    @Override
    public void handle(ExtendedBlock block, IOException e) {
      LOG.info("handling block {} (exception {})", block, e);
      Info info = getInfo(scanner.volume);
      Semaphore sem;
      synchronized (info) {
        sem = info.sem;
      }
      if (sem != null) {
        try {
          sem.acquire();
        } catch (InterruptedException ie) {
          throw new RuntimeException("interrupted");
        }
      }
      synchronized (info) {
        if (!info.shouldRun) {
          throw new RuntimeException("stopping volumescanner thread.");
        }
        if (e == null) {
          info.goodBlocks.add(block);
        } else {
          info.badBlocks.add(block);
        }
        info.blocksScanned++;
      }
    }
  }

  private void waitForRescan(final TestScanResultHandler.Info info,
      final int numExpectedBlocks)
      throws TimeoutException, InterruptedException {
    LOG.info("Waiting for the first {} blocks to be scanned.", numExpectedBlocks);
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        synchronized (info) {
          if (info.blocksScanned >= numExpectedBlocks) {
            LOG.info("info = {}.  blockScanned has now reached {}.", info, numExpectedBlocks);
            return true;
          } else {
            LOG.info("info = {}.  Waiting for blockScanned to reach {}.", info, numExpectedBlocks);
            return false;
          }
        }
      }
    }, 1000, 30000);

    synchronized (info) {
      assertEquals("Expected " + numExpectedBlocks + " good block.",
          numExpectedBlocks, info.goodBlocks.size());
      info.goodBlocks.clear();
      assertEquals("Expected " + numExpectedBlocks + " blocksScanned",
          numExpectedBlocks, info.blocksScanned);
      assertEquals("Did not expect bad blocks.", 0, info.badBlocks.size());
      info.blocksScanned = 0;
    }
  }

  /**
   * Core test logic for concurrent append and scan with restart injection.
   */
  private void testAppendWhileScanningWithRestart(RestartTarget target, RestartMode mode) throws Exception {
    GenericTestUtils.setLogLevel(DataNode.LOG, Level.ALL);
    Configuration conf = new Configuration();
    // Client connection survives NN restart
    conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);
    // throttle the block scanner: 1MB per second
    conf.setLong(DFS_BLOCK_SCANNER_VOLUME_BYTES_PER_SECOND, 1048576);
    // Set a really long scan period.
    conf.setLong(DFS_DATANODE_SCAN_PERIOD_HOURS_KEY, 100L);
    conf.set(INTERNAL_VOLUME_SCANNER_SCAN_RESULT_HANDLER,
        TestScanResultHandler.class.getName());
    conf.setLong(INTERNAL_DFS_BLOCK_SCANNER_CURSOR_SAVE_INTERVAL_MS, 0L);
    final int numExpectedFiles = 1;
    final int numExpectedBlocks = 1;
    final int numNameServices = 1;
    // the initial file length can not be too small.
    final int initialFileLength = 2*1024*1024+100;

    // Clear any previous test state
    TestScanResultHandler.clearInfos();

    final TestContext ctx = new TestContext(conf, numNameServices);
    try {
      // create one file, with one block.
      ctx.createFiles(0, numExpectedFiles, initialFileLength);
      final TestScanResultHandler.Info info =
          TestScanResultHandler.getInfo(ctx.volumes.get(0));
      String storageID = ctx.volumes.get(0).getStorageID();
      synchronized (info) {
        info.sem = new Semaphore(numExpectedBlocks*2);
        info.shouldRun = true;
        info.notify();
      }
      // VolumeScanner scans the first block when DN starts.
      waitForRescan(info, numExpectedBlocks);

      // update throttler to schedule rescan immediately.
      conf.setLong(DFS_BLOCK_SCANNER_VOLUME_BYTES_PER_SECOND,
          initialFileLength+32*1024);
      BlockScanner.Conf newConf = new BlockScanner.Conf(conf);
      ctx.datanode.getBlockScanner().setConf(newConf);
      // schedule the first block for scanning
      ExtendedBlock first = ctx.getFileBlock(0, 0);
      ctx.datanode.getBlockScanner().markSuspectBlock(storageID, first);

      // append the file before VolumeScanner completes scanning the block
      FileSystem fs = ctx.cluster.getFileSystem();
      FSDataOutputStream os = fs.append(ctx.getPath(0));
      long seed = -1;
      int size = 200;
      final byte[] bytes = AppendTestUtil.randomBytes(seed, size);
      os.write(bytes);
      os.hflush();

      // === RESTART INJECTION POINT: After hflush ===
      LOG.info("=== INJECTING RESTART: target={}, mode={} ===", target, mode);
      executeRestart(ctx.cluster, target, mode, true);
      verifyClusterHealth(ctx.cluster, fs);
      ctx.refreshAfterRestart();
      LOG.info("=== RESTART COMPLETE ===");

      os.close();

      // verify that volume scanner does not find bad blocks after append and restart.
      // After restart, we need to re-get the info since volumes might have changed
      if (ctx.volumes != null && ctx.volumes.size() > 0) {
        final TestScanResultHandler.Info infoAfterRestart =
            TestScanResultHandler.getInfo(ctx.volumes.get(0));
        synchronized (infoAfterRestart) {
          infoAfterRestart.sem = new Semaphore(numExpectedBlocks*2);
          infoAfterRestart.shouldRun = true;
          infoAfterRestart.notify();
        }
        waitForRescan(infoAfterRestart, numExpectedBlocks);
      }

      GenericTestUtils.setLogLevel(DataNode.LOG, Level.INFO);
    } finally {
      ctx.close();
    }
  }

  // ============================================================
  // Test variants: testAppendWhileScanning with restart injection
  // AfterHflush x 4 targets x 2 modes = 8 variants
  // ============================================================

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_NN_Graceful() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.NAMENODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_NN_Crash() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.NAMENODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_SingleDN_Graceful() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_SingleDN_Crash() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.SINGLE_DATANODE, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_AllDN_Graceful() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_AllDN_Crash() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.ALL_DATANODES, RestartMode.CRASH);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_NNDN_Graceful() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.GRACEFUL);
  }

  @Test(timeout = 180000)
  public void testAppendWhileScanning_AfterHflush_NNDN_Crash() throws Exception {
    testAppendWhileScanningWithRestart(RestartTarget.NAMENODE_AND_DATANODES, RestartMode.CRASH);
  }
}

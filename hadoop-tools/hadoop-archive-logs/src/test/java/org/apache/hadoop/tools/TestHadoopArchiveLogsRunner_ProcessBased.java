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

package org.apache.hadoop.tools;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.HarFs;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ProcessBased version of {@link TestHadoopArchiveLogsRunner}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * @see TestHadoopArchiveLogsRunner Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class TestHadoopArchiveLogsRunner_ProcessBased extends YarnUpgradeTestBase {

  private static final int FILE_SIZE_INCREMENT = 4096;
  private static final int[] FILE_SIZES = {3, 4, 2};
  private static final int FILE_COUNT = FILE_SIZES.length;
  private static final byte[] DUMMY_DATA = new byte[FILE_SIZE_INCREMENT];
  static {
    new Random().nextBytes(DUMMY_DATA);
  }

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_HDFS_START",
      "AFTER_LOG_FILES_CREATED",
      "AFTER_HAL_INIT",
      "AFTER_HAL_RUN"
    );
  }

  private MiniDFSCluster dfsCluster;
  private FileSystem fs;
  private ApplicationId app1;
  private Path app1Path;
  private Path workingDir;
  private Path remoteRootLogDir;
  private String suffix;

  private void setupHadoopArchiveLogsTest() throws Exception {
    conf = new YarnConfiguration();
    conf.setBoolean(YarnConfiguration.LOG_AGGREGATION_ENABLED, true);
    conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);

    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(2)
        .numResourceManagers(1)
        .build();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    conf = cluster.getConfiguration();
    dfsCluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build();
    conf = new JobConf(conf);

    checkpoint("AFTER_HDFS_START");

    app1 = ApplicationId.newInstance(System.currentTimeMillis(), 1);
    fs = FileSystem.get(conf);
    remoteRootLogDir = new Path(conf.get(
        YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
        YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR));
    workingDir = new Path(remoteRootLogDir, "archive-logs-work");
    suffix = "logs";
    Path logDir = new Path(remoteRootLogDir,
        new Path(System.getProperty("user.name"), suffix));
    fs.mkdirs(logDir);
    app1Path = new Path(logDir, app1.toString());
    fs.mkdirs(app1Path);
    for (int i = 0; i < FILE_COUNT; i++) {
      createFile(fs, new Path(app1Path, "log" + (i + 1)), FILE_SIZES[i]);
    }
    FileStatus[] app1Files = fs.listStatus(app1Path);
    Assert.assertEquals(FILE_COUNT, app1Files.length);

    checkpoint("AFTER_LOG_FILES_CREATED");
  }

  private void teardownHadoopArchiveLogsTest() throws IOException {
    if (fs != null) {
      fs.close();
      fs = null;
    }
    if (dfsCluster != null) {
      dfsCluster.shutdown();
      dfsCluster = null;
    }
  }

  @Test(timeout=120000)
  public void testHadoopArchiveLogs() throws Exception {
    setupHadoopArchiveLogsTest();
    try {
      String[] args = getArgs();
      final HadoopArchiveLogsRunner halr = new HadoopArchiveLogsRunner(conf);
      checkpoint("AFTER_HAL_INIT");

      assertEquals(0, ToolRunner.run(halr, args));
      checkpoint("AFTER_HAL_RUN");

      fs = FileSystem.get(conf);
      FileStatus[] app1Files = fs.listStatus(app1Path);
      Assert.assertEquals(1, app1Files.length);
      FileStatus harFile = app1Files[0];
      Assert.assertEquals(app1.toString() + ".har", harFile.getPath().getName());
      Path harPath = new Path("har:///" + harFile.getPath().toUri().getRawPath());
      FileStatus[] harLogs = HarFs.get(harPath.toUri(), conf).listStatus(harPath);
      Assert.assertEquals(FILE_COUNT, harLogs.length);
      Arrays.sort(harLogs, new Comparator<FileStatus>() {
        @Override
        public int compare(FileStatus o1, FileStatus o2) {
          return o1.getPath().getName().compareTo(o2.getPath().getName());
        }
      });
      for (int i = 0; i < FILE_COUNT; i++) {
        FileStatus harLog = harLogs[i];
        Assert.assertEquals("log" + (i + 1), harLog.getPath().getName());
        Assert.assertEquals(FILE_SIZES[i] * FILE_SIZE_INCREMENT, harLog.getLen());
        Assert.assertEquals(
            new FsPermission(FsAction.READ_WRITE, FsAction.READ, FsAction.NONE),
            harLog.getPermission());
        Assert.assertEquals(System.getProperty("user.name"),
            harLog.getOwner());
      }
      Assert.assertEquals(0, fs.listStatus(workingDir).length);
    } finally {
      teardownHadoopArchiveLogsTest();
    }
  }

  @Test(timeout=120000)
  public void testHadoopArchiveLogsWithArchiveError() throws Exception {
    setupHadoopArchiveLogsTest();
    try {
      String[] args = getArgs();
      final HadoopArchiveLogsRunner halr = new HadoopArchiveLogsRunner(conf);
      HadoopArchives mockHadoopArchives = mock(HadoopArchives.class);
      when(mockHadoopArchives.run(Mockito.<String[]>any())).thenReturn(-1);
      halr.hadoopArchives = mockHadoopArchives;
      checkpoint("AFTER_HAL_INIT");

      assertNotEquals(0, ToolRunner.run(halr, args));
      checkpoint("AFTER_HAL_RUN");

      // Make sure the original log files are intact
      FileStatus[] app1Files = fs.listStatus(app1Path);
      assertEquals(FILE_COUNT, app1Files.length);
      for (int i = 0; i < FILE_COUNT; i++) {
        Assert.assertEquals(FILE_SIZES[i] * FILE_SIZE_INCREMENT,
            app1Files[i].getLen());
      }
    } finally {
      teardownHadoopArchiveLogsTest();
    }
  }

  private String[] getArgs() {
    return new String[]{
        "-appId", app1.toString(),
        "-user", System.getProperty("user.name"),
        "-workingDir", workingDir.toString(),
        "-remoteRootLogDir", remoteRootLogDir.toString(),
        "-suffix", suffix};
  }

  private static void createFile(FileSystem fs, Path p, long sizeMultiple)
      throws IOException {
    FSDataOutputStream out = null;
    try {
      out = fs.create(p);
      for (int i = 0 ; i < sizeMultiple; i++) {
        out.write(DUMMY_DATA);
      }
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}

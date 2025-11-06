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

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.TestListFiles;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestListFilesInDFS}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class tests the FileStatus API.
 *
 * @see TestListFilesInDFS Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestListFilesInDFS_ProcessBased extends TestListFiles {
  {
    GenericTestUtils.setLogLevel(FileSystem.LOG, Level.ALL);
  }

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        UpgradeCheckpoints.NO_UPGRADE,
        UpgradeCheckpoints.AFTER_CLUSTER_START
    );
  }

  private ProcessBasedMiniDFSCluster cluster;
  private UpgradeHelper helper;

  // Helper class to access ProcessBasedUpgradeTestBase protected methods
  private static class UpgradeHelper extends ProcessBasedUpgradeTestBase {
    public void setUpgradeCheckpoint(String checkpoint) {
      this.upgradeCheckpoint = checkpoint;
    }

    public void doCheckpoint(String name) throws Exception {
      checkpoint(name);
    }

    public org.apache.hadoop.conf.Configuration getConf() {
      return conf;
    }

    public void setCluster(ProcessBasedMiniDFSCluster c) {
      this.cluster = c;
    }

    public void setFs(org.apache.hadoop.hdfs.DistributedFileSystem dfs) {
      this.fs = dfs;
    }
  }

  @Before
  public void setUp() throws Exception {
    helper = new UpgradeHelper();
    helper.setUpgradeCheckpoint(this.upgradeCheckpoint);
    helper.setupTest();

    setTestPaths(new Path("/tmp/TestListFilesInDFS"));
    cluster = new ProcessBasedMiniDFSCluster.Builder(helper.getConf()).build();
    cluster.waitClusterUp();
    helper.setCluster(cluster);
    helper.doCheckpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    helper.setFs((org.apache.hadoop.hdfs.DistributedFileSystem) fs);
    fs.delete(TEST_DIR, true);
  }

  @After
  public void tearDown() throws Exception {
    if (fs != null) {
      try {
        fs.close();
      } catch (Exception e) {
        // Ignore
      }
    }
    if (helper != null) {
      helper.tearDownTest();
    }
  }

  protected static Path getTestDir() {
    return new Path("/main_");
  }
}

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestLocalDFS}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class tests the DFS class via the FileSystem interface in a single node
 * mini-cluster.
 *
 * @see TestLocalDFS Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestLocalDFS_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_WRITE",
      "BEFORE_CLEANUP"
    );
  }

  private void writeFile(FileSystem fileSys, Path name) throws IOException {
    DataOutputStream stm = fileSys.create(name);
    stm.writeBytes("oom");
    stm.close();
  }

  private void readFile(FileSystem fileSys, Path name) throws IOException {
    DataInputStream stm = fileSys.open(name);
    byte[] buffer = new byte[4];
    int bytesRead = stm.read(buffer, 0 , 4);
    assertEquals("oom", new String(buffer, 0 , bytesRead));
    stm.close();
  }

  private void cleanupFile(FileSystem fileSys, Path name) throws IOException {
    assertTrue(fileSys.exists(name));
    fileSys.delete(name, true);
    assertTrue(!fileSys.exists(name));
  }

  static String getUserName(FileSystem fs) {
    if (fs instanceof DistributedFileSystem) {
      return ((DistributedFileSystem)fs).dfs.ugi.getShortUserName();
    }
    return System.getProperty("user.name");
  }

  /**
   * Tests get/set working directory in DFS.
   */
  @Test(timeout=20000)
  public void testWorkingDirectory() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    Path orig_path = fs.getWorkingDirectory();
    assertTrue(orig_path.isAbsolute());
    Path file1 = new Path("somewhat/random.txt");
    writeFile(fs, file1);
    checkpoint("AFTER_FILE_WRITE");
    assertTrue(fs.exists(new Path(orig_path, file1.toString())));
    fs.delete(file1, true);
    Path subdir1 = new Path("/somewhere");
    fs.setWorkingDirectory(subdir1);
    writeFile(fs, file1);
    cleanupFile(fs, new Path(subdir1, file1.toString()));
    Path subdir2 = new Path("else");
    fs.setWorkingDirectory(subdir2);
    writeFile(fs, file1);
    readFile(fs, file1);
    checkpoint("BEFORE_CLEANUP");
    cleanupFile(fs, new Path(new Path(subdir1, subdir2.toString()),
                              file1.toString()));

    // test home directory
    Path home =
      fs.makeQualified(
          new Path(HdfsClientConfigKeys.DFS_USER_HOME_DIR_PREFIX_DEFAULT
              + "/" + getUserName(fs)));
    Path fsHome = fs.getHomeDirectory();
    assertEquals(home, fsHome);
  }

  /**
   * Tests get/set working directory in DFS.
   */
  @Test(timeout=30000)
  public void testHomeDirectory() throws Exception {
    final String[] homeBases = new String[] {"/home", "/home/user"};
    conf = new HdfsConfiguration();
    for (final String homeBase : homeBases) {
      conf.set(HdfsClientConfigKeys.DFS_USER_HOME_DIR_PREFIX_KEY, homeBase);
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
      cluster.waitClusterUp();
      fs = cluster.getFileSystem();

      checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

      // test home directory
      Path home =
          fs.makeQualified(
              new Path(homeBase + "/" + getUserName(fs)));
      Path fsHome = fs.getHomeDirectory();
      assertEquals(home, fsHome);

      checkpoint("BEFORE_CLEANUP");

      // Cleanup between iterations
      fs.close();
      cluster.shutdown();
      fs = null;
      cluster = null;
    }
  }
}

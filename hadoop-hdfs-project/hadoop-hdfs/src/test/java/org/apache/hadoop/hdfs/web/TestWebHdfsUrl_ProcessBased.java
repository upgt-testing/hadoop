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

package org.apache.hadoop.hdfs.web;

import static org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod.KERBEROS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.WebHdfs;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestWebHdfsUrl}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Note: Only cluster-using tests transformed. Non-cluster URL encoding tests
 * remain in original TestWebHdfsUrl.
 *
 * @see TestWebHdfsUrl Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestWebHdfsUrl_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final String SPECIAL_CHARACTER_FILENAME =
          "specialFile ?\"\\()[]_-=&+;,{}#%'`~!@$^*|<>.";

  private static final String BACKWARD_COMPATIBLE_SPECIAL_CHARACTER_FILENAME =
          "specialFile ?\"\\()[]_-=&,{}#'`~!@$^*|<>.+%";

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_STATUS_CHECK",
      "AFTER_LISTING",
      "BEFORE_VERIFICATION"
    );
  }

  @Test
  public void testWebHdfsSpecialCharacterFile() throws Exception {
    UserGroupInformation ugi =
            UserGroupInformation.createRemoteUser("test-user");
    ugi.setAuthenticationMethod(KERBEROS);
    UserGroupInformation.setLoginUser(ugi);

    final Configuration clusterConf = new HdfsConfiguration(conf);
    final Path dir = new Path("/testWebHdfsSpecialCharacterFile");

    final short numDatanodes = 1;
    cluster = new ProcessBasedMiniDFSCluster.Builder(clusterConf)
            .numDataNodes(numDatanodes)
            .build();

    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final FileSystem webhdfs = WebHdfsTestUtil
            .getWebHdfsFileSystem(clusterConf, WebHdfs.SCHEME);

    //create a file
    final long length = 1L << 10;
    final Path file1 = new Path(dir, SPECIAL_CHARACTER_FILENAME);

    DFSTestUtil.createFile(webhdfs, file1, length, numDatanodes, 20120406L);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    //get file status and check that it was written properly.
    final FileStatus s1 = webhdfs.getFileStatus(file1);
    assertEquals("Write failed for file " + file1, length, s1.getLen());
    checkpoint("AFTER_STATUS_CHECK");

    boolean found = false;
    RemoteIterator<LocatedFileStatus> statusRemoteIterator =
            webhdfs.listFiles(dir, false);
    while (statusRemoteIterator.hasNext()) {
      LocatedFileStatus locatedFileStatus = statusRemoteIterator.next();
      if (locatedFileStatus.isFile() &&
              SPECIAL_CHARACTER_FILENAME
                      .equals(locatedFileStatus.getPath().getName())) {
        found = true;
      }
    }
    checkpoint("AFTER_LISTING");

    checkpoint("BEFORE_VERIFICATION");
    assertFalse("Could not find file with special character", !found);
  }

  @Test
  public void testWebHdfsBackwardCompatibleSpecialCharacterFile()
          throws Exception {
    UserGroupInformation ugi =
            UserGroupInformation.createRemoteUser("test-user");
    ugi.setAuthenticationMethod(KERBEROS);
    UserGroupInformation.setLoginUser(ugi);

    final Configuration clusterConf = new HdfsConfiguration(conf);
    final Path dir = new Path("/testWebHdfsBackwardCompatibleSpecialCharacterFile");

    final short numDatanodes = 1;
    cluster = new ProcessBasedMiniDFSCluster.Builder(clusterConf)
            .numDataNodes(numDatanodes)
            .build();

    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final FileSystem webhdfs = WebHdfsTestUtil
            .getWebHdfsFileSystem(clusterConf, WebHdfs.SCHEME);

    //create a file
    final long length = 1L << 10;
    final Path file1 = new Path(dir,
            BACKWARD_COMPATIBLE_SPECIAL_CHARACTER_FILENAME);

    DFSTestUtil.createFile(webhdfs, file1, length, numDatanodes, 20120406L);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    //get file status and check that it was written properly.
    final FileStatus s1 = webhdfs.getFileStatus(file1);
    assertEquals("Write failed for file " + file1, length, s1.getLen());
    checkpoint("AFTER_STATUS_CHECK");

    boolean found = false;
    RemoteIterator<LocatedFileStatus> statusRemoteIterator =
            webhdfs.listFiles(dir, false);
    while (statusRemoteIterator.hasNext()) {
      LocatedFileStatus locatedFileStatus = statusRemoteIterator.next();
      if (locatedFileStatus.isFile() &&
              BACKWARD_COMPATIBLE_SPECIAL_CHARACTER_FILENAME
                      .equals(locatedFileStatus.getPath().getName())) {
        found = true;
      }
    }
    checkpoint("AFTER_LISTING");

    checkpoint("BEFORE_VERIFICATION");
    assertFalse("Could not find file with special character", !found);
  }

  @Test
  public void testWebHdfsPathWithSemicolon() throws Exception {
    final Configuration clusterConf = new HdfsConfiguration(conf);
    cluster = new ProcessBasedMiniDFSCluster.Builder(clusterConf)
            .numDataNodes(1)
            .build();

    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // regression test for HDFS-14423.
    final Path semicolon = new Path("/a;b");
    final Path plus = new Path("/a+b");
    final Path percent = new Path("/a%b");

    final WebHdfsFileSystem webhdfs = WebHdfsTestUtil.getWebHdfsFileSystem(
        clusterConf, WebHdfs.SCHEME);
    webhdfs.create(semicolon).close();
    webhdfs.create(plus).close();
    webhdfs.create(percent).close();
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    final DistributedFileSystem dfs = cluster.getFileSystem();
    assertEquals(semicolon.getName(),
        dfs.getFileStatus(semicolon).getPath().getName());
    assertEquals(plus.getName(),
        dfs.getFileStatus(plus).getPath().getName());
    assertEquals(percent.getName(),
        dfs.getFileStatus(percent).getPath().getName());
    checkpoint("BEFORE_VERIFICATION");
  }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

import static org.apache.hadoop.fs.CreateFlag.CREATE;
import static org.apache.hadoop.fs.FileContextTestHelper.getDefaultBlockSize;
import static org.apache.hadoop.fs.FileContextTestHelper.getFileData;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestWebHdfsFileContextMainOperations}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestWebHdfsFileContextMainOperations Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestWebHdfsFileContextMainOperations_ProcessBased
    extends FileContextMainOperationsBaseTest {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        UpgradeCheckpoints.NO_UPGRADE,
        UpgradeCheckpoints.AFTER_CLUSTER_START
    );
  }

  protected ProcessBasedMiniDFSCluster cluster;
  private Path defaultWorkingDirectory;
  protected URI webhdfsUrl;

  protected int numBlocks = 2;

  protected final byte[] data = getFileData(numBlocks,
      getDefaultBlockSize());
  protected HdfsConfiguration conf;

  @Override
  public Path getDefaultWorkingDirectory() {
    return defaultWorkingDirectory;
  }

  @Override
  protected FileContextTestHelper createFileContextHelper() {
    return new FileContextTestHelper();
  }

  public URI getWebhdfsUrl() {
    return webhdfsUrl;
  }

  @Before
  public void setUp() throws Exception {

    conf = new HdfsConfiguration();

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();
    webhdfsUrl = new URI(WebHdfs.SCHEME + "://" + cluster.getConfiguration(0)
        .get(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY));
    fc = FileContext.getFileContext(webhdfsUrl, conf);
    defaultWorkingDirectory = fc.makeQualified(new Path(
        "/user/" + UserGroupInformation.getCurrentUser().getShortUserName()));
    fc.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);

    // Upgrade checkpoint after cluster start
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Now initialize test paths
    URI webhdfsUrlReal = getWebhdfsUrl();
    Path testBuildData = new Path(
        webhdfsUrlReal + "/" + GenericTestUtils.DEFAULT_TEST_DATA_PATH
            + RandomStringUtils.randomAlphanumeric(10));
    Path rootPath = new Path(testBuildData, "root-uri");

    localFsRootPath = rootPath.makeQualified(webhdfsUrlReal, null);
    fc.mkdir(getTestRootPath(fc, "test"), FileContext.DEFAULT_PERM, true);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  /**
   * Check if upgrade should be performed at the given checkpoint.
   */
  protected boolean shouldUpgrade(String name) {
    return upgradeCheckpoint != null
        && !upgradeCheckpoint.equals(UpgradeCheckpoints.NO_UPGRADE)
        && upgradeCheckpoint.equals(name);
  }

  /**
   * Insert an upgrade checkpoint in the test.
   */
  protected void checkpoint(String name) throws Exception {
    if (shouldUpgrade(name)) {
      cluster.upgrade();
      cluster.waitClusterUp();
    }
  }

  private Path getTestRootPath(FileContext fc, String path) {
    return fileContextTestHelper.getTestRootPath(fc, path);
  }

  @Override
  protected boolean listCorruptedBlocksSupported() {
    return false;
  }

  /**
   * Test FileContext APIs when symlinks are not supported
   * TODO: Open separate JIRA for full support of the Symlink in webhdfs
   */
  @Test
  public void testUnsupportedSymlink() throws IOException {
    /**
     * WebHdfs client Partially supports the Symlink.
     * creation of Symlink is supported, but the getLinkTargetPath() api is not supported currently,
     * Implement the test case once the full support is available.
     */
  }

  /**
   * TODO: Open JIRA for the idiosyncrasies between hdfs and webhdfs
   */
  public void testSetVerifyChecksum() throws IOException {
    final Path rootPath = getTestRootPath(fc, "test");
    final Path path = new Path(rootPath, "zoo");

    FSDataOutputStream out = fc
        .create(path, EnumSet.of(CREATE), Options.CreateOpts.createParent());
    try {
      out.write(data, 0, data.length);
    } finally {
      out.close();
    }

    //In webhdfs scheme fc.setVerifyChecksum() can be called only after
    // writing first few bytes but in case of the hdfs scheme we can call
    // immediately after the creation call.
    // instruct FS to verify checksum through the FileContext:
    fc.setVerifyChecksum(true, path);

    FileStatus fileStatus = fc.getFileStatus(path);
    final long len = fileStatus.getLen();
    assertTrue(len == data.length);
    byte[] bb = new byte[(int) len];
    FSDataInputStream fsdis = fc.open(path);
    try {
      fsdis.readFully(bb);
    } finally {
      fsdis.close();
    }
    assertArrayEquals(data, bb);
  }

}

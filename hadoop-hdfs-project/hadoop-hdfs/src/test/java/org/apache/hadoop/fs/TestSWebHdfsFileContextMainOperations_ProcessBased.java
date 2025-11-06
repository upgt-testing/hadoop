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

import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import static org.apache.hadoop.fs.FileContextTestHelper.getDefaultBlockSize;
import static org.apache.hadoop.fs.FileContextTestHelper.getFileData;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSWebHdfsFileContextMainOperations}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Test of FileContext apis on SWebhdfs (Secure WebHDFS with HTTPS).
 *
 * @see TestSWebHdfsFileContextMainOperations Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestSWebHdfsFileContextMainOperations_ProcessBased
    extends TestWebHdfsFileContextMainOperations {

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
  private Path defaultWorkingDirectory;
  private String keystoresDir;
  private String sslConfDir;
  protected URI webhdfsUrl;

  private HdfsConfiguration conf;

  private final String BASEDIR = GenericTestUtils
      .getTempPath(TestSWebHdfsFileContextMainOperations_ProcessBased.class.getSimpleName());
  protected int numBlocks = 2;
  protected final byte[] data = getFileData(numBlocks,
      getDefaultBlockSize());

  @Before
  public void setUp() throws Exception {

    File base = new File(BASEDIR);
    FileUtil.fullyDelete(base);
    base.mkdirs();
    keystoresDir = new File(BASEDIR).getAbsolutePath();

    conf = new HdfsConfiguration();

    try {
      sslConfDir = KeyStoreTestUtil
          .getClasspathDir(TestSWebHdfsFileContextMainOperations_ProcessBased.class);
      KeyStoreTestUtil.setupSSLConfig(keystoresDir, sslConfDir, conf, false);
      conf.set(DFSConfigKeys.DFS_CLIENT_HTTPS_KEYSTORE_RESOURCE_KEY,
          KeyStoreTestUtil.getClientSSLConfigFileName());
      conf.set(DFSConfigKeys.DFS_SERVER_HTTPS_KEYSTORE_RESOURCE_KEY,
          KeyStoreTestUtil.getServerSSLConfigFileName());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    conf.set(DFSConfigKeys.DFS_HTTP_POLICY_KEY, "HTTPS_ONLY");
    conf.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, "localhost:0");
    conf.set(DFSConfigKeys.DFS_DATANODE_HTTPS_ADDRESS_KEY, "localhost:0");
    conf.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "DEFAULT_AND_LOCALHOST");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();

    webhdfsUrl = new URI(SWebHdfs.SCHEME + "://" + cluster.getConfiguration(0)
        .get(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY));

    fc = FileContext.getFileContext(webhdfsUrl, conf);
    defaultWorkingDirectory = fc.makeQualified(new Path(
        "/user/" + UserGroupInformation.getCurrentUser().getShortUserName()));
    fc.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);

    // Upgrade checkpoint after cluster start
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
    FileUtil.fullyDelete(new File(BASEDIR));
    KeyStoreTestUtil.cleanupSSLConfig(keystoresDir, sslConfDir);
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

  @Override
  protected FileContextTestHelper createFileContextHelper() {
    return new FileContextTestHelper("/tmp/TestSWebHdfsFileContextMainOperations_ProcessBased");
  }

  @Override
  public URI getWebhdfsUrl() {
    return webhdfsUrl;
  }
}

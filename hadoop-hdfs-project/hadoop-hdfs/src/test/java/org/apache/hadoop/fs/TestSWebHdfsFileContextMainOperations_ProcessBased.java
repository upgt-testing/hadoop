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
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
public class TestSWebHdfsFileContextMainOperations_ProcessBased
    extends TestWebHdfsFileContextMainOperations {

  private static ProcessBasedMiniDFSCluster cluster;
  private static Path defaultWorkingDirectory;
  private static String keystoresDir;
  private static String sslConfDir;
  protected static URI webhdfsUrl;

  private static final HdfsConfiguration CONF = new HdfsConfiguration();

  private static final String BASEDIR = GenericTestUtils
      .getTempPath(TestSWebHdfsFileContextMainOperations_ProcessBased.class.getSimpleName());
  protected static int numBlocks = 2;
  protected static final byte[] data = getFileData(numBlocks,
      getDefaultBlockSize());

  @BeforeClass
  public static void clusterSetupAtBeginning()
      throws IOException, LoginException, URISyntaxException {

    File base = new File(BASEDIR);
    FileUtil.fullyDelete(base);
    base.mkdirs();
    keystoresDir = new File(BASEDIR).getAbsolutePath();
    try {
      sslConfDir = KeyStoreTestUtil
          .getClasspathDir(TestSWebHdfsFileContextMainOperations_ProcessBased.class);
      KeyStoreTestUtil.setupSSLConfig(keystoresDir, sslConfDir, CONF, false);
      CONF.set(DFSConfigKeys.DFS_CLIENT_HTTPS_KEYSTORE_RESOURCE_KEY,
          KeyStoreTestUtil.getClientSSLConfigFileName());
      CONF.set(DFSConfigKeys.DFS_SERVER_HTTPS_KEYSTORE_RESOURCE_KEY,
          KeyStoreTestUtil.getServerSSLConfigFileName());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    CONF.set(DFSConfigKeys.DFS_HTTP_POLICY_KEY, "HTTPS_ONLY");
    CONF.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, "localhost:0");
    CONF.set(DFSConfigKeys.DFS_DATANODE_HTTPS_ADDRESS_KEY, "localhost:0");
    CONF.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, "DEFAULT_AND_LOCALHOST");

    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(CONF).numDataNodes(2).build();
      cluster.waitClusterUp();
    } catch (TimeoutException e) {
      throw new IOException("Cluster startup timed out", e);
    }

    webhdfsUrl = new URI(SWebHdfs.SCHEME + "://" + cluster.getConfiguration(0)
        .get(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY));

    fc = FileContext.getFileContext(webhdfsUrl, CONF);
    defaultWorkingDirectory = fc.makeQualified(new Path(
        "/user/" + UserGroupInformation.getCurrentUser().getShortUserName()));
    fc.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);

  }

  @Override
  protected FileContextTestHelper createFileContextHelper() {
    return new FileContextTestHelper("/tmp/TestSWebHdfsFileContextMainOperations_ProcessBased");
  }

  @Override
  public URI getWebhdfsUrl() {
    return webhdfsUrl;
  }

  @AfterClass
  public static void ClusterShutdownAtEnd() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
    FileUtil.fullyDelete(new File(BASEDIR));
    KeyStoreTestUtil.cleanupSSLConfig(keystoresDir, sslConfDir);
  }
}

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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestHttpsFileSystem}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests secure WebHDFS (swebhdfs://) with HTTPS-only configuration.
 *
 * @see TestHttpsFileSystem Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestHttpsFileSystem_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final String BASEDIR =
      GenericTestUtils.getTempPath(TestHttpsFileSystem_ProcessBased.class.getSimpleName());

  private String keystoresDir;
  private String sslConfDir;
  private String nnAddr;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      UpgradeCheckpoints.AFTER_WRITE
    );
  }

  @Override
  @Before
  public void setupTest() throws Exception {
    super.setupTest();

    conf.set(DFSConfigKeys.DFS_HTTP_POLICY_KEY, HttpConfig.Policy.HTTPS_ONLY.name());
    conf.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, "localhost:0");
    conf.set(DFSConfigKeys.DFS_DATANODE_HTTPS_ADDRESS_KEY, "localhost:0");

    File base = new File(BASEDIR);
    FileUtil.fullyDelete(base);
    base.mkdirs();
    keystoresDir = new File(BASEDIR).getAbsolutePath();
    sslConfDir = KeyStoreTestUtil.getClasspathDir(TestHttpsFileSystem_ProcessBased.class);

    KeyStoreTestUtil.setupSSLConfig(keystoresDir, sslConfDir, conf, false);
    conf.set(DFSConfigKeys.DFS_CLIENT_HTTPS_KEYSTORE_RESOURCE_KEY,
        KeyStoreTestUtil.getClientSSLConfigFileName());
    conf.set(DFSConfigKeys.DFS_SERVER_HTTPS_KEYSTORE_RESOURCE_KEY,
        KeyStoreTestUtil.getServerSSLConfigFileName());

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    OutputStream os = fs.create(new Path("/test"));
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);
    os.write(23);
    checkpoint(UpgradeCheckpoints.AFTER_WRITE);
    os.close();

    // Get HTTPS address from configuration
    // Note: Cannot access NameNode object directly in ProcessBased cluster.
    // The HTTPS address is read from the NameNode's configuration.
    nnAddr = conf.get(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY);
    conf.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, nnAddr);
  }

  @Override
  @After
  public void tearDownTest() {
    super.tearDownTest();

    try {
      FileUtil.fullyDelete(new File(BASEDIR));
      KeyStoreTestUtil.cleanupSSLConfig(keystoresDir, sslConfDir);
    } catch (Exception e) {
      // Log but don't fail test
    }
  }

  @Test
  public void testSWebHdfsFileSystem() throws Exception {
    FileSystem webFs = WebHdfsTestUtil.getWebHdfsFileSystem(conf, "swebhdfs");
    final Path f = new Path("/testswebhdfs");
    FSDataOutputStream os = webFs.create(f);
    os.write(23);
    os.close();
    Assert.assertTrue(webFs.exists(f));
    InputStream is = webFs.open(f);
    Assert.assertEquals(23, is.read());
    is.close();
    webFs.close();
  }
}

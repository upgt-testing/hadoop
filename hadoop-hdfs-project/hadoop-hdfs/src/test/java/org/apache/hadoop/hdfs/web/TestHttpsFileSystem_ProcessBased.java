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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.security.ssl.KeyStoreTestUtil;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
public class TestHttpsFileSystem_ProcessBased {
  private static final String BASEDIR =
      GenericTestUtils.getTempPath(TestHttpsFileSystem_ProcessBased.class.getSimpleName());

  private static ProcessBasedMiniDFSCluster cluster;
  private static Configuration conf;

  private static String keystoresDir;
  private static String sslConfDir;
  private static String nnAddr;

  @BeforeClass
  public static void setUp() throws Exception {
    conf = new Configuration();
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

    OutputStream os = cluster.getFileSystem().create(new Path("/test"));
    os.write(23);
    os.close();

    // Get HTTPS address from configuration
    // Note: Cannot access NameNode object directly in ProcessBased cluster.
    // The HTTPS address is read from the NameNode's configuration.
    nnAddr = conf.get(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY);
    conf.set(DFSConfigKeys.DFS_NAMENODE_HTTPS_ADDRESS_KEY, nnAddr);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
    FileUtil.fullyDelete(new File(BASEDIR));
    KeyStoreTestUtil.cleanupSSLConfig(keystoresDir, sslConfDir);
  }

  @Test
  public void testSWebHdfsFileSystem() throws Exception {
    FileSystem fs = WebHdfsTestUtil.getWebHdfsFileSystem(conf, "swebhdfs");
    final Path f = new Path("/testswebhdfs");
    FSDataOutputStream os = fs.create(f);
    os.write(23);
    os.close();
    Assert.assertTrue(fs.exists(f));
    InputStream is = fs.open(f);
    Assert.assertEquals(23, is.read());
    is.close();
    fs.close();
  }
}

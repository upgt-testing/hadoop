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
package org.apache.hadoop.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.PathUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestUrlStreamHandler}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestUrlStreamHandler Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestUrlStreamHandler_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final File TEST_ROOT_DIR =
      PathUtils.getTestDir(TestUrlStreamHandler_ProcessBased.class);

  private static final FsUrlStreamHandlerFactory HANDLER_FACTORY
      = new FsUrlStreamHandlerFactory();

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_WRITE,
      UpgradeCheckpoints.AFTER_CLOSE
    );
  }

  @BeforeClass
  public static void setupHandler() {

    // Setup our own factory
    // setURLStreamHandlerFactor is can be set at most once in the JVM
    // the new URLStreamHandler is valid for all tests cases
    // in TestStreamHandler
    URL.setURLStreamHandlerFactory(HANDLER_FACTORY);
  }

  /**
   * Test opening and reading from an InputStream through a hdfs:// URL.
   * <p>
   * First generate a file with some content through the FileSystem API, then
   * try to open and read the file through the URL stream API.
   *
   * @throws IOException
   */
  @Test
  public void testDfsUrls() throws Exception {

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs = cluster.getFileSystem();
    Path filePath = new Path("/thefile");

    byte[] fileContent = new byte[1024];
    for (int i = 0; i < fileContent.length; ++i)
      fileContent[i] = (byte) i;

    // First create the file through the FileSystem API
    OutputStream os = fs.create(filePath);
    os.write(fileContent);
    checkpoint(UpgradeCheckpoints.AFTER_WRITE);
    os.close();
    checkpoint(UpgradeCheckpoints.AFTER_CLOSE);

    // Second, open and read the file content through the URL API
    URI uri = fs.getUri();
    URL fileURL =
        new URL(uri.getScheme(), uri.getHost(), uri.getPort(), filePath
            .toString());

    InputStream is = fileURL.openStream();
    assertNotNull(is);

    byte[] bytes = new byte[4096];
    assertEquals(1024, is.read(bytes));
    is.close();

    for (int i = 0; i < fileContent.length; ++i)
      assertEquals(fileContent[i], bytes[i]);

    // Cleanup: delete the file
    fs.delete(filePath, false);
  }

  /**
   * Test opening and reading from an InputStream through a file:// URL.
   *
   * @throws IOException
   * @throws URISyntaxException
   */
  @Test
  public void testFileUrls() throws IOException, URISyntaxException {
    // URLStreamHandler is already set in JVM by testDfsUrls()

    // Locate the test temporary directory.
    if (!TEST_ROOT_DIR.exists()) {
      if (!TEST_ROOT_DIR.mkdirs())
        throw new IOException("Cannot create temporary directory: " + TEST_ROOT_DIR);
    }

    File tmpFile = new File(TEST_ROOT_DIR, "thefile");
    URI uri = tmpFile.toURI();

    FileSystem localFs = FileSystem.get(uri, conf);

    try {
      byte[] fileContent = new byte[1024];
      for (int i = 0; i < fileContent.length; ++i)
        fileContent[i] = (byte) i;

      // First create the file through the FileSystem API
      OutputStream os = localFs.create(new Path(uri.getPath()));
      os.write(fileContent);
      os.close();

      // Second, open and read the file content through the URL API.
      URL fileURL = uri.toURL();

      InputStream is = fileURL.openStream();
      assertNotNull(is);

      byte[] bytes = new byte[4096];
      assertEquals(1024, is.read(bytes));
      is.close();

      for (int i = 0; i < fileContent.length; ++i)
        assertEquals(fileContent[i], bytes[i]);

      // Cleanup: delete the file
      localFs.delete(new Path(uri.getPath()), false);

    } finally {
      localFs.close();
    }

  }

  @Test
  public void testHttpDefaultHandler() throws Throwable {
    assertNull("Handler for HTTP is the Hadoop one",
        HANDLER_FACTORY.createURLStreamHandler("http"));
  }

  @Test
  public void testHttpsDefaultHandler() throws Throwable {
    assertNull("Handler for HTTPS is the Hadoop one",
        HANDLER_FACTORY.createURLStreamHandler("https"));
  }

  @Test
  public void testUnknownProtocol() throws Throwable {
    assertNull("Unknown protocols are not handled",
        HANDLER_FACTORY.createURLStreamHandler("gopher"));
  }

}

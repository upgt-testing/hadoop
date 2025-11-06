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
package org.apache.hadoop.hdfs.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.crypto.CryptoCodec;
import org.apache.hadoop.crypto.CryptoStreamsTestBase;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.crypto.CryptoFSDataInputStream;
import org.apache.hadoop.fs.crypto.CryptoFSDataOutputStream;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestHdfsCryptoStreams}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestHdfsCryptoStreams Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestHdfsCryptoStreams_ProcessBased extends CryptoStreamsTestBase {
  private ProcessBasedMiniDFSCluster dfsCluster;
  private FileSystem testFs;
  private int pathCount = 0;
  private Path path;
  private Path file;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE
    );
  }

  @Before
  @Override
  public void setUp() throws IOException {
    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.hdfs.HdfsConfiguration();
    try {
      dfsCluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
      dfsCluster.waitClusterUp();
    } catch (Exception e) {
      throw new IOException("Failed to start cluster", e);
    }
    testFs = dfsCluster.getFileSystem();
    codec = CryptoCodec.getInstance(conf);

    ++pathCount;
    path = new Path("/p" + pathCount);
    file = new Path(path, "file");
    FileSystem.mkdirs(testFs, path, FsPermission.createImmutable((short) 0700));

    super.setUp();
  }

  @After
  public void cleanUp() throws Exception {
    if (testFs != null) {
      try {
        testFs.delete(path, true);
      } catch (Exception e) {
        // Ignore
      }
      try {
        testFs.close();
      } catch (Exception e) {
        // Ignore
      }
    }
    if (dfsCluster != null) {
      try {
        dfsCluster.shutdown(true);
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Override
  protected OutputStream getOutputStream(int bufferSize, byte[] key, byte[] iv)
      throws IOException {
    return new CryptoFSDataOutputStream(testFs.create(file), codec, bufferSize,
        key, iv);
  }

  @Override
  protected InputStream getInputStream(int bufferSize, byte[] key, byte[] iv)
      throws IOException {
    return new CryptoFSDataInputStream(testFs.open(file), codec, bufferSize, key,
        iv);
  }
}

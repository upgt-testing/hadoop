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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestGetFileChecksum}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests file checksum functionality including checksums for files under
 * construction and checksums at different file lengths during append operations.
 *
 * @see TestGetFileChecksum Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestGetFileChecksum_ProcessBased extends ProcessBasedUpgradeTestBase {

  private static final int BLOCKSIZE = 1024;
  private static final short REPLICATION = 3;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_UNDER_CONSTRUCTION_TEST",
      "AFTER_FIRST_CHECKSUM_TEST",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  public void testGetFileChecksum(DistributedFileSystem dfs, final Path foo,
      final int appendLength) throws Exception {
    final int appendRounds = 16;
    FileChecksum[] fc = new FileChecksum[appendRounds + 1];
    DFSTestUtil.createFile(dfs, foo, appendLength, REPLICATION, 0L);
    fc[0] = dfs.getFileChecksum(foo);
    for (int i = 0; i < appendRounds; i++) {
      DFSTestUtil.appendFile(dfs, foo, appendLength);
      fc[i + 1] = dfs.getFileChecksum(foo);
    }

    for (int i = 0; i < appendRounds + 1; i++) {
      FileChecksum checksum = dfs.getFileChecksum(foo, appendLength * (i+1));
      Assert.assertTrue(checksum.equals(fc[i]));
    }
  }

  @Test
  public void testGetFileChecksumForBlocksUnderConstruction() throws Exception {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    DistributedFileSystem dfs = cluster.getFileSystem();

    try {
      FSDataOutputStream file = dfs.create(new Path("/testFile"));
      file.write("Performance Testing".getBytes());
      dfs.getFileChecksum(new Path("/testFile"));
      fail("getFileChecksum should fail for files "
          + "with blocks under construction");
    } catch (IOException ie) {
      Assert.assertTrue(ie.getMessage().contains(
          "Fail to get checksum, since file /testFile "
              + "is under construction."));
    }

    checkpoint("AFTER_FILE_UNDER_CONSTRUCTION_TEST");
  }

  @Test
  public void testGetFileChecksum() throws Exception {
    conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCKSIZE);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    DistributedFileSystem dfs = cluster.getFileSystem();

    testGetFileChecksum(dfs, new Path("/foo"), BLOCKSIZE / 4);

    checkpoint("AFTER_FIRST_CHECKSUM_TEST");

    testGetFileChecksum(dfs, new Path("/bar"), BLOCKSIZE / 4 - 1);

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

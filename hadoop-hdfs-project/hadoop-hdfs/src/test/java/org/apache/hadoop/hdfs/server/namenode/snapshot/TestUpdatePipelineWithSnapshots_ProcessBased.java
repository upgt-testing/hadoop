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
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSOutputStream;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.io.IOUtils;
import static org.apache.hadoop.test.GenericTestUtils.assertExceptionContains;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestUpdatePipelineWithSnapshots}.
 *
 * Tests pipeline recovery with snapshots using process-based cluster
 * to enable multi-version upgrade scenarios.
 *
 * @see TestUpdatePipelineWithSnapshots Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestUpdatePipelineWithSnapshots_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_FIRST_WRITE",
      "AFTER_FIRST_FLUSH",
      "AFTER_SNAPSHOT_CREATE",
      "AFTER_BLOCK_INFO_READ",
      "AFTER_FILE_DELETE",
      "BEFORE_PIPELINE_UPDATE",
      "AFTER_PIPELINE_UPDATE",
      "BEFORE_NAMENODE_RESTART"
    );
  }

  // Regression test for HDFS-6647.
  @Test
  public void testUpdatePipelineAfterDelete() throws Exception {
    Configuration conf = new HdfsConfiguration();
    Path file = new Path("/test-file");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();
    DistributedFileSystem dfs = (DistributedFileSystem) fs;

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Get NameNode RPC interface through client
    ClientProtocol namenode = dfs.getClient().getNamenode();

    FSDataOutputStream fsOut = null;
    DFSOutputStream out = null;
    try {
      // Create a file and make sure a block is allocated for it.
      fsOut = fs.create(file);
      out = (DFSOutputStream)(fsOut.getWrappedStream());
      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      out.write(1);
      checkpoint("AFTER_FIRST_WRITE");

      out.hflush();
      checkpoint("AFTER_FIRST_FLUSH");

      // Create a snapshot that includes the file.
      SnapshotTestHelper.createSnapshot(dfs, new Path("/"), "s1");
      checkpoint("AFTER_SNAPSHOT_CREATE");

      // Grab the block info of this file for later use.
      FSDataInputStream in = null;
      ExtendedBlock oldBlock = null;
      try {
        in = fs.open(file);
        oldBlock = DFSTestUtil.getAllBlocks(in).get(0).getBlock();
      } finally {
        IOUtils.closeStream(in);
      }
      checkpoint("AFTER_BLOCK_INFO_READ");

      // Allocate a new block ID/gen stamp so we can simulate pipeline recovery.
      String clientName = dfs.getClient().getClientName();
      LocatedBlock newLocatedBlock = namenode.updateBlockForPipeline(
          oldBlock, clientName);
      ExtendedBlock newBlock = new ExtendedBlock(oldBlock.getBlockPoolId(),
          oldBlock.getBlockId(), oldBlock.getNumBytes(),
          newLocatedBlock.getBlock().getGenerationStamp());

      // Delete the file from the present FS. It will still exist in the
      // previously-created snapshot. This will log an OP_DELETE for the
      // file in question.
      fs.delete(file, true);
      checkpoint("AFTER_FILE_DELETE");

      // Simulate a pipeline recovery, wherein a new block is allocated
      // for the existing block, resulting in an OP_UPDATE_BLOCKS being
      // logged for the file in question.
      checkpoint("BEFORE_PIPELINE_UPDATE");
      try {
        namenode.updatePipeline(clientName, oldBlock, newBlock,
            newLocatedBlock.getLocations(), newLocatedBlock.getStorageIDs());
      } catch (IOException ioe) {
        // normal - expected exception
        assertExceptionContains(
            "does not exist or it is not under construction", ioe);
      }
      checkpoint("AFTER_PIPELINE_UPDATE");

      // Make sure the NN can restart with the edit logs as we have them now.
      checkpoint("BEFORE_NAMENODE_RESTART");
      cluster.restartNameNode(0);
      cluster.waitClusterUp();
      fs = cluster.getFileSystem();

    } finally {
      IOUtils.closeStream(out);
      IOUtils.closeStream(fsOut);
    }
  }
}

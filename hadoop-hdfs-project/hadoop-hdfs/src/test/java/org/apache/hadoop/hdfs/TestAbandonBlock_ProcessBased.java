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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.hdfs.protocol.QuotaExceededException;
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
 * ProcessBasedMiniDFSCluster version of {@link TestAbandonBlock}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Test abandoning blocks, which clients do on pipeline creation failure.
 *
 * @see TestAbandonBlock Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestAbandonBlock_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestAbandonBlock_ProcessBased.class);

  private static final String FILE_NAME_PREFIX
      = "/" + TestAbandonBlock_ProcessBased.class.getSimpleName() + "_";

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
      "AFTER_ABANDON_BLOCK",
      "AFTER_NAMENODE_RESTART",
      "BEFORE_VERIFICATION"
    );
  }

  @Test
  /** Abandon a block while creating a file */
  public void testAbandonBlock() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    String src = FILE_NAME_PREFIX + "foo";

    // Start writing a file but do not close it
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)1, 512L);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    for (int i = 0; i < 1024; i++) {
      fout.write(123);
    }
    checkpoint("AFTER_FIRST_WRITE");

    fout.hflush();
    checkpoint("AFTER_FIRST_FLUSH");

    long fileId = ((DFSOutputStream)fout.getWrappedStream()).getFileId();

    // Now abandon the last block
    DFSClient dfsclient = DFSClientAdapter.getDFSClient((DistributedFileSystem) fs);
    LocatedBlocks blocks =
      dfsclient.getNamenode().getBlockLocations(src, 0, Integer.MAX_VALUE);
    int orginalNumBlocks = blocks.locatedBlockCount();
    LocatedBlock b = blocks.getLastLocatedBlock();
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    // call abandonBlock again to make sure the operation is idempotent
    dfsclient.getNamenode().abandonBlock(b.getBlock(), fileId, src,
        dfsclient.clientName);

    checkpoint("AFTER_ABANDON_BLOCK");

    // And close the file
    fout.close();

    // Close cluster and check the block has been abandoned after restart
    cluster.restartNameNode(0);
    checkpoint("AFTER_NAMENODE_RESTART");

    blocks = dfsclient.getNamenode().getBlockLocations(src, 0,
        Integer.MAX_VALUE);
    checkpoint("BEFORE_VERIFICATION");

    Assert.assertEquals("Blocks " + b + " has not been abandoned.",
        orginalNumBlocks, blocks.locatedBlockCount() + 1);
  }

  @Test
  /** Make sure that the quota is decremented correctly when a block is abandoned */
  public void testQuotaUpdatedWhenBlockAbandoned() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Setting diskspace quota to 3MB
    fs.setQuota(new Path("/"), HdfsConstants.QUOTA_DONT_SET, 3 * 1024 * 1024);

    // Start writing a file with 2 replicas to ensure each datanode has one.
    // Block Size is 1MB.
    String src = FILE_NAME_PREFIX + "test_quota1";
    FSDataOutputStream fout = fs.create(new Path(src), true, 4096, (short)2, 1024 * 1024);
    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    for (int i = 0; i < 1024; i++) {
      fout.writeByte(123);
    }
    checkpoint("AFTER_FIRST_WRITE");

    // Shutdown one datanode, causing the block abandonment.
    cluster.shutdownDataNode(0);
    checkpoint("AFTER_DATANODE_SHUTDOWN");

    // Close the file, new block will be allocated with 2MB pending size.
    try {
      fout.close();
    } catch (QuotaExceededException e) {
      fail("Unexpected quota exception when closing fout");
    }

    checkpoint("BEFORE_VERIFICATION");
  }
}

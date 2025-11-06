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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.util.ThreadUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSClientExcludedNodes}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * These tests make sure that DFSClient excludes writing data to
 * a DN properly in case of errors.
 *
 * @see TestDFSClientExcludedNodes Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestDFSClientExcludedNodes_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_DN_SHUTDOWN",
      "AFTER_WRITE"
    );
  }

  @After
  public void tearDown() {
    super.tearDownTest();
  }

  @Test(timeout=60000)
  public void testExcludedNodes() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(3).build();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem fs = cluster.getFileSystem();
    Path filePath = new Path("/testExcludedNodes");

    // kill a datanode
    cluster.shutdownDataNode(AppendTestUtil.nextInt(3));
    checkpoint("AFTER_DN_SHUTDOWN");

    OutputStream out = fs.create(
        filePath,
        true,
        4096,
        (short) 3,
        fs.getDefaultBlockSize(filePath)
    );
    out.write(20);
    checkpoint("AFTER_WRITE");

    try {
      out.close();
    } catch (Exception e) {
      fail("Single DN failure should not result in a block abort: \n" +
          e.getMessage());
    }
  }

  @Test(timeout=60000)
  public void testExcludedNodesForgiveness() throws Exception {
    conf = new HdfsConfiguration();
    // Forgive nodes in under 2.5s for this test case.
    conf.setLong(
        HdfsClientConfigKeys.Write.EXCLUDE_NODES_CACHE_EXPIRY_INTERVAL_KEY,
        2500);
    // We'll be using a 512 bytes block size just for tests
    // so making sure the checksum bytes too match it.
    conf.setInt("io.bytes.per.checksum", 512);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(3).build();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    FileSystem fs = cluster.getFileSystem();
    Path filePath = new Path("/testForgivingExcludedNodes");

    // 256 bytes data chunk for writes
    byte[] bytes = new byte[256];
    for (int index=0; index<bytes.length; index++) {
      bytes[index] = '0';
    }

    // File with a 512 bytes block size
    FSDataOutputStream out = fs.create(filePath, true, 4096, (short) 3, 512);

    // Write a block to all 3 DNs (2x256bytes).
    out.write(bytes);
    out.write(bytes);
    out.hflush();
    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    // Remove two DNs, to put them into the exclude list.
    cluster.shutdownDataNode(2);
    cluster.shutdownDataNode(1);
    checkpoint("AFTER_DN_SHUTDOWN");

    // Write another block.
    // At this point, we have two nodes already in excluded list.
    out.write(bytes);
    out.write(bytes);
    out.hflush();
    checkpoint("AFTER_WRITE");

    // Bring back the older DNs, since they are gonna be forgiven only
    // afterwards of this previous block write.
    cluster.restartDataNode(1);
    cluster.restartDataNode(2);
    cluster.waitClusterUp();

    // Sleep for 5s, to let the excluded nodes be expired
    // from the excludes list (i.e. forgiven after the configured wait period).
    // [Sleeping just in case the restart of the DNs completed < 5s cause
    // otherwise, we'll end up quickly excluding those again.]
    ThreadUtil.sleepAtLeastIgnoreInterrupts(5000);

    // Terminate the last good DN, to assert that there's no
    // single-DN-available scenario, caused by not forgiving the other
    // two by now.
    cluster.shutdownDataNode(0);

    try {
      // Attempt writing another block, which should still pass
      // cause the previous two should have been forgiven by now,
      // while the last good DN added to excludes this time.
      out.write(bytes);
      out.hflush();
      out.close();
    } catch (Exception e) {
      fail("Excluded DataNodes should be forgiven after a while and " +
           "not cause file writing exception of: '" + e.getMessage() + "'");
    }
  }
}

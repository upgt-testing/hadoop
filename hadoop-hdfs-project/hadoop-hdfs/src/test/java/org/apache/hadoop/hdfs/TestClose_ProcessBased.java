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

import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestClose}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests OutputStream close behavior: write-after-close fails, double-close succeeds.
 *
 * @see TestClose Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestClose_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_FIRST_WRITE",
      "AFTER_FIRST_CLOSE",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test
  public void testWriteAfterClose() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final byte[] data = "foo".getBytes();

    OutputStream out = fs.create(new Path("/test"));

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    out.write(data);

    checkpoint("AFTER_FIRST_WRITE");

    out.close();

    checkpoint("AFTER_FIRST_CLOSE");

    try {
      // Should fail.
      out.write(data);
      fail("Should not have been able to write more data after file is closed.");
    } catch (ClosedChannelException cce) {
      // We got the correct exception. Ignoring.
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // Should succeed. Double closes are OK.
    out.close();
  }
}

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

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestWriteConfigurationToDFS}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Regression test for HDFS-1542, a deadlock between the main thread
 * and the DFSOutputStream.DataStreamer thread caused because
 * Configuration.writeXML holds a lock on itself while writing to DFS.
 *
 * @see TestWriteConfigurationToDFS Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestWriteConfigurationToDFS_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_CONFIG_SETUP",
      "AFTER_XML_WRITE",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test(timeout=60000)
  public void testWriteConf() throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 4096);
    System.out.println("Setting conf in: " + System.identityHashCode(conf));

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    OutputStream os = null;
    try {
      Path filePath = new Path("/testWriteConf.xml");
      os = fs.create(filePath);

      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      StringBuilder longString = new StringBuilder();
      for (int i = 0; i < 100000; i++) {
        longString.append("hello");
      } // 500KB
      conf.set("foobar", longString.toString());

      checkpoint("AFTER_CONFIG_SETUP");

      conf.writeXml(os);

      checkpoint("AFTER_XML_WRITE");

      os.close();
      os = null;

      checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    } finally {
      IOUtils.cleanupWithLogger(null, os);
      // fs and cluster cleanup handled by @After in base class
    }
  }
}

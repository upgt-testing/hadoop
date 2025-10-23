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
package org.apache.hadoop.hdfs.server.namenode;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSClient;
import org.apache.hadoop.hdfs.DFSUtilClient;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestINodeFile}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class contains selective transformations of test methods from the
 * original TestINodeFile class.
 *
 * @see TestINodeFile Original test using MiniDFSCluster
 */
public class TestINodeFile_ProcessBased {
  public static final Logger LOG = LoggerFactory.getLogger(TestINodeFile_ProcessBased.class);

  @Test
  public void testDotdotInodePath() throws Exception {
    final Configuration conf = new Configuration();

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = null;
    DFSClient client = null;
    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(1)
          .format(true)
          .build();
      cluster.waitClusterUp();
      final DistributedFileSystem hdfs = cluster.getFileSystem();

      // TRANSFORMATION: Instead of accessing FSDirectory directly,
      // use DFSClient API to get inode IDs from HdfsFileStatus
      final Path dir = new Path("/dir");
      hdfs.mkdirs(dir);

      client = new DFSClient(DFSUtilClient.getNNAddress(conf), conf);

      // Get inode ID via client API (HdfsFileStatus has getFileId())
      long dirId = client.getFileInfo(dir.toString()).getFileId();
      long parentId = client.getFileInfo("/").getFileId();

      String testPath = "/.reserved/.inodes/" + dirId + "/..";
      HdfsFileStatus status = client.getFileInfo(testPath);
      assertTrue(parentId == status.getFileId());

      // Test root's parent is still root
      testPath = "/.reserved/.inodes/" + parentId + "/..";
      status = client.getFileInfo(testPath);
      assertTrue(parentId == status.getFileId());

    } finally {
      IOUtils.cleanupWithLogger(LOG, client);
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}

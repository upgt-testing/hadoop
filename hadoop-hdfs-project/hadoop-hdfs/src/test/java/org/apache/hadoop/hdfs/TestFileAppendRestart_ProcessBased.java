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

import static org.junit.Assume.assumeNotNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppendRestart}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class contains selective transformations of test methods from the
 * original TestFileAppendRestart class.
 *
 * @see TestFileAppendRestart Original test using MiniDFSCluster
 */
public class TestFileAppendRestart_ProcessBased {
  private static final int BLOCK_SIZE = 4096;

  /**
   * Test to append to the file, when one of datanode in the existing pipeline
   * is down.
   */
  @Test
  public void testAppendWithPipelineRecovery() throws Exception {
    Configuration conf = new Configuration();

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = null;
    FSDataOutputStream out = null;
    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(4)
          // Note: ProcessBasedMiniDFSCluster doesn't support rack configuration
          .allNodesHadoopDistribution(hadoopHome)
          .format(true)
          .build();
      cluster.waitClusterUp();

      DistributedFileSystem fs = cluster.getFileSystem();
      Path path = new Path("/test1");

      out = fs.create(path, true, BLOCK_SIZE, (short) 3, BLOCK_SIZE);
      AppendTestUtil.write(out, 0, 1024);
      out.close();

      cluster.shutdownDataNode(3);
      out = fs.append(path);
      AppendTestUtil.write(out, 1024, 1024);
      out.close();

      cluster.restartNameNode(0);
      AppendTestUtil.check(fs, path, 2048);
    } finally {
      IOUtils.closeStream(out);
      if (null != cluster) {
        cluster.shutdown();
      }
    }
  }
}

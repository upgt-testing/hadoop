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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import static org.apache.hadoop.hdfs.client.HdfsClientConfigKeys.DFS_CLIENT_SOCKET_SEND_BUFFER_SIZE_KEY;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSClientSocketSize}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestDFSClientSocketSize Original test using MiniDFSCluster
 */
public class TestDFSClientSocketSize_ProcessBased {
  private static final Logger LOG = LoggerFactory.getLogger(
      TestDFSClientSocketSize_ProcessBased.class);
  static {
    GenericTestUtils.setLogLevel(DataStreamer.LOG, Level.ALL);
  }

  /**
   * Test that the send buffer size default value is 0, in which case the socket
   * will use a TCP auto-tuned value.
   */
  @Test
  public void testDefaultSendBufferSize() throws IOException, TimeoutException {
    final int sendBufferSize = getSendBufferSize(new Configuration());
    LOG.info("If not specified, the auto tuned send buffer size is: {}",
        sendBufferSize);
    assertTrue("Send buffer size should be non-negative value which is " +
        "determined by system (kernel).", sendBufferSize > 0);
  }

  /**
   * Note that {@link java.net.Socket#setSendBufferSize(int)} is only a hint.
   * If this test is flaky it should be ignored.  See HADOOP-13351.
   */
  @Test
  public void testSpecifiedSendBufferSize() throws IOException, TimeoutException {
    final Configuration conf1 = new Configuration();
    conf1.setInt(DFS_CLIENT_SOCKET_SEND_BUFFER_SIZE_KEY, 256 * 1024); // 256 KB
    final int sendBufferSize1 = getSendBufferSize(conf1);

    final Configuration conf2 = new Configuration();
    conf2.setInt(DFS_CLIENT_SOCKET_SEND_BUFFER_SIZE_KEY, 1024);       // 1 KB
    final int sendBufferSize2 = getSendBufferSize(conf2);

    LOG.info("Large buf size is {}, small is {}",
        sendBufferSize1, sendBufferSize2);
    assertTrue("Larger specified send buffer should have effect",
        sendBufferSize1 > sendBufferSize2);
  }

  /**
   * Test that if the send buffer size is 0, the socket will use a TCP
   * auto-tuned value.
   */
  @Test
  public void testAutoTuningSendBufferSize() throws IOException, TimeoutException {
    final Configuration conf = new Configuration();
    conf.setInt(DFS_CLIENT_SOCKET_SEND_BUFFER_SIZE_KEY, 0);
    final int sendBufferSize = getSendBufferSize(conf);
    LOG.info("The auto tuned send buffer size is: {}", sendBufferSize);
    assertTrue("Send buffer size should be non-negative value which is " +
        "determined by system (kernel).", sendBufferSize > 0);
  }

  private int getSendBufferSize(Configuration conf) throws IOException, TimeoutException {
    final ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .build();
    try {
      cluster.waitClusterUp();
      LOG.info("ProcessBasedMiniDFSCluster started.");

      // Get DatanodeInfo via client-side API instead of direct DataNode access
      DistributedFileSystem dfs = cluster.getFileSystem();
      DatanodeInfo[] datanodes = dfs.getDataNodeStats();
      if (datanodes == null || datanodes.length == 0) {
        throw new IOException("No datanodes found in cluster");
      }
      DatanodeInfo firstDatanode = datanodes[0];

      try (Socket socket = DataStreamer.createSocketForPipeline(
          firstDatanode,
          1, dfs.getClient())) {
        return socket.getSendBufferSize();
      }
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

}

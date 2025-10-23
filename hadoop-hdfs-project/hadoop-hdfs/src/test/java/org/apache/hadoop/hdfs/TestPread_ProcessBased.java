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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ChecksumException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestPread}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class contains selective transformations of test methods from the
 * original TestPread class.
 *
 * @see TestPread Original test using MiniDFSCluster
 */
public class TestPread_ProcessBased {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestPread_ProcessBased.class.getName());

  @Test(timeout=30000)
  public void testHedgedReadFromAllDNFailed() throws Exception {
    Configuration conf = new Configuration();
    int numHedgedReadPoolThreads = 5;
    final int hedgedReadTimeoutMillis = 50;

    conf.setInt(HdfsClientConfigKeys.HedgedRead.THREADPOOL_SIZE_KEY,
        numHedgedReadPoolThreads);
    conf.setLong(HdfsClientConfigKeys.HedgedRead.THRESHOLD_MILLIS_KEY,
        hedgedReadTimeoutMillis);
    conf.setInt(HdfsClientConfigKeys.Retry.WINDOW_BASE_KEY, 0);

    // Set up the InjectionHandler
    DFSClientFaultInjector.set(Mockito.mock(DFSClientFaultInjector.class));
    DFSClientFaultInjector injector = DFSClientFaultInjector.get();
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        if (true) {
          LOG.info("-------------- throw Checksum Exception");
          throw new ChecksumException("ChecksumException test", 100);
        }
        return null;
      }
    }).when(injector).fetchFromDatanodeException();

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();
    DistributedFileSystem fileSys = cluster.getFileSystem();
    DFSClient dfsClient = fileSys.getClient();
    FSDataOutputStream output = null;
    DFSInputStream input = null;
    String filename = "/hedgedReadMaxOut.dat";
    DFSHedgedReadMetrics metrics = dfsClient.getHedgedReadMetrics();
    // Metrics instance is static, so we need to reset counts from prior tests.
    metrics.hedgedReadOps.reset();
    try {
      cluster.waitClusterUp();

      Path file = new Path(filename);
      output = fileSys.create(file, (short) 2);
      byte[] data = new byte[64 * 1024];
      output.write(data);
      output.flush();
      output.close();
      byte[] buffer = new byte[64 * 1024];
      input = dfsClient.open(filename);
      input.read(0, buffer, 0, 1024);
      Assert.fail("Reading the block should have thrown BlockMissingException");
    } catch (BlockMissingException e) {
      assertEquals(3, input.getHedgedReadOpsLoopNumForTesting());
      assertTrue(metrics.getHedgedReadOps() == 0);
    } finally {
      Mockito.reset(injector);
      IOUtils.cleanupWithLogger(LOG, input);
      IOUtils.cleanupWithLogger(LOG, output);
      fileSys.close();
      cluster.shutdown();
    }
  }
}

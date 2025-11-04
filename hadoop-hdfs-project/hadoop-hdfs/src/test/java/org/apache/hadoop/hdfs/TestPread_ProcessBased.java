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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.ChecksumException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestPread} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during hedged read operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestPread Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestPread_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestPread_ProcessBased.class.getName());

  /**
   * Define upgrade checkpoints for parameterized test execution.
   *
   * @return collection of checkpoint names
   */
  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        // Baseline - no upgrade
        UpgradeCheckpoints.NO_UPGRADE,

        // Cluster lifecycle
        UpgradeCheckpoints.AFTER_CLUSTER_START,

        // File creation
        UpgradeCheckpoints.AFTER_FILE_CREATE,

        // Write operations
        UpgradeCheckpoints.AFTER_WRITE,
        UpgradeCheckpoints.AFTER_FLUSH,
        "AFTER_OUTPUT_CLOSE",

        // Read operations
        "AFTER_INPUT_OPEN",
        "BEFORE_READ"
    );
  }

  /**
   * Test hedged read from all DataNodes failed with parameterized upgrade checkpoints.
   *
   * <p>This test verifies that when all DataNodes fail to serve a read request,
   * the hedged read mechanism correctly handles the failure. The test injects
   * ChecksumException failures and verifies that the client exhausts all hedged
   * read attempts.
   *
   * <p>With 8 checkpoints, this single test method generates 8 test executions,
   * each testing upgrade at a different point in the read workflow.
   *
   * @throws Exception if test fails
   */
  @Test(timeout=30000)
  public void testHedgedReadFromAllDNFailed() throws Exception {
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

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    DFSClient dfsClient = ((DistributedFileSystem) fs).getClient();
    FSDataOutputStream output = null;
    DFSInputStream input = null;
    String filename = "/hedgedReadMaxOut.dat";
    DFSHedgedReadMetrics metrics = dfsClient.getHedgedReadMetrics();
    // Metrics instance is static, so we need to reset counts from prior tests.
    metrics.hedgedReadOps.reset();

    try {
      Path file = new Path(filename);
      output = fs.create(file, (short) 2);

      checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

      byte[] data = new byte[64 * 1024];
      output.write(data);

      checkpoint(UpgradeCheckpoints.AFTER_WRITE);

      output.flush();

      checkpoint(UpgradeCheckpoints.AFTER_FLUSH);

      output.close();
      output = null;

      checkpoint("AFTER_OUTPUT_CLOSE");

      byte[] buffer = new byte[64 * 1024];
      input = dfsClient.open(filename);

      checkpoint("AFTER_INPUT_OPEN");

      checkpoint("BEFORE_READ");

      input.read(0, buffer, 0, 1024);
      Assert.fail("Reading the block should have thrown BlockMissingException");
    } catch (BlockMissingException e) {
      assertEquals(3, input.getHedgedReadOpsLoopNumForTesting());
      assertTrue(metrics.getHedgedReadOps() == 0);
    } finally {
      Mockito.reset(injector);
      IOUtils.cleanupWithLogger(LOG, input);
      IOUtils.cleanupWithLogger(LOG, output);
      // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
    }
  }
}

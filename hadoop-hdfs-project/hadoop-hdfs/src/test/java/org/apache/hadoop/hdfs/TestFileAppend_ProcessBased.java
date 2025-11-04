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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataOutputStream;
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
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppend} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during file append operations.
 *
 * <p>Each test execution tests ONE specific upgrade checkpoint:
 * <ul>
 *   <li>NO_UPGRADE - baseline test without any upgrade</li>
 *   <li>AFTER_CLUSTER_START - upgrade right after cluster starts</li>
 *   <li>AFTER_FILE_CREATE - upgrade after creating file</li>
 *   <li>... and many more checkpoints throughout the test</li>
 * </ul>
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * @see TestFileAppend Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestFileAppend_ProcessBased extends ProcessBasedUpgradeTestBase {

  /**
   * The upgrade checkpoint for this test execution.
   * Set by JUnit parameterization framework.
   */
  @Parameter
  public String upgradeCheckpoint;

  /**
   * Define upgrade checkpoints for parameterized test execution.
   *
   * <p>Each checkpoint represents a point in the test where an upgrade
   * might occur. The test runs once for each checkpoint.
   *
   * @return collection of checkpoint names
   */
  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
        // Baseline - no upgrade
        UpgradeCheckpoints.NO_UPGRADE,

        // Cluster lifecycle
        UpgradeCheckpoints.AFTER_CLUSTER_START,

        // File operations
        UpgradeCheckpoints.AFTER_FILE_CREATE,

        // Write operations
        UpgradeCheckpoints.AFTER_FIRST_WRITE,
        UpgradeCheckpoints.AFTER_FIRST_FLUSH,
        "AFTER_FIRST_CLOSE",

        // After reopening in append mode
        UpgradeCheckpoints.AFTER_APPEND_REOPEN,

        // Second write sequence
        UpgradeCheckpoints.AFTER_SECOND_WRITE,
        "AFTER_SECOND_HFLUSH_1",
        "AFTER_SECOND_HFLUSH_2",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION,
        UpgradeCheckpoints.BEFORE_FINAL_CLOSE,
        UpgradeCheckpoints.AFTER_VERIFICATION
    );
  }

  private static byte[] fileContents = null;

  /**
   * Verify that the data written to the full blocks are sane.
   * This helper method uses only FileSystem APIs and works with ProcessBasedMiniDFSCluster.
   */
  private void checkFile(DistributedFileSystem fileSys, Path name, int repl)
      throws IOException {
    boolean done = false;

    // wait till all full blocks are confirmed by the datanodes.
    while (!done) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {;}
      done = true;
      BlockLocation[] locations = fileSys.getFileBlockLocations(
          fileSys.getFileStatus(name), 0, AppendTestUtil.FILE_SIZE);
      if (locations.length < AppendTestUtil.NUM_BLOCKS) {
        System.out.println("Number of blocks found " + locations.length);
        done = false;
        continue;
      }
      for (int idx = 0; idx < AppendTestUtil.NUM_BLOCKS; idx++) {
        if (locations[idx].getHosts().length < repl) {
          System.out.println("Block index " + idx + " not yet replicated.");
          done = false;
          break;
        }
      }
    }
    byte[] expected =
        new byte[AppendTestUtil.NUM_BLOCKS * AppendTestUtil.BLOCK_SIZE];
    System.arraycopy(fileContents, 0, expected, 0, expected.length);
    // do a sanity check. Read the file
    // do not check file status since the file is not yet closed.
    AppendTestUtil.checkFullFile(fileSys, name,
        AppendTestUtil.NUM_BLOCKS * AppendTestUtil.BLOCK_SIZE,
        expected, "Read 1", false);
  }

  /**
   * Test a simple flush on a simple HDFS file with parameterized upgrade checkpoints.
   *
   * <p>This test performs file writes with flush operations and tests upgrades
   * at various points in the workflow. The specific upgrade point is determined
   * by the {@link #upgradeCheckpoint} parameter.
   *
   * <p>The test workflow:
   * <ol>
   *   <li>Create cluster and file</li>
   *   <li>Write and flush first half of data</li>
   *   <li>Close stream, potentially upgrade, reopen in append mode</li>
   *   <li>Write and flush second half of data</li>
   *   <li>Verify data integrity</li>
   * </ol>
   *
   * <p>With 13 checkpoints, this single test method generates 13 test executions,
   * each testing upgrade at a different point in the workflow.
   *
   * <p><b>Note:</b> Cluster and FileSystem cleanup is handled automatically by
   * {@link ProcessBasedUpgradeTestBase} @After method. No try-finally block needed.
   *
   * @throws Exception if test fails
   */
  @Test
  public void testSimpleFlush() throws Exception {
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    // Create cluster - uses conf from base class
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create file
    Path file1 = new Path("/simpleFlush.dat");
    FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
    System.out.println("Created file simpleFlush.dat");

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // Write first half
    int mid = AppendTestUtil.FILE_SIZE / 2;
    stm.write(fileContents, 0, mid);

    checkpoint(UpgradeCheckpoints.AFTER_FIRST_WRITE);

    stm.hflush();
    System.out.println("Wrote and Flushed first part of file.");

    checkpoint(UpgradeCheckpoints.AFTER_FIRST_FLUSH);

    // Close stream before potential upgrade
    // IMPORTANT: Must close before checkpoint to handle pipeline break
    stm.close();
    System.out.println("Closed stream before potential upgrade");

    checkpoint("AFTER_FIRST_CLOSE");

    // Reopen in append mode (needed regardless of whether upgrade happened)
    stm = fs.append(file1);
    System.out.println("Reopened file in append mode");

    checkpoint(UpgradeCheckpoints.AFTER_APPEND_REOPEN);

    // Write second half
    stm.write(fileContents, mid, AppendTestUtil.FILE_SIZE - mid);
    System.out.println("Written second part of file");

    checkpoint(UpgradeCheckpoints.AFTER_SECOND_WRITE);

    stm.hflush();

    checkpoint("AFTER_SECOND_HFLUSH_1");

    stm.hflush();
    System.out.println("Wrote and Flushed second part of file.");

    checkpoint("AFTER_SECOND_HFLUSH_2");

    // Verify blocks before final close
    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    checkFile(fs, file1, 1);

    checkpoint(UpgradeCheckpoints.BEFORE_FINAL_CLOSE);

    stm.close();
    System.out.println("Closed file.");

    // Final verification
    AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
        fileContents, "Read 2");

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);

    // Cleanup handled by @After in ProcessBasedUpgradeTestBase
  }

  /**
   * Test that file data can be flushed with parameterized upgrade checkpoints.
   *
   * <p>This test performs many small writes with flushes and tests upgrades
   * at various points. The specific upgrade point is determined by the
   * {@link #upgradeCheckpoint} parameter.
   *
   * <p>The test workflow:
   * <ol>
   *   <li>Create cluster and file</li>
   *   <li>Perform many small writes (29 bytes) with flush after each</li>
   *   <li>Final flush and verification</li>
   * </ol>
   *
   * <p><b>Note:</b> Cluster and FileSystem cleanup is handled automatically by
   * {@link ProcessBasedUpgradeTestBase} @After method. No try-finally block needed.
   *
   * @throws Exception if test fails
   */
  @Test
  public void testComplexFlush() throws Exception {
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    // Create cluster - uses conf from base class
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    // Create file
    Path file1 = new Path("/complexFlush.dat");
    FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
    System.out.println("Created file complexFlush.dat");

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    // Many small writes with flushes
    int start = 0;
    int writeCount = 0;
    for (start = 0; (start + 29) < AppendTestUtil.FILE_SIZE; ) {
      stm.write(fileContents, start, 29);
      stm.hflush();
      start += 29;
      writeCount++;

      // Insert checkpoints every 10 writes to avoid too many test executions
      if (writeCount % 10 == 0) {
        checkpoint("AFTER_WRITE_" + writeCount);
      }
    }

    // Write remaining bytes
    stm.write(fileContents, start, AppendTestUtil.FILE_SIZE - start);

    checkpoint("AFTER_FINAL_WRITE");

    // Final flush - need to make sure we completely write out all full blocks
    // before the checkFile() call (see FSOutputSummer#flush)
    stm.flush();

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // Verify that full blocks are sane
    checkFile(fs, file1, 1);

    checkpoint(UpgradeCheckpoints.BEFORE_FINAL_CLOSE);

    stm.close();

    checkpoint(UpgradeCheckpoints.AFTER_FINAL_CLOSE);

    // Verify that entire file is good
    AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
        fileContents, "Read 2");

    checkpoint(UpgradeCheckpoints.AFTER_VERIFICATION);

    // Cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

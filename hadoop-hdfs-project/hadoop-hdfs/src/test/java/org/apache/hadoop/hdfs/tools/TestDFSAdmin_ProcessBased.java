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
package org.apache.hadoop.hdfs.tools;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.PathUtils;
import org.apache.hadoop.util.ToolRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestDFSAdmin} with
 * parameterized upgrade checkpoints.
 *
 * <p>Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * <p>This test uses JUnit parameterization to run each test method multiple
 * times with upgrades at different checkpoints, providing comprehensive
 * coverage of upgrade scenarios during DFSAdmin operations.
 *
 * <p>Cleanup between parameter executions is guaranteed by
 * {@link ProcessBasedUpgradeTestBase} @Before and @After methods.
 *
 * <p>Only testListOpenFiles is transformed.
 *
 * @see TestDFSAdmin Original test using MiniDFSCluster
 * @see ProcessBasedUpgradeTestBase Base class with cleanup and checkpoint support
 */
@RunWith(Parameterized.class)
public class TestDFSAdmin_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestDFSAdmin_ProcessBased.class);

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

        // File operations
        "AFTER_FILE_CREATION",
        "AFTER_FIRST_LIST",
        "AFTER_REOPEN_FILES",

        // Path-specific operations
        "AFTER_PATH_TESTS",

        // Verification
        UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  private static final PrintStream OLD_OUT = System.out;
  private static final PrintStream OLD_ERR = System.err;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  private void redirectStream() {
    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));
  }

  private void resetStream() {
    out.reset();
    err.reset();
  }

  /**
   * Cleanup stream redirection after each test.
   * This runs after the base class @After method (cluster/fs cleanup).
   */
  @After
  public void tearDownStreams() throws Exception {
    try {
      System.out.flush();
      System.err.flush();
    } finally {
      System.setOut(OLD_OUT);
      System.setErr(OLD_ERR);
    }
  }

  private String scanIntoString(final ByteArrayOutputStream baos) {
    final Scanner scanner = new Scanner(baos.toString());
    final StringBuilder builder = new StringBuilder();
    while (scanner.hasNextLine()) {
      builder.append(scanner.nextLine());
      builder.append(System.lineSeparator());
    }
    scanner.close();
    return builder.toString();
  }

  private void verifyOpenFilesListing(HashSet<Path> closedFileSet,
      HashMap<Path, FSDataOutputStream> openFilesMap) {
    final String outStr = scanIntoString(out);
    LOG.info("dfsadmin -listOpenFiles output: \n" + out);
    if (closedFileSet != null) {
      for (Path closedFilePath : closedFileSet) {
        assertThat(outStr,
            not(containsString(closedFilePath.toString() +
                System.lineSeparator())));
      }
    }

    for (Path openFilePath : openFilesMap.keySet()) {
      assertThat(outStr, is(containsString(openFilePath.toString() +
          System.lineSeparator())));
    }
  }

  /**
   * Test DFSAdmin -listOpenFiles command with upgrade checkpoints.
   *
   * <p>With 7 checkpoints, this single test method generates 7 test executions,
   * each testing upgrade at a different point in the DFSAdmin workflow.
   */
  @Test(timeout=120000)
  public void testListOpenFiles() throws Exception {
    redirectStream();

    conf.setInt(
        DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 500);
    conf.setLong(DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_LIST_OPENFILES_NUM_RESPONSES, 5);
    final Path baseDir = new Path(
        PathUtils.getTestDir(getClass()).getAbsolutePath(),
        GenericTestUtils.getMethodName());
    // Note: HDFS_MINIDFS_BASEDIR not used with ProcessBasedMiniDFSCluster

    final int numDataNodes = 3;
    final int numClosedFiles = 25;
    final int numOpenFiles = 15;

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
                .numDataNodes(numDataNodes)
                .format(true)
                .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final short replFactor = 1;
    final long fileLength = 512L;
    fs = cluster.getFileSystem();
    final Path parentDir = new Path("/tmp/files/");

    fs.mkdirs(parentDir);
    HashSet<Path> closedFileSet = new HashSet<>();
    for (int i = 0; i < numClosedFiles; i++) {
      Path file = new Path(parentDir, "closed-file-" + i);
      DFSTestUtil.createFile(fs, file, fileLength, replFactor, 12345L);
      closedFileSet.add(file);
    }

    HashMap<Path, FSDataOutputStream> openFilesMap = new HashMap<>();
    for (int i = 0; i < numOpenFiles; i++) {
      Path file = new Path(parentDir, "open-file-" + i);
      DFSTestUtil.createFile(fs, file, fileLength, replFactor, 12345L);
      FSDataOutputStream outputStream = fs.append(file);
      openFilesMap.put(file, outputStream);
    }

    checkpoint("AFTER_FILE_CREATION");

    final DFSAdmin dfsAdmin = new DFSAdmin(conf);
    assertEquals(0, ToolRunner.run(dfsAdmin,
        new String[]{"-listOpenFiles"}));
    verifyOpenFilesListing(closedFileSet, openFilesMap);

    checkpoint("AFTER_FIRST_LIST");

    // Close open files before checkpoint, then reopen after
    HashMap<Path, FSDataOutputStream> tempOpenFilesMap = new HashMap<>(openFilesMap);
    for (FSDataOutputStream stream : openFilesMap.values()) {
      stream.close();
    }
    openFilesMap.clear();

    checkpoint("AFTER_REOPEN_FILES");

    // Reopen files
    for (Path file : tempOpenFilesMap.keySet()) {
      FSDataOutputStream outputStream = fs.append(file);
      openFilesMap.put(file, outputStream);
    }

    for (int count = 0; count < numOpenFiles; count++) {
      closedFileSet.addAll(DFSTestUtil.closeOpenFiles(openFilesMap, 1));
      resetStream();
      assertEquals(0, ToolRunner.run(dfsAdmin,
          new String[]{"-listOpenFiles"}));
      verifyOpenFilesListing(closedFileSet, openFilesMap);
    }

    // test -listOpenFiles command with option <path>
    openFilesMap.clear();
    Path file;
    HashMap<Path, FSDataOutputStream> openFiles1 = new HashMap<>();
    HashMap<Path, FSDataOutputStream> openFiles2 = new HashMap<>();
    for (int i = 0; i < numOpenFiles; i++) {
      if (i % 2 == 0) {
        file = new Path(new Path("/tmp/files/a"), "open-file-" + i);
      } else {
        file = new Path(new Path("/tmp/files/b"), "open-file-" + i);
      }

      DFSTestUtil.createFile(fs, file, fileLength, replFactor, 12345L);
      FSDataOutputStream outputStream = fs.append(file);

      if (i % 2 == 0) {
        openFiles1.put(file, outputStream);
      } else {
        openFiles2.put(file, outputStream);
      }
      openFilesMap.put(file, outputStream);
    }

    resetStream();
    // list all open files
    assertEquals(0,
        ToolRunner.run(dfsAdmin, new String[] {"-listOpenFiles"}));
    verifyOpenFilesListing(null, openFilesMap);

    resetStream();
    // list open files under directory path /tmp/files/a
    assertEquals(0,
        ToolRunner.run(dfsAdmin, new String[] {"-listOpenFiles",
            "-path", "/tmp/files/a"}));
    verifyOpenFilesListing(null, openFiles1);

    checkpoint("AFTER_PATH_TESTS");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    // close all open files
    for (FSDataOutputStream outputStream : openFilesMap.values()) {
      outputStream.close();
    }
    // fs and cluster cleanup handled by @After in ProcessBasedUpgradeTestBase
  }
}

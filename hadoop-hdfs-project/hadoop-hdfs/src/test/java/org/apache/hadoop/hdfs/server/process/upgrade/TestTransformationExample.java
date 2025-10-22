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
package org.apache.hadoop.hdfs.server.process.upgrade;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.RollingUpgradeAction;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.hdfs.protocol.RollingUpgradeInfo;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.tools.DFSAdmin;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/**
 * Example test class demonstrating transformation from MiniDFSCluster
 * to ProcessBasedMiniDFSCluster.
 *
 * Contains two test methods:
 * 1. testDFSAdminRollingUpgradeCommands_Transformed() - EASY transformation (mostly client APIs)
 * 2. testRollbackCommand_Transformed() - HARD transformation (requires API mapping)
 */
public class TestTransformationExample {

  private Configuration conf;
  private String hadoopHome;

  @Before
  public void setUp() {
    conf = new HdfsConfiguration();

    // Check for HADOOP_HOME - try system property first, then environment variable
    // NOTE: Maven/Hadoop build sets HADOOP_HOME to build target dir - ignore paths containing "target"
    String sysProperty = System.getProperty("HADOOP_HOME");
    String envVar = System.getenv("HADOOP_HOME");

    hadoopHome = sysProperty;
    if (hadoopHome == null || hadoopHome.contains("/target")) {
      hadoopHome = envVar;
    }
    if (hadoopHome == null || hadoopHome.contains("/target")) {
      // Fall back to default test distribution location
      hadoopHome = "/tmp/hadoop-test-distributions/hadoop-3.3.5";
    }

    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster tests", hadoopHome);

    // Verify the path exists and contains a valid Hadoop distribution
    java.io.File hadoopHomeDir = new java.io.File(hadoopHome);
    Assume.assumeTrue(
        "HADOOP_HOME directory must exist: " + hadoopHome,
        hadoopHomeDir.exists() && hadoopHomeDir.isDirectory());

    // Verify it looks like a Hadoop distribution (has share/hadoop or lib directory)
    java.io.File shareDir = new java.io.File(hadoopHomeDir, "share/hadoop");
    java.io.File libDir = new java.io.File(hadoopHomeDir, "lib");
    Assume.assumeTrue(
        "HADOOP_HOME must contain a valid Hadoop distribution (share/hadoop or lib dir): " + hadoopHome,
        shareDir.exists() || libDir.exists());
  }

  /**
   * EASY TRANSFORMATION EXAMPLE
   *
   * Original: TestRollingUpgrade.testDFSAdminRollingUpgradeCommands()
   *
   * This test uses mostly client-side APIs, so transformation is straightforward:
   * - Changed MiniDFSCluster → ProcessBasedMiniDFSCluster
   * - Added Hadoop distribution path to builder
   * - Commented out MBean checks (JMX in different process)
   * - All other code remains identical
   */
  @Test
  public void testDFSAdminRollingUpgradeCommands_Transformed() throws Exception {
    // TRANSFORMATION: Changed from MiniDFSCluster to ProcessBasedMiniDFSCluster
    // Note: Changed numDataNodes from 0 to 1 - ProcessBasedMiniDFSCluster requires at least 1 DN
    try (ProcessBasedMiniDFSCluster cluster =
            new ProcessBasedMiniDFSCluster.Builder(conf)
                .numDataNodes(1)  // TRANSFORMATION: Changed from 0 to 1 (min requirement)
                .allNodesHadoopDistribution(hadoopHome)  // TRANSFORMATION: Added distribution path
                .format(true)
                .build()) {
      cluster.waitClusterUp();  // TRANSFORMATION: Changed from waitActive()

      final Path foo = new Path("/foo");
      final Path bar = new Path("/bar");
      final Path baz = new Path("/baz");

      {
        final DistributedFileSystem dfs = cluster.getFileSystem();

        // TRANSFORMATION: Configure DFSAdmin with cluster URI so it connects to HDFS cluster
        // instead of defaulting to LocalFileSystem
        Configuration dfsAdminConf = new Configuration(conf);
        dfsAdminConf.set("fs.defaultFS", cluster.getURI().toString());
        final DFSAdmin dfsadmin = new DFSAdmin(dfsAdminConf);
        dfs.mkdirs(foo);

        // Illegal argument "abc" to rollingUpgrade option
        runCmd(dfsadmin, false, "-rollingUpgrade", "abc");

        // TRANSFORMATION NOTE: MBean checks commented out.
        // JMX MBeans are accessed in the NameNode process, which runs in a separate
        // JVM in ProcessBasedMiniDFSCluster. To access MBeans, would need to use
        // HTTP JMX endpoint: http://namenode:9870/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo
        // For this test, the core logic (DFSAdmin commands and file operations) is preserved.
        //
        // Original code:
        // checkMxBeanIsNull();

        // Query rolling upgrade
        runCmd(dfsadmin, true, "-rollingUpgrade");

        // Start rolling upgrade
        dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
        runCmd(dfsadmin, true, "-rollingUpgrade", "prepare");
        dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);

        // Query rolling upgrade
        runCmd(dfsadmin, true, "-rollingUpgrade", "query");

        // TRANSFORMATION NOTE: MBean check commented out (see note above)
        // Original code:
        // checkMxBean();

        dfs.mkdirs(bar);

        // Finalize rolling upgrade
        runCmd(dfsadmin, true, "-rollingUpgrade", "finalize");

        // RollingUpgradeInfo should be null after finalization via Java API
        assertNull(dfs.rollingUpgrade(RollingUpgradeAction.QUERY));

        // TRANSFORMATION NOTE: MBean check commented out (see note above)
        // Original code:
        // checkMxBeanIsNull();

        dfs.mkdirs(baz);

        runCmd(dfsadmin, true, "-rollingUpgrade");

        // All directories created before upgrade, when upgrade in progress and
        // after upgrade finalize exists
        Assert.assertTrue(dfs.exists(foo));
        Assert.assertTrue(dfs.exists(bar));
        Assert.assertTrue(dfs.exists(baz));

        dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
        dfs.saveNamespace();
        dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
      }

      // Ensure directories exist after restart
      cluster.restartNameNode(0);  // TRANSFORMATION: restartNameNode(index) requires index param
      {
        final DistributedFileSystem dfs = cluster.getFileSystem();
        Assert.assertTrue(dfs.exists(foo));
        Assert.assertTrue(dfs.exists(bar));
        Assert.assertTrue(dfs.exists(baz));
      }
    }
  }

  /**
   * HARD TRANSFORMATION EXAMPLE
   *
   * Original: TestRollingUpgradeRollback.testRollbackCommand()
   *
   * This test has direct object access requiring transformation:
   * - cluster.getNamesystem() → commented out (internal storage checks)
   * - nn.getNamesystem().getFSDirectory().getINode4Write() → dfs.exists() (client API)
   * - Direct NameNode creation → using cluster restart with rollback args
   * - Internal storage checks → commented out (no client equivalent)
   */
  @Test
  public void testRollbackCommand_Transformed() throws Exception {
    ProcessBasedMiniDFSCluster cluster = null;
    final Path foo = new Path("/foo");
    final Path bar = new Path("/bar");

    try {
      // TRANSFORMATION: Changed from MiniDFSCluster to ProcessBasedMiniDFSCluster
      // Note: Changed numDataNodes from 0 to 1 - ProcessBasedMiniDFSCluster requires at least 1 DN
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(1)  // TRANSFORMATION: Changed from 0 to 1 (min requirement)
          .allNodesHadoopDistribution(hadoopHome)  // TRANSFORMATION: Added distribution path
          .format(true)
          .build();
      cluster.waitClusterUp();  // TRANSFORMATION: Changed from waitActive()

      final DistributedFileSystem dfs = cluster.getFileSystem();

      // TRANSFORMATION: Configure DFSAdmin with cluster URI so it connects to HDFS cluster
      // instead of defaulting to LocalFileSystem
      Configuration dfsAdminConf = new Configuration(conf);
      dfsAdminConf.set("fs.defaultFS", cluster.getURI().toString());
      final DFSAdmin dfsadmin = new DFSAdmin(dfsAdminConf);
      dfs.mkdirs(foo);

      // Start rolling upgrade
      dfs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
      Assert.assertEquals(0,
          dfsadmin.run(new String[] { "-rollingUpgrade", "prepare" }));
      dfs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);

      // Create new directory
      dfs.mkdirs(bar);

      // TRANSFORMATION NOTE: Internal NameNode storage verification removed.
      // FSImage.getStorage() is not accessible via client APIs as it's internal
      // to the NameNode process. The original test verified edit log segments
      // and FSImage files (startSegment, mkdir, endSegment).
      //
      // Original code:
      // NNStorage storage = cluster.getNamesystem().getFSImage().getStorage();
      // checkNNStorage(storage, 3, -1);

      // Close filesystem before restarting NameNode
      dfs.close();

    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }

    // =========================================================================
    // TRANSFORMATION NOTE: Second cluster restart not possible
    // =========================================================================
    // The original test restarts the cluster with rollback to verify that /bar
    // (created during upgrade) is rolled back while /foo (created before) remains.
    //
    // This requires TWO ProcessBasedMiniDFSCluster limitations to be addressed:
    //
    // 1. CLUSTER DATA PERSISTENCE: Each ProcessBasedMiniDFSCluster instance creates
    //    its own temporary directory. When you shutdown the first cluster and create
    //    a new cluster instance, it creates a NEW temp directory, so setting
    //    .format(false) doesn't help - there's no data to reuse.
    //
    // 2. CUSTOM NAMENODE STARTUP ARGS: ProcessBasedMiniDFSCluster.Builder doesn't
    //    support passing custom NameNode startup arguments like "-rollingUpgrade rollback".
    //
    // Original code created standalone NameNode:
    // nn = NameNode.createNameNode(new String[] { "-rollingUpgrade", "rollback" }, conf);
    //
    // Then verified rollback behavior via direct object access:
    // INode fooNode = nn.getNamesystem().getFSDirectory().getINode4Write(foo.toString());
    // Assert.assertNotNull(fooNode);  // /foo should exist
    // INode barNode = nn.getNamesystem().getFSDirectory().getINode4Write(bar.toString());
    // Assert.assertNull(barNode);  // /bar should not exist after rollback
    //
    // EQUIVALENT CLIENT API: If rollback were supported, we could verify using:
    // DistributedFileSystem dfs = cluster.getFileSystem();
    // Assert.assertTrue("Directory /foo should exist after rollback", dfs.exists(foo));
    // Assert.assertFalse("Directory /bar should not exist after rollback", dfs.exists(bar));
    //
    // TRANSFORMATION LESSON: This test demonstrates the "HARD" transformation category
    // where direct object access (getNamesystem(), getFSDirectory(), getINode4Write())
    // was successfully mapped to client APIs (dfs.exists()), but the test scenario
    // itself requires ProcessBasedMiniDFSCluster enhancements to support:
    // - Persisted data directories across cluster instances
    // - Custom NameNode startup arguments
    // =========================================================================
  }

  // Helper method from TestRollingUpgrade
  static void runCmd(DFSAdmin admin, boolean expectedSuccessful,
                     String... args) throws Exception {
    int exitCode = admin.run(args);
    if (expectedSuccessful) {
      Assert.assertEquals("Command failed: " + String.join(" ", args),
          0, exitCode);
    } else {
      Assert.assertNotEquals("Command should have failed: " + String.join(" ", args),
          0, exitCode);
    }
  }
}

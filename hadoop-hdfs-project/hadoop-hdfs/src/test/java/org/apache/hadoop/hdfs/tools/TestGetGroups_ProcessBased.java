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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.tools.GetGroupsTestBase;
import org.apache.hadoop.util.Tool;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestGetGroups}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests for the HDFS implementation of {@link GetGroups}
 *
 * @see TestGetGroups Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestGetGroups_ProcessBased extends GetGroupsTestBase {

  private ProcessBasedMiniDFSCluster cluster;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START
    );
  }

  @Before
  public void setUpNameNode() throws Exception {
    conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(0).build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);
  }

  @org.junit.After
  public void tearDownNameNode() {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Override
  protected Tool getTool(PrintStream o) {
    return new GetGroups(conf, o);
  }

  /**
   * Insert an upgrade checkpoint in the test.
   */
  protected void checkpoint(String name) throws Exception {
    if (shouldUpgrade(name)) {
      cluster.upgrade();
    }
  }

  /**
   * Check if upgrade should be performed at the given checkpoint.
   */
  protected boolean shouldUpgrade(String name) {
    return upgradeCheckpoint != null
        && !upgradeCheckpoint.equals(UpgradeCheckpoints.NO_UPGRADE)
        && upgradeCheckpoint.equals(name);
  }

}

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

package org.apache.hadoop.cli;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import org.apache.hadoop.cli.util.CLICommand;
import org.apache.hadoop.cli.util.CommandExecutor.Result;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HDFSPolicyProvider;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestXAttrCLI}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestXAttrCLI Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestXAttrCLI_ProcessBased  extends CLITestHelperDFS {
  protected ProcessBasedMiniDFSCluster dfsCluster = null;
  protected FileSystem fs = null;
  protected String namenode = null;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "checkpoint={0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {UpgradeCheckpoints.NO_UPGRADE},
        {UpgradeCheckpoints.AFTER_CLUSTER_START}
    });
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    conf.setClass(PolicyProvider.POLICY_PROVIDER_CONFIG,
        HDFSPolicyProvider.class, PolicyProvider.class);
    conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, 1);

    dfsCluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(1).build();
    dfsCluster.waitClusterUp();

    // Apply upgrade checkpoint logic
    if (upgradeCheckpoint != null && !upgradeCheckpoint.equals(UpgradeCheckpoints.NO_UPGRADE)
        && upgradeCheckpoint.equals(UpgradeCheckpoints.AFTER_CLUSTER_START)) {
      dfsCluster.upgrade();
    }

    namenode = conf.get(DFSConfigKeys.FS_DEFAULT_NAME_KEY, "file:///");

    username = System.getProperty("user.name");

    fs = dfsCluster.getFileSystem();
    assertTrue("Not a HDFS: "+fs.getUri(),
        fs instanceof DistributedFileSystem);
  }

  @Override
  protected String getTestFile() {
    return "testXAttrConf.xml";
  }

  @After
  @Override
  public void tearDown() throws Exception {
    if (fs != null) {
      fs.close();
      fs = null;
    }
    if (dfsCluster != null) {
      dfsCluster.shutdown();
      dfsCluster = null;
    }
    Thread.sleep(2000);
    super.tearDown();
  }

  @Override
  protected String expandCommand(final String cmd) {
    String expCmd = cmd;
    expCmd = expCmd.replaceAll("NAMENODE", namenode);
    expCmd = expCmd.replaceAll("#LF#",
        System.getProperty("line.separator"));
    expCmd = super.expandCommand(expCmd);
    return expCmd;
  }

  @Override
  protected Result execute(CLICommand cmd) throws Exception {
    return cmd.getExecutor(namenode, conf).executeCommand(cmd.getCmd());
  }

  @Test
  @Override
  public void testAll () {
    super.testAll();
  }

}

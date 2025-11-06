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

package org.apache.hadoop.fs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import javax.security.auth.login.LoginException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFcHdfsPermission}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestFcHdfsPermission Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFcHdfsPermission_ProcessBased extends FileContextPermissionBase {

  private static final FileContextTestHelper fileContextTestHelper =
      new FileContextTestHelper("/tmp/TestFcHdfsPermission_ProcessBased");
  private FileContext fcInstance;

  private Path defaultWorkingDirectory;

  @Parameter
  public String upgradeCheckpoint;

  private ProcessBasedMiniDFSCluster cluster;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START
    );
  }

  @Override
  protected FileContextTestHelper getFileContextHelper() {
    return fileContextTestHelper;
  }

  @Override
  protected FileContext getFileContext() {
    return fcInstance;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    Configuration conf = new HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();

    if (shouldUpgrade(UpgradeCheckpoints.AFTER_CLUSTER_START)) {
      cluster.upgrade();
      cluster.waitClusterUp();
    }

    fcInstance = FileContext.getFileContext(cluster.getURI(0), conf);
    defaultWorkingDirectory = fcInstance.makeQualified(new Path("/user/" +
        UserGroupInformation.getCurrentUser().getShortUserName()));
    fcInstance.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);

    super.setUp();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    if (cluster != null) {
      try {
        cluster.shutdown();
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  private boolean shouldUpgrade(String checkpointName) {
    return upgradeCheckpoint != null
        && !upgradeCheckpoint.equals(UpgradeCheckpoints.NO_UPGRADE)
        && upgradeCheckpoint.equals(checkpointName);
  }
}

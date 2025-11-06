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

import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFcHdfsCreateMkdir}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestFcHdfsCreateMkdir Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFcHdfsCreateMkdir_ProcessBased extends
                    FileContextCreateMkdirBaseTest {

  private ProcessBasedMiniDFSCluster cluster;
  private Path defaultWorkingDirectory;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START
    );
  }

  @Override
  protected FileContextTestHelper createFileContextHelper() {
    return new FileContextTestHelper("/tmp/TestFcHdfsCreateMkdir_ProcessBased");
  }

  @Before
  @Override
  public void setUp() throws Exception {
    org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.hdfs.HdfsConfiguration();
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(2).build();
    cluster.waitClusterUp();
    fc = FileContext.getFileContext(cluster.getURI(0), conf);
    defaultWorkingDirectory = fc.makeQualified( new Path("/user/" +
        UserGroupInformation.getCurrentUser().getShortUserName()));
    fc.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);

    super.setUp();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    if (cluster != null) {
      try {
        cluster.shutdown(true);
      } catch (Exception e) {
        // Ignore
      }
    }
  }
}

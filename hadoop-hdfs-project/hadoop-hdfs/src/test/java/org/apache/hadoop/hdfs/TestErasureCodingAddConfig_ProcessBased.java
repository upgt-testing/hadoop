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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.hdfs.protocol.AddErasureCodingPolicyResponse;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.io.erasurecode.ECSchema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestErasureCodingAddConfig}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests that addition of user defined EC policies is allowed only when
 * dfs.namenode.ec.userdefined.policy.allowed is set to true.
 *
 * @see TestErasureCodingAddConfig Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestErasureCodingAddConfig_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_POLICY_CREATE",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test(timeout=180000)
  public void testECAddPolicyConfigDisable() throws Exception {
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_EC_POLICIES_USERPOLICIES_ALLOWED_KEY,
        false);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(0)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    ErasureCodingPolicy newPolicy1 =
        new ErasureCodingPolicy(new ECSchema("rs", 5, 3), 1024 * 1024);

    checkpoint("AFTER_POLICY_CREATE");

    AddErasureCodingPolicyResponse[] response =
        ((DistributedFileSystem) fs).addErasureCodingPolicies(
            new ErasureCodingPolicy[] {newPolicy1});

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    assertFalse(response[0].isSucceed());
    assertEquals(
        "Addition of user defined erasure coding policy is disabled.",
        response[0].getErrorMsg());
  }

  @Test(timeout=180000)
  public void testECAddPolicyConfigEnable() throws Exception {
    conf.setBoolean(
        DFSConfigKeys.DFS_NAMENODE_EC_POLICIES_USERPOLICIES_ALLOWED_KEY, true);

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(0)
        .build();
    cluster.waitClusterUp();
    fs = cluster.getFileSystem();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    ErasureCodingPolicy newPolicy1 =
        new ErasureCodingPolicy(new ECSchema("rs", 5, 3), 1024 * 1024);

    checkpoint("AFTER_POLICY_CREATE");

    AddErasureCodingPolicyResponse[] response =
        ((DistributedFileSystem) fs).addErasureCodingPolicies(
            new ErasureCodingPolicy[] {newPolicy1});

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    assertTrue(response[0].isSucceed());
    assertNull(response[0].getErrorMsg());
  }
}

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

package org.apache.hadoop.yarn.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeTestBase;
import org.apache.hadoop.yarn.server.process.upgrade.YarnUpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBased version of {@link TestMiniYARNClusterForHA}.
 *
 * Transformed from MiniYARNCluster to ProcessBasedMiniYARNCluster to enable
 * process-based testing and multi-version Hadoop upgrade scenarios.
 *
 * Tests High Availability cluster setup and NodeManager connection in HA mode
 * with parameterized upgrade checkpoints.
 *
 * @see TestMiniYARNClusterForHA Original test using MiniYARNCluster
 */
@RunWith(Parameterized.class)
public class TestMiniYARNClusterForHA_ProcessBased extends YarnUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      YarnUpgradeCheckpoints.NO_UPGRADE,           // Baseline - no upgrade
      YarnUpgradeCheckpoints.AFTER_CLUSTER_START,  // After cluster initialization
      "AFTER_RM_ELECTION",                         // After active RM elected
      "BEFORE_NM_CONNECT_WAIT",                    // Before waiting for NM connection
      "AFTER_NM_CONNECT"                           // After NM connection verified
    );
  }

  @Test(timeout = 120000)
  public void testClusterWorks() throws Exception {
    Configuration conf = new YarnConfiguration();
    conf.setBoolean(YarnConfiguration.AUTO_FAILOVER_ENABLED, false);
    conf.set(YarnConfiguration.RM_WEBAPP_ADDRESS, "localhost:0");

    // Create ProcessBasedMiniYARNCluster with 2 RMs (HA) and 1 NM
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(1)
        .build();

    // Start the cluster
    cluster.start();

    checkpoint(YarnUpgradeCheckpoints.AFTER_CLUSTER_START);

    // Verify active RM elected
    assertFalse("RM never turned active", -1 == cluster.getActiveRMIndex());

    checkpoint("AFTER_RM_ELECTION");

    checkpoint("BEFORE_NM_CONNECT_WAIT");

    // Test NM connection
    assertTrue("NMs fail to connect to the RM",
        cluster.waitForNodeManagersToConnect(5000));

    checkpoint("AFTER_NM_CONNECT");

    // No try-finally needed - YarnUpgradeTestBase @After handles cleanup!
  }
}

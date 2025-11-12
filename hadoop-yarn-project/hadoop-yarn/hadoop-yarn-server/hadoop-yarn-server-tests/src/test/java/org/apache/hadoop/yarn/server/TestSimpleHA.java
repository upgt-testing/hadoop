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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;
import org.junit.After;
import org.junit.Test;

/**
 * Simple HA test without parameterization to debug RM startup issues.
 */
public class TestSimpleHA {

  private ProcessBasedMiniYARNCluster cluster;

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 60000)
  public void testSimpleHA() throws Exception {
    YarnConfiguration conf = new YarnConfiguration();

    // Configuration from IntegrationTestBase.configureYarn()
    // Small memory limits for testing
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MAXIMUM_ALLOCATION_MB, 2048);
    conf.setInt(YarnConfiguration.NM_PMEM_MB, 4096);
    conf.setInt(YarnConfiguration.NM_VMEM_PMEM_RATIO, 3);

    // Fast heartbeats for quicker tests
    conf.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 500);
    conf.setLong(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 10000);

    // Disable ACLs for testing
    conf.setBoolean(YarnConfiguration.YARN_ACL_ENABLE, false);

    // Fast application completion
    conf.setInt(YarnConfiguration.RM_AM_MAX_ATTEMPTS, 2);

    // HA configuration
    conf.setBoolean(YarnConfiguration.AUTO_FAILOVER_ENABLED, false);
    conf.set(YarnConfiguration.RM_WEBAPP_ADDRESS, "localhost:0");

    System.out.println("Creating cluster with 2 RMs (HA mode)...");
    String hadoopHome = "/Users/allenwang/xlab/hadoop-test-distributions/hadoop-3.3.5";
    cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(1)
        .allNodesHadoopDistribution(hadoopHome)
        .build();

    System.out.println("Starting cluster...");
    cluster.start();

    System.out.println("Cluster started successfully!");
    System.out.println("Active RM index: " + cluster.getActiveRMIndex());
  }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.hdfs.server.diskbalancer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.hdfs.server.diskbalancer.connectors.ClusterConnector;
import org.apache.hadoop.hdfs.server.diskbalancer.connectors.ConnectorFactory;
import org.apache.hadoop.hdfs.server.diskbalancer.datamodel.DiskBalancerCluster;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestConnectors}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * @see TestConnectors Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestConnectors_ProcessBased extends ProcessBasedUpgradeTestBase {

  private final int numDatanodes = 3;
  private final int volumeCount = 2; // default volumes in MiniDFSCluster.

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_CONNECTOR_CREATION",
      "AFTER_CLUSTER_READ",
      "BEFORE_VERIFICATION"
    );
  }

  @Test
  public void testNameNodeConnector() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numDatanodes)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    ClusterConnector nameNodeConnector =
        ConnectorFactory.getCluster(cluster.getFileSystem().getUri(), conf);
    checkpoint("AFTER_CONNECTOR_CREATION");

    DiskBalancerCluster diskBalancerCluster =
        new DiskBalancerCluster(nameNodeConnector);
    diskBalancerCluster.readClusterInfo();
    checkpoint("AFTER_CLUSTER_READ");

    checkpoint("BEFORE_VERIFICATION");
    Assert.assertEquals("Expected number of Datanodes not found.",
        numDatanodes, diskBalancerCluster.getNodes().size());
    Assert.assertEquals("Expected number of volumes not found.",
        volumeCount, diskBalancerCluster.getNodes().get(0).getVolumeCount());
  }

  @Test
  public void testJsonConnector() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numDatanodes)
        .build();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    ClusterConnector nameNodeConnector =
        ConnectorFactory.getCluster(cluster.getFileSystem().getUri(), conf);
    checkpoint("AFTER_CONNECTOR_CREATION");

    DiskBalancerCluster diskBalancerCluster =
        new DiskBalancerCluster(nameNodeConnector);
    diskBalancerCluster.readClusterInfo();
    checkpoint("AFTER_CLUSTER_READ");

    String diskBalancerJson = diskBalancerCluster.toJson();
    checkpoint("AFTER_JSON_SERIALIZATION");

    DiskBalancerCluster serializedCluster =
        DiskBalancerCluster.parseJson(diskBalancerJson);
    checkpoint("BEFORE_VERIFICATION");

    Assert.assertEquals("Parsed cluster is not equal to persisted info.",
        diskBalancerCluster.getNodes().size(),
        serializedCluster.getNodes().size());
  }
}

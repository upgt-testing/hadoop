/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicy;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicyDefault;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementPolicyRackFaultTolerant;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestErasureCodingMultipleRacks}.
 *
 * Tests erasure coding block placement with skewed number of nodes per rack,
 * with support for process-based testing and upgrade scenarios.
 *
 * @see TestErasureCodingMultipleRacks Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestErasureCodingMultipleRacks_ProcessBased extends ProcessBasedUpgradeTestBase {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestErasureCodingMultipleRacks_ProcessBased.class);

  static {
    GenericTestUtils.setLogLevel(BlockPlacementPolicy.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(BlockPlacementPolicyDefault.LOG, Level.TRACE);
    GenericTestUtils.setLogLevel(BlockPlacementPolicyRackFaultTolerant.LOG,
        Level.TRACE);
    GenericTestUtils.setLogLevel(NetworkTopology.LOG, Level.DEBUG);
  }

  @Rule
  public Timeout globalTimeout = new Timeout(300000);

  private ErasureCodingPolicy ecPolicy;
  private DistributedFileSystem dfs;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_WRITE",
      "BEFORE_VERIFICATION"
    );
  }

  public ErasureCodingPolicy getPolicy() {
    return StripedFileTestUtil.getDefaultECPolicy();
  }

  /**
   * Setup cluster with desired number of DN, racks, and specified number of
   * rack that only has 1 DN. Other racks will be evenly setup with the number
   * of DNs.
   *
   * @param numDatanodes number of total Datanodes.
   * @param numRacks number of total racks
   * @param numSingleDnRacks number of racks that only has 1 DN
   * @throws Exception
   */
  public void setupCluster(final int numDatanodes, final int numRacks,
      final int numSingleDnRacks) throws Exception {
    assert numDatanodes > numRacks;
    assert numRacks > numSingleDnRacks;
    assert numSingleDnRacks >= 0;

    ecPolicy = getPolicy();
    conf.setBoolean(DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY, false);

    // Build rack array same as DFSTestUtil.setupCluster
    final String[] racks = new String[numDatanodes];
    for (int i = 0; i < numSingleDnRacks; i++) {
      racks[i] = "/rack" + i;
    }
    for (int i = numSingleDnRacks; i < numDatanodes; i++) {
      racks[i] =
          "/rack" + (numSingleDnRacks + (i % (numRacks - numSingleDnRacks)));
    }

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(numDatanodes)
        .racks(racks)
        .format(true)
        .build();
    cluster.waitClusterUp();
    dfs = (DistributedFileSystem) cluster.getFileSystem();
    dfs.setErasureCodingPolicy(new Path("/"), ecPolicy.getName());
  }

  /**
   * Client-side wait for replication (replacement for DFSTestUtil.waitForReplication
   * which requires internal cluster access).
   */
  private void waitForReplication(Path path, int expectedHosts, int maxAttempts)
      throws Exception {
    for (int i = 0; i < maxAttempts; i++) {
      Thread.sleep(1000);
      BlockLocation[] blocks = dfs.getFileBlockLocations(path, 0, Long.MAX_VALUE);
      if (blocks.length > 0 && blocks[0].getHosts().length >= expectedHosts) {
        return;
      }
    }
    throw new Exception("Timed out waiting for replication");
  }

  // Extreme case.
  @Test
  public void testSkewedRack1() throws Exception {
    final int dataUnits = ecPolicy.getNumDataUnits();
    final int parityUnits = ecPolicy.getNumParityUnits();
    setupCluster(dataUnits + parityUnits, 2, 1);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final int filesize = ecPolicy.getNumDataUnits() * ecPolicy.getCellSize();
    byte[] contents = new byte[filesize];

    final Path path = new Path("/testfile");
    LOG.info("Writing file " + path);
    DFSTestUtil.writeFile(dfs, path, contents);

    checkpoint("AFTER_FILE_WRITE");

    BlockLocation[] blocks = dfs.getFileBlockLocations(path, 0, Long.MAX_VALUE);

    checkpoint("BEFORE_VERIFICATION");

    assertEquals(ecPolicy.getNumDataUnits() + ecPolicy.getNumParityUnits(),
        blocks[0].getHosts().length);
  }

  // 1 rack has many nodes, other racks have single node. Extreme case.
  @Test
  public void testSkewedRack2() throws Exception {
    final int dataUnits = ecPolicy.getNumDataUnits();
    final int parityUnits = ecPolicy.getNumParityUnits();
    setupCluster(dataUnits + parityUnits * 2, dataUnits, dataUnits - 1);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final int filesize = ecPolicy.getNumDataUnits() * ecPolicy.getCellSize();
    byte[] contents = new byte[filesize];

    final Path path = new Path("/testfile");
    LOG.info("Writing file " + path);
    DFSTestUtil.writeFile(dfs, path, contents);

    checkpoint("AFTER_FILE_WRITE");

    BlockLocation[] blocks = dfs.getFileBlockLocations(path, 0, Long.MAX_VALUE);

    checkpoint("BEFORE_VERIFICATION");

    assertEquals(ecPolicy.getNumDataUnits() + ecPolicy.getNumParityUnits(),
        blocks[0].getHosts().length);
  }

  // 2 racks have sufficient nodes, other racks has 1. Should be able to
  // tolerate 1 rack failure.
  @Test
  public void testSkewedRack3() throws Exception {
    final int dataUnits = ecPolicy.getNumDataUnits();
    final int parityUnits = ecPolicy.getNumParityUnits();
    // Create enough extra DNs on the 2 racks to test even placement.
    // Desired placement is parityUnits replicas on the 2 racks, and 1 replica
    // on the rest of the racks (which only have 1 DN)
    int numRacks = dataUnits - parityUnits + 2;
    setupCluster(dataUnits + parityUnits * 4, numRacks,
        dataUnits - parityUnits);

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final int filesize = ecPolicy.getNumDataUnits() * ecPolicy.getCellSize();
    byte[] contents = new byte[filesize];

    for (int i = 0; i < 10; ++i) {
      final Path path = new Path("/testfile" + i);
      LOG.info("Writing file " + path);
      DFSTestUtil.writeFile(dfs, path, contents);

      checkpoint("AFTER_FILE_WRITE");

      // Wait for replication using client-side block location checks
      waitForReplication(path,
          ecPolicy.getNumDataUnits() + ecPolicy.getNumParityUnits(), 20);

      BlockLocation[] blocks =
          dfs.getFileBlockLocations(path, 0, Long.MAX_VALUE);

      checkpoint("BEFORE_VERIFICATION");

      assertEquals(ecPolicy.getNumDataUnits() + ecPolicy.getNumParityUnits(),
          blocks[0].getHosts().length);
      assertRackFailureTolerated(blocks[0].getTopologyPaths());
    }
  }

  // Verifies that no more than numParityUnits is placed on a rack.
  private void assertRackFailureTolerated(final String[] topologies) {
    final Map<String, Integer> racksCount = new HashMap<>();
    for (String t : topologies) {
      final Integer count = racksCount.get(getRackName(t));
      if (count == null) {
        racksCount.put(getRackName(t), 1);
      } else {
        racksCount.put(getRackName(t), count + 1);
      }
    }
    LOG.info("Rack count map is: {}", racksCount);

    for (Integer count : racksCount.values()) {
      assertTrue(count <= ecPolicy.getNumParityUnits());
    }
  }

  private String getRackName(final String topology) {
    assert topology.indexOf('/', 1) > 0;
    return topology.substring(0, topology.indexOf('/', 1));
  }
}

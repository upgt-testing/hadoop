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
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestApplyingStoragePolicy}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Tests applying storage policies (HOT, WARM, COLD) to files and directories.
 *
 * @see TestApplyingStoragePolicy Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestApplyingStoragePolicy_ProcessBased extends ProcessBasedUpgradeTestBase {
  private static final short REPL = 1;
  private static final int SIZE = 128;

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      UpgradeCheckpoints.AFTER_FILE_CREATE,
      "AFTER_SET_POLICY",
      "AFTER_UNSET_POLICY",
      UpgradeCheckpoints.BEFORE_VERIFICATION
    );
  }

  @Test
  public void testStoragePolicyByDefault() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(REPL).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    final Path fooz = new Path(bar, "/fooz");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy hot = suite.getPolicy("HOT");

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    /*
     * test: storage policy is HOT by default or inherited from nearest
     * ancestor, if not explicitly specified for newly created dir/file.
     */
    assertEquals(fs.getStoragePolicy(foo), hot);
    assertEquals(fs.getStoragePolicy(bar), hot);
    assertEquals(fs.getStoragePolicy(wow), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }
  }

  @Test
  public void testSetAndUnsetStoragePolicy() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(REPL).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    final Path fooz = new Path(bar, "/fooz");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");

    /*
     * test: set storage policy
     */
    fs.setStoragePolicy(foo, warm.getName());
    fs.setStoragePolicy(bar, cold.getName());
    fs.setStoragePolicy(wow, hot.getName());
    try {
      fs.setStoragePolicy(fooz, warm.getName());
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint("AFTER_SET_POLICY");

    /*
     * test: get storage policy after set
     */
    assertEquals(fs.getStoragePolicy(foo), warm);
    assertEquals(fs.getStoragePolicy(bar), cold);
    assertEquals(fs.getStoragePolicy(wow), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    /*
     * test: unset storage policy in the case of being set
     */
    fs.unsetStoragePolicy(foo);
    fs.unsetStoragePolicy(bar);
    fs.unsetStoragePolicy(wow);
    try {
      fs.unsetStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint("AFTER_UNSET_POLICY");

    /*
     * test: default storage policy is applied after unset, since there are no
     * more available ancestors
     */
    assertEquals(fs.getStoragePolicy(foo), hot);
    assertEquals(fs.getStoragePolicy(bar), hot);
    assertEquals(fs.getStoragePolicy(wow), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }
  }

  @Test
  public void testNestedStoragePolicy() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(REPL).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    final Path fooz = new Path("/foos");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");

    /*
     * test: set storage policy
     */
    fs.setStoragePolicy(foo, warm.getName());
    fs.setStoragePolicy(bar, cold.getName());
    fs.setStoragePolicy(wow, hot.getName());
    try {
      fs.setStoragePolicy(fooz, warm.getName());
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint("AFTER_SET_POLICY");

    /*
     * test: get storage policy after set
     */
    assertEquals(fs.getStoragePolicy(foo), warm);
    assertEquals(fs.getStoragePolicy(bar), cold);
    assertEquals(fs.getStoragePolicy(wow), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);

    /*
     * test: unset storage policy in the case of being nested
     */
    // unset wow
    fs.unsetStoragePolicy(wow);
    // inherit storage policy from wow's nearest ancestor
    assertEquals(fs.getStoragePolicy(wow), cold);
    // unset bar
    fs.unsetStoragePolicy(bar);
    // inherit storage policy from bar's nearest ancestor
    assertEquals(fs.getStoragePolicy(bar), warm);
    // unset foo
    fs.unsetStoragePolicy(foo);
    // default storage policy is applied, since no more available ancestors
    assertEquals(fs.getStoragePolicy(foo), hot);
    // unset fooz
    try {
      fs.unsetStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint("AFTER_UNSET_POLICY");

    /*
     * test: default storage policy is applied, since no explicit policies from
     * ancestors are available
     */
    assertEquals(fs.getStoragePolicy(foo), hot);
    assertEquals(fs.getStoragePolicy(bar), hot);
    assertEquals(fs.getStoragePolicy(wow), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }
  }

  @Test
  public void testSetAndGetStoragePolicy() throws Exception {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf).numDataNodes(REPL).build();
    fs = cluster.getFileSystem();
    cluster.waitClusterUp();

    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path fooz = new Path("/fooz");
    DFSTestUtil.createFile(fs, bar, SIZE, REPL, 0);

    checkpoint(UpgradeCheckpoints.AFTER_FILE_CREATE);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");

    assertEquals(fs.getStoragePolicy(foo), hot);
    assertEquals(fs.getStoragePolicy(bar), hot);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    /*
     * test: set storage policy
     */
    fs.setStoragePolicy(foo, warm.getName());
    fs.setStoragePolicy(bar, cold.getName());
    try {
      fs.setStoragePolicy(fooz, warm.getName());
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint("AFTER_SET_POLICY");

    /*
     * test: get storage policy after set
     */
    assertEquals(fs.getStoragePolicy(foo), warm);
    assertEquals(fs.getStoragePolicy(bar), cold);
    try {
      fs.getStoragePolicy(fooz);
    } catch (Exception e) {
      assertTrue(e instanceof FileNotFoundException);
    }

    checkpoint(UpgradeCheckpoints.BEFORE_VERIFICATION);
  }
}

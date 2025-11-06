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

package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.XAttrSetFlag;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.SafeModeAction;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.ProcessBasedUpgradeTestBase;
import org.apache.hadoop.hdfs.server.process.UpgradeCheckpoints;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFSImageWithXAttr}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios. Tests XAttr
 * persistence across NameNode restarts and FSImage saves.
 *
 * Tests:
 * 1) save xattrs, restart NN, assert xattrs reloaded from edit log
 * 2) save xattrs, create new checkpoint, restart NN, assert xattrs
 *    reloaded from fsimage
 *
 * @see TestFSImageWithXAttr Original test using MiniDFSCluster
 */
@RunWith(Parameterized.class)
public class TestFSImageWithXAttr_ProcessBased extends ProcessBasedUpgradeTestBase {

  @Parameter
  public String upgradeCheckpoint;

  @Parameters(name = "upgrade-at={0}")
  public static Collection<String> checkpoints() {
    return Arrays.asList(
      UpgradeCheckpoints.NO_UPGRADE,
      UpgradeCheckpoints.AFTER_CLUSTER_START,
      "AFTER_FILE_CREATE",
      "AFTER_XATTR_SET",
      "AFTER_FIRST_RESTART",
      "AFTER_XATTR_REPLACE",
      "AFTER_SECOND_RESTART",
      "AFTER_XATTR_REMOVE",
      "AFTER_THIRD_RESTART"
    );
  }

  // xattrs
  private static final String name1 = "user.a1";
  private static final byte[] value1 = {0x31, 0x32, 0x33};
  private static final byte[] newValue1 = {0x31, 0x31, 0x31};
  private static final String name2 = "user.a2";
  private static final byte[] value2 = {0x37, 0x38, 0x39};
  private static final String name3 = "user.a3";
  private static final byte[] value3 = {};

  private void testXAttr(boolean persistNamespace) throws Exception {
    Path path = new Path("/p");

    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_XATTRS_ENABLED_KEY, true);
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    cluster.waitClusterUp();

    DistributedFileSystem fs = cluster.getFileSystem();
    checkpoint(UpgradeCheckpoints.AFTER_CLUSTER_START);

    fs.create(path).close();
    checkpoint("AFTER_FILE_CREATE");

    fs.setXAttr(path, name1, value1, EnumSet.of(XAttrSetFlag.CREATE));
    fs.setXAttr(path, name2, value2, EnumSet.of(XAttrSetFlag.CREATE));
    fs.setXAttr(path, name3, null, EnumSet.of(XAttrSetFlag.CREATE));
    checkpoint("AFTER_XATTR_SET");

    restart(fs, persistNamespace);
    checkpoint("AFTER_FIRST_RESTART");

    Map<String, byte[]> xattrs = fs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 3);
    Assert.assertArrayEquals(value1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));
    Assert.assertArrayEquals(value3, xattrs.get(name3));

    fs.setXAttr(path, name1, newValue1, EnumSet.of(XAttrSetFlag.REPLACE));
    checkpoint("AFTER_XATTR_REPLACE");

    restart(fs, persistNamespace);
    checkpoint("AFTER_SECOND_RESTART");

    xattrs = fs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 3);
    Assert.assertArrayEquals(newValue1, xattrs.get(name1));
    Assert.assertArrayEquals(value2, xattrs.get(name2));
    Assert.assertArrayEquals(value3, xattrs.get(name3));

    fs.removeXAttr(path, name1);
    fs.removeXAttr(path, name2);
    fs.removeXAttr(path, name3);
    checkpoint("AFTER_XATTR_REMOVE");

    restart(fs, persistNamespace);
    checkpoint("AFTER_THIRD_RESTART");

    xattrs = fs.getXAttrs(path);
    Assert.assertEquals(xattrs.size(), 0);
  }

  @Test
  public void testPersistXAttr() throws Exception {
    testXAttr(true);
  }

  @Test
  public void testXAttrEditLog() throws Exception {
    testXAttr(false);
  }

  /**
   * Restart the NameNode, optionally saving a new checkpoint.
   *
   * @param fs DistributedFileSystem used for saving namespace
   * @param persistNamespace boolean true to save a new checkpoint
   * @throws Exception if restart fails
   */
  private void restart(DistributedFileSystem fs, boolean persistNamespace)
      throws Exception {
    if (persistNamespace) {
      fs.setSafeMode(SafeModeAction.SAFEMODE_ENTER);
      fs.saveNamespace();
      fs.setSafeMode(SafeModeAction.SAFEMODE_LEAVE);
    }

    cluster.restartNameNode(0);
    cluster.waitClusterUp();
  }
}

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
package org.apache.hadoop.hdfs.tools;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;
import org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;

public class TestStoragePolicyCommands_RestartInjected {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestStoragePolicyCommands_RestartInjected.class);
  private static final short REPL = 1;
  private static final int SIZE = 128;

  protected static Configuration conf;
  protected static MiniDFSCluster cluster;
  protected static FileSystem fs;

  @Before
  public void clusterSetUp() throws IOException, URISyntaxException {
    conf = new HdfsConfiguration();
    conf.set(DFSConfigKeys.DFS_STORAGE_POLICY_SATISFIER_MODE_KEY,
        StoragePolicySatisfierMode.EXTERNAL.toString());
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    StorageType[][] newtypes = new StorageType[][] {
        {StorageType.ARCHIVE, StorageType.DISK}};
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPL)
        .storageTypes(newtypes).build();
    cluster.waitActive();
    fs = cluster.getFileSystem();
  }

  @After
  public void clusterShutdown() throws IOException{
    if(fs != null) {
      fs.close();
      fs = null;
    }
    if(cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterCreate_NN_Graceful() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterCreate_NN_Crash() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterSetPolicy_NN_Graceful() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterSetPolicy_NN_Crash() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterUnsetPolicy_NN_Graceful() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndUnsetStoragePolicy_AfterUnsetPolicy_NN_Crash() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    final Path wow = new Path(bar, "wow");
    DFSTestUtil.createFile(fs, wow, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path " + fs.getUri()
        + "/foo -policy WARM", 0, "Set storage policy WARM on " + fs.getUri()
        + "/foo");
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar/wow -policy HOT",
        0, "Set storage policy HOT on " + wow.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    final BlockStoragePolicy hot = suite.getPolicy("HOT");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "The storage policy of " + fs.getUri() + "/foo:\n"
        + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + ":\n" + hot);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path " + fs.getUri()
        + "/foo", 0, "Unset storage policy from " + fs.getUri() + "/foo");
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar", 0,
        "Unset storage policy from " + bar.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /foo/bar/wow", 0,
        "Unset storage policy from " + wow.toString());
    DFSTestUtil.toolRun(admin, "-unsetStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar/wow", 0,
        "The storage policy of " + wow.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndGetStoragePolicy_AfterCreate_NN_Graceful() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    DFSTestUtil.createFile(fs, bar, SIZE, REPL, 0);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");

    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo -policy WARM", 0,
        "Set storage policy WARM on " + foo.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + ":\n" + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndGetStoragePolicy_AfterCreate_NN_Crash() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    DFSTestUtil.createFile(fs, bar, SIZE, REPL, 0);

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");

    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo -policy WARM", 0,
        "Set storage policy WARM on " + foo.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + ":\n" + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndGetStoragePolicy_AfterSetPolicy_NN_Graceful() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    DFSTestUtil.createFile(fs, bar, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");

    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo -policy WARM", 0,
        "Set storage policy WARM on " + foo.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, fs);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + ":\n" + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }

  @Test(timeout = 120000)
  public void testSetAndGetStoragePolicy_AfterSetPolicy_NN_Crash() throws Exception {
    final Path foo = new Path("/foo");
    final Path bar = new Path(foo, "bar");
    DFSTestUtil.createFile(fs, bar, SIZE, REPL, 0);

    final StoragePolicyAdmin admin = new StoragePolicyAdmin(conf);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + " is unspecified");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + " is unspecified");

    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo -policy WARM", 0,
        "Set storage policy WARM on " + foo.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /foo/bar -policy COLD",
        0, "Set storage policy COLD on " + bar.toString());
    DFSTestUtil.toolRun(admin, "-setStoragePolicy -path /fooz -policy WARM",
        2, "File/Directory does not exist: /fooz");

    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, fs);

    final BlockStoragePolicySuite suite = BlockStoragePolicySuite
        .createDefaultSuite();
    final BlockStoragePolicy warm = suite.getPolicy("WARM");
    final BlockStoragePolicy cold = suite.getPolicy("COLD");
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo", 0,
        "The storage policy of " + foo.toString() + ":\n" + warm);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /foo/bar", 0,
        "The storage policy of " + bar.toString() + ":\n" + cold);
    DFSTestUtil.toolRun(admin, "-getStoragePolicy -path /fooz", 2,
        "File/Directory does not exist: /fooz");
  }
}

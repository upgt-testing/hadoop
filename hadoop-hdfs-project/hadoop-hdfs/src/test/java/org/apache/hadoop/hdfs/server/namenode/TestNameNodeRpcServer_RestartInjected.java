/*
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

import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_IP_PROXY_USERS;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.LocatedBlocks;
import org.apache.hadoop.ipc.CallerContext;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameNodeRpcServer_RestartInjected {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestNameNodeRpcServer_RestartInjected.class);

  private static String getPreferredLocation(DistributedFileSystem fs,
                                             Path p) throws IOException{
    LocatedBlocks blocks = fs.getClient()
        .getLocatedBlocks(p.toUri().getPath(), 0);
    return blocks.get(0).getLocations()[0].getHostName();
  }

  static final int ITERATIONS_TO_USE = 20;

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_NN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_NN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_DN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_DN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_AllDN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_AllDN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.ALL_DATANODES, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_RandomDN_Graceful() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }

  @Test(timeout = 120000)
  public void testNamenodeRpcClientIpProxy_AfterClose_RandomDN_Crash() throws Exception {
    Configuration conf = new HdfsConfiguration();

    conf.set(DFS_NAMENODE_IP_PROXY_USERS, "fake_joe");
    final String[] racks = new String[]{"/rack1", "/rack2", "/rack3"};
    final String[] hosts = new String[]{"node1", "node2", "node3"};
    MiniDFSCluster cluster = null;
    final CallerContext original = CallerContext.getCurrent();

    try {
      cluster = new MiniDFSCluster.Builder(conf)
          .racks(racks).hosts(hosts).numDataNodes(hosts.length)
          .build();
      cluster.waitActive();
      DistributedFileSystem fs = cluster.getFileSystem();
      final Path fooName = fs.makeQualified(new Path("/foo"));
      FSDataOutputStream stream = fs.create(fooName);
      stream.write("Hello world!\n".getBytes(StandardCharsets.UTF_8));
      stream.close();

      executeRestart(cluster, RestartTarget.RANDOM_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, fs);

      CallerContext.setCurrent(
          new CallerContext.Builder("test", conf)
              .append(CallerContext.CLIENT_IP_STR, hosts[0])
              .build());
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(fs, fooName);
        if (!hosts[0].equals(host)) {
          break;
        } else if (trial == ITERATIONS_TO_USE - 1) {
          assertNotEquals("Failed to get non-node1", hosts[0], host);
        }
      }
      UserGroupInformation joe =
          UserGroupInformation.createUserForTesting("fake_joe",
              new String[]{"fake_group"});
      DistributedFileSystem joeFs =
          (DistributedFileSystem) DFSTestUtil.getFileSystemAs(joe, conf);
      for (int trial = 0; trial < ITERATIONS_TO_USE; ++trial) {
        String host = getPreferredLocation(joeFs, fooName);
        assertEquals("Trial " + trial + " failed", hosts[0], host);
      }
    } finally {
      CallerContext.setCurrent(original);
      if (cluster != null) {
        cluster.shutdown();
      }
      conf.unset(DFS_NAMENODE_IP_PROXY_USERS);
    }
  }
}

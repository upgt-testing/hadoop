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


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.thirdparty.com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ParentNotDirectoryException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.XAttr;
import org.apache.hadoop.fs.XAttrSetFlag;
import org.apache.hadoop.hdfs.protocol.NSQuotaExceededException;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory.DirOp;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.hadoop.thirdparty.com.google.common.collect.Lists;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartTarget;
import org.apache.hadoop.hdfs.RestartInjectionFramework.RestartMode;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.executeRestart;
import static org.apache.hadoop.hdfs.RestartInjectionFramework.verifyClusterHealth;

public class TestFSDirectory_RestartInjected {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestFSDirectory_RestartInjected.class);

  private static final long seed = 0;
  private static final short REPLICATION = 3;

  private final Path dir = new Path("/" + getClass().getSimpleName());
  
  private final Path sub1 = new Path(dir, "sub1");
  private final Path file1 = new Path(sub1, "file1");
  private final Path file2 = new Path(sub1, "file2");

  private final Path sub11 = new Path(sub1, "sub11");
  private final Path file3 = new Path(sub11, "file3");
  private final Path file5 = new Path(sub1, "z_file5");

  private final Path sub2 = new Path(dir, "sub2");
  private final Path file6 = new Path(sub2, "file6");

  private Configuration conf;
  private MiniDFSCluster cluster;
  private FSNamesystem fsn;
  private FSDirectory fsdir;

  private DistributedFileSystem hdfs;

  private static final int numGeneratedXAttrs = 256;
  private static final ImmutableList<XAttr> generatedXAttrs =
      ImmutableList.copyOf(generateXAttrs(numGeneratedXAttrs));

  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_MAX_XATTRS_PER_INODE_KEY, 2);
    conf.setInt(
        CommonConfigurationKeysPublic.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY,
        0);
    cluster = new MiniDFSCluster.Builder(conf)
      .numDataNodes(REPLICATION)
      .build();
    cluster.waitActive();
    
    fsn = cluster.getNamesystem();
    fsdir = fsn.getFSDirectory();
    
    hdfs = cluster.getFileSystem();
    DFSTestUtil.createFile(hdfs, file1, 1024, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, 1024, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file3, 1024, REPLICATION, seed);

    DFSTestUtil.createFile(hdfs, file5, 1024, REPLICATION, seed);
    hdfs.mkdirs(sub2);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test(timeout = 120000)
  public void testDumpTree_AfterSetup_NN_Graceful() throws Exception {
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);
    fsn = cluster.getNamesystem();
    fsdir = fsn.getFSDirectory();

    final INode root = fsdir.getINode("/");

    LOG.info("Original tree");
    final StringBuffer b1 = root.dumpTreeRecursively();
    System.out.println("b1=" + b1);

    final BufferedReader in = new BufferedReader(new StringReader(b1.toString()));

    String line = in.readLine();
    checkClassName(line);

    for(; (line = in.readLine()) != null; ) {
      line = line.trim();
      if (!line.isEmpty() && !line.contains("snapshot")) {
        assertTrue("line=" + line,
            line.startsWith(INodeDirectory.DUMPTREE_LAST_ITEM)
                || line.startsWith(INodeDirectory.DUMPTREE_EXCEPT_LAST_ITEM)
        );
        checkClassName(line);
      }
    }
  }

  @Test(timeout = 120000)
  public void testDumpTree_AfterSetup_NN_Crash() throws Exception {
    executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);
    fsn = cluster.getNamesystem();
    fsdir = fsn.getFSDirectory();

    final INode root = fsdir.getINode("/");

    LOG.info("Original tree");
    final StringBuffer b1 = root.dumpTreeRecursively();
    System.out.println("b1=" + b1);

    final BufferedReader in = new BufferedReader(new StringReader(b1.toString()));

    String line = in.readLine();
    checkClassName(line);

    for(; (line = in.readLine()) != null; ) {
      line = line.trim();
      if (!line.isEmpty() && !line.contains("snapshot")) {
        assertTrue("line=" + line,
            line.startsWith(INodeDirectory.DUMPTREE_LAST_ITEM)
                || line.startsWith(INodeDirectory.DUMPTREE_EXCEPT_LAST_ITEM)
        );
        checkClassName(line);
      }
    }
  }

  @Test(timeout = 120000)
  public void testDumpTree_AfterSetup_DN_Graceful() throws Exception {
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
    verifyClusterHealth(cluster, hdfs);

    final INode root = fsdir.getINode("/");

    LOG.info("Original tree");
    final StringBuffer b1 = root.dumpTreeRecursively();
    System.out.println("b1=" + b1);

    final BufferedReader in = new BufferedReader(new StringReader(b1.toString()));

    String line = in.readLine();
    checkClassName(line);

    for(; (line = in.readLine()) != null; ) {
      line = line.trim();
      if (!line.isEmpty() && !line.contains("snapshot")) {
        assertTrue("line=" + line,
            line.startsWith(INodeDirectory.DUMPTREE_LAST_ITEM)
                || line.startsWith(INodeDirectory.DUMPTREE_EXCEPT_LAST_ITEM)
        );
        checkClassName(line);
      }
    }
  }

  @Test(timeout = 120000)
  public void testDumpTree_AfterSetup_DN_Crash() throws Exception {
    executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
    verifyClusterHealth(cluster, hdfs);

    final INode root = fsdir.getINode("/");

    LOG.info("Original tree");
    final StringBuffer b1 = root.dumpTreeRecursively();
    System.out.println("b1=" + b1);

    final BufferedReader in = new BufferedReader(new StringReader(b1.toString()));

    String line = in.readLine();
    checkClassName(line);

    for(; (line = in.readLine()) != null; ) {
      line = line.trim();
      if (!line.isEmpty() && !line.contains("snapshot")) {
        assertTrue("line=" + line,
            line.startsWith(INodeDirectory.DUMPTREE_LAST_ITEM)
                || line.startsWith(INodeDirectory.DUMPTREE_EXCEPT_LAST_ITEM)
        );
        checkClassName(line);
      }
    }
  }

  @Test(timeout = 120000)
  public void testSkipQuotaCheck_AfterQuotaSet_NN_Graceful() throws Exception {
    try {
      hdfs.setQuota(sub2, 1, Long.MAX_VALUE);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);
      fsn = cluster.getNamesystem();
      fsdir = fsn.getFSDirectory();
      hdfs = cluster.getFileSystem();

      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
      fsdir.disableQuotaChecks();
      DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);

      hdfs.delete(file6, false);
      fsdir.enableQuotaChecks();
      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
    } finally {
      hdfs.delete(file6, false);
      hdfs.setQuota(sub2, Long.MAX_VALUE, Long.MAX_VALUE);
    }
  }

  @Test(timeout = 120000)
  public void testSkipQuotaCheck_AfterQuotaSet_NN_Crash() throws Exception {
    try {
      hdfs.setQuota(sub2, 1, Long.MAX_VALUE);

      executeRestart(cluster, RestartTarget.NAMENODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);
      fsn = cluster.getNamesystem();
      fsdir = fsn.getFSDirectory();
      hdfs = cluster.getFileSystem();

      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
      fsdir.disableQuotaChecks();
      DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);

      hdfs.delete(file6, false);
      fsdir.enableQuotaChecks();
      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
    } finally {
      hdfs.delete(file6, false);
      hdfs.setQuota(sub2, Long.MAX_VALUE, Long.MAX_VALUE);
    }
  }

  @Test(timeout = 120000)
  public void testSkipQuotaCheck_AfterQuotaSet_DN_Graceful() throws Exception {
    try {
      hdfs.setQuota(sub2, 1, Long.MAX_VALUE);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.GRACEFUL, true);
      verifyClusterHealth(cluster, hdfs);

      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
      fsdir.disableQuotaChecks();
      DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);

      hdfs.delete(file6, false);
      fsdir.enableQuotaChecks();
      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
    } finally {
      hdfs.delete(file6, false);
      hdfs.setQuota(sub2, Long.MAX_VALUE, Long.MAX_VALUE);
    }
  }

  @Test(timeout = 120000)
  public void testSkipQuotaCheck_AfterQuotaSet_DN_Crash() throws Exception {
    try {
      hdfs.setQuota(sub2, 1, Long.MAX_VALUE);

      executeRestart(cluster, RestartTarget.SINGLE_DATANODE, RestartMode.CRASH, true);
      verifyClusterHealth(cluster, hdfs);

      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
      fsdir.disableQuotaChecks();
      DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);

      hdfs.delete(file6, false);
      fsdir.enableQuotaChecks();
      try {
        DFSTestUtil.createFile(hdfs, file6, 1024, REPLICATION, seed);
        throw new IOException("The create should have failed.");
      } catch (NSQuotaExceededException qe) {
      }
    } finally {
      hdfs.delete(file6, false);
      hdfs.setQuota(sub2, Long.MAX_VALUE, Long.MAX_VALUE);
    }
  }
  
  static void checkClassName(String line) {
    int i = line.lastIndexOf('(');
    int j = line.lastIndexOf('@');
    final String classname = line.substring(i+1, j);
    assertTrue(classname.startsWith(INodeFile.class.getSimpleName())
        || classname.startsWith(INodeDirectory.class.getSimpleName()));
  }

  private static void verifyXAttrsPresent(List<XAttr> newXAttrs,
      final int num) {
    assertEquals("Unexpected number of XAttrs after multiset", num,
        newXAttrs.size());
    for (int i=0; i<num; i++) {
      XAttr search = generatedXAttrs.get(i);
      assertTrue("Did not find set XAttr " + search + " + after multiset",
          newXAttrs.contains(search));
    }
  }

  private static List<XAttr> generateXAttrs(final int numXAttrs) {
    List<XAttr> generatedXAttrs = Lists.newArrayListWithCapacity(numXAttrs);
    for (int i=0; i<numXAttrs; i++) {
      XAttr xAttr = (new XAttr.Builder())
          .setNameSpace(XAttr.NameSpace.SYSTEM)
          .setName("a" + i)
          .setValue(new byte[] { (byte) i, (byte) (i + 1), (byte) (i + 2) })
          .build();
      generatedXAttrs.add(xAttr);
    }
    return generatedXAttrs;
  }
}

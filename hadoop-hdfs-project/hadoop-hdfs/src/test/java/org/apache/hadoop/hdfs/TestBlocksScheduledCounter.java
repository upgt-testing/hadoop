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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.blockmanagement.*;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.apache.hadoop.hdfs.server.datanode.DataNodeJVMInterface;
import org.apache.hadoop.hdfs.server.datanode.DataNodeTestUtils;
import org.apache.hadoop.hdfs.server.namenode.NameNodeAdapter;
import org.junit.After;
import org.junit.Test;

/**
 * This class tests DatanodeDescriptor.getBlocksScheduled() at the
 * NameNode. This counter is supposed to keep track of blocks currently
 * scheduled to a datanode.
 */
public class TestBlocksScheduledCounter {
  MiniDFSClusterInJVM cluster = null;
  FileSystem fs = null;

  @After
  public void tearDown() throws IOException {
    if (fs != null) {
      fs.close();
      fs = null;
    }
    if(cluster!=null){
      cluster.shutdown();
      cluster = null;
    }
  }

  @Test
  public void testBlocksScheduledCounter() throws IOException {
    cluster = new MiniDFSClusterInJVM.Builder(new HdfsConfiguration()).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();
    
    //open a file an write a few bytes:
    FSDataOutputStream out = fs.create(new Path("/testBlockScheduledCounter"));
    for (int i=0; i<1024; i++) {
      out.write(i);
    }
    // flush to make sure a block is allocated.
    out.hflush();
    
    ArrayList<DatanodeDescriptorJVMInterface> dnList = new ArrayList<DatanodeDescriptorJVMInterface>();
    final DatanodeManagerJVMInterface dm = cluster.getNamesystem().getBlockManager(
        ).getDatanodeManager();
    dm.fetchDatanodesJVM(dnList, dnList, false);
    DatanodeDescriptorJVMInterface dn = dnList.get(0);
    
    assertEquals(1, dn.getBlocksScheduled());
   
    // close the file and the counter should go to zero.
    out.close();   
    assertEquals(0, dn.getBlocksScheduled());
  }

  /**
   * Abandon block should decrement the scheduledBlocks count for the dataNode.
   */
  @Test
  public void testScheduledBlocksCounterShouldDecrementOnAbandonBlock()
      throws Exception {
    cluster = new MiniDFSClusterInJVM.Builder(new HdfsConfiguration()).numDataNodes(
        2).build();

    cluster.waitActive();
    fs = cluster.getFileSystem();

    DatanodeManagerJVMInterface datanodeManager = cluster.getNamesystem().getBlockManager()
        .getDatanodeManager();
    ArrayList<DatanodeDescriptorJVMInterface> dnList = new ArrayList<DatanodeDescriptorJVMInterface>();
    datanodeManager.fetchDatanodesJVM(dnList, dnList, false);
    for (DatanodeDescriptorJVMInterface descriptor : dnList) {
      assertEquals("Blocks scheduled should be 0 for " + descriptor.getName(),
          0, descriptor.getBlocksScheduled());
    }

    cluster.getDataNodes().get(0).shutdown();
    // open a file an write a few bytes:
    FSDataOutputStream out = fs.create(new Path("/testBlockScheduledCounter"),
        (short) 2);
    for (int i = 0; i < 1024; i++) {
      out.write(i);
    }
    // flush to make sure a block is allocated.
    out.hflush();

    DatanodeDescriptorJVMInterface abandonedDn = datanodeManager.getDatanode(cluster
        .getDataNodes().get(0).getDatanodeId());
    assertEquals("for the abandoned dn scheduled counts should be 0", 0,
        abandonedDn.getBlocksScheduled());

    for (DatanodeDescriptorJVMInterface descriptor : dnList) {
      if (descriptor.equals(abandonedDn)) {
        continue;
      }
      assertEquals("Blocks scheduled should be 1 for " + descriptor.getName(),
          1, descriptor.getBlocksScheduled());
    }
    // close the file and the counter should go to zero.
    out.close();
    for (DatanodeDescriptorJVMInterface descriptor : dnList) {
      assertEquals("Blocks scheduled should be 0 for " + descriptor.getName(),
          0, descriptor.getBlocksScheduled());
    }
  }

  /**
   * Test if Block Scheduled counter decrement if scheduled blocks file is.
   * deleted
   * @throws Exception
   */
  @Test
  public void testScheduledBlocksCounterDecrementOnDeletedBlock()
      throws Exception {
    final Configuration conf = new HdfsConfiguration();
    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024);
    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1);
    cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(5).build();
    cluster.waitActive();
    BlockManagerJVMInterface bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      // 1. create a file
      Path filePath = new Path("/tmp.txt");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 3, 0L);

      // 2. disable the heartbeats
      for (DataNodeJVMInterface dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      DatanodeManagerJVMInterface datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptorJVMInterface> dnList =
          new ArrayList<DatanodeDescriptorJVMInterface>();
      datanodeManager.fetchDatanodesJVM(dnList, dnList, false);

      // 3. mark a couple of blocks as corrupt
      /*
      LocatedBlock block = NameNodeAdapter
          .getBlockLocations(cluster.getNameNode(), filePath.toString(), 0, 1)
          .get(0);
      DatanodeInfo[] locs = block.getLocations();
      cluster.getNamesystem().writeLock();
      try {
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), locs[0], "STORAGE_ID",
            "TEST");
        bm.findAndMarkBlockAsCorrupt(block.getBlock(), locs[1], "STORAGE_ID",
            "TEST");
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(1L, bm.getPendingReconstructionBlocksCount());
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      // 4. delete the file
      dfs.delete(filePath, true);
      BlockManagerTestUtil.waitForMarkedDeleteQueueIsEmpty(
          cluster.getNamesystem(0).getBlockManager());
      int blocksScheduled = 0;
      for (DatanodeDescriptor descriptor : dnList) {
        if (descriptor.getBlocksScheduled() != 0) {
          blocksScheduled += descriptor.getBlocksScheduled();
        }
      }
      assertEquals(0, blocksScheduled);
       */
    } finally {
      cluster.shutdown();
    }
  }

  /**
   * Test Block Scheduled counter on truncating a file.
   * @throws Exception
   */
  @Test
  public void testBlocksScheduledCounterOnTruncate() throws Exception {
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_INTERVAL_SECONDS_KEY, 1);
    cluster = new MiniDFSClusterInJVM.Builder(conf).numDataNodes(3).build();
    cluster.waitActive();
    BlockManagerJVMInterface bm = cluster.getNamesystem().getBlockManager();
    try {
      DistributedFileSystem dfs = cluster.getFileSystem();
      // 1. stop a datanode
      cluster.stopDataNode(0);

      // 2. create a file
      Path filePath = new Path("/tmp");
      DFSTestUtil.createFile(dfs, filePath, 1024, (short) 3, 0L);

      DatanodeManagerJVMInterface datanodeManager =
          cluster.getNamesystem().getBlockManager().getDatanodeManager();
      ArrayList<DatanodeDescriptorJVMInterface> dnList =
          new ArrayList<DatanodeDescriptorJVMInterface>();
      datanodeManager.fetchDatanodesJVM(dnList, dnList, false);

      // 3. restart the stopped datanode
      cluster.restartDataNode(0);

      // 4. disable the heartbeats
      for (DataNodeJVMInterface dn : cluster.getDataNodes()) {
        DataNodeTestUtils.setHeartbeatsDisabledForTests(dn, true);
      }

      cluster.getNamesystem().writeLock();
      try {
        BlockManagerTestUtil.computeAllPendingWork(bm);
        BlockManagerTestUtil.updateState(bm);
        assertEquals(1L, bm.getPendingReconstructionBlocksCount());
      } finally {
        cluster.getNamesystem().writeUnlock();
      }

      // 5.truncate the file whose block exists in pending reconstruction
      dfs.truncate(filePath, 10);
      int blocksScheduled = 0;
      for (DatanodeDescriptorJVMInterface descriptor : dnList) {
        if (descriptor.getBlocksScheduled() != 0) {
          blocksScheduled += descriptor.getBlocksScheduled();
        }
      }
      assertEquals(0, blocksScheduled);
    } finally {
      cluster.shutdown();
    }
  }
}
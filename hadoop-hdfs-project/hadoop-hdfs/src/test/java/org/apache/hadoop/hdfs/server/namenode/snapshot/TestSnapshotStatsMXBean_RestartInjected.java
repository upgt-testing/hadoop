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
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestSnapshotStatsMXBean_RestartInjected {

  /**
   * Test getting SnapshotStatsMXBean information
   */
  @Test
  public void testSnapshotStatsMXBeanInfo() throws Exception {
    Configuration conf = new Configuration();
    MiniDFSCluster cluster = null;
    String pathName = "/snapshot";
    Path path = new Path(pathName);

    try {
      cluster = new MiniDFSCluster.Builder(conf).build();
      cluster.waitActive();

      SnapshotManager sm = cluster.getNamesystem().getSnapshotManager();
      DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
      dfs.mkdirs(path);
      RestartFramework.at("after_mkdir")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      dfs.allowSnapshot(path);
      RestartFramework.at("after_allow_snapshot")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      dfs.createSnapshot(path);
      RestartFramework.at("after_create_snapshot")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();

      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      ObjectName mxbeanName = new ObjectName(
          "Hadoop:service=NameNode,name=SnapshotInfo");

      CompositeData[] directories =
          (CompositeData[]) mbs.getAttribute(
              mxbeanName, "SnapshottableDirectories");
      int numDirectories = Array.getLength(directories);
      assertEquals(sm.getNumSnapshottableDirs(), numDirectories);
      RestartFramework.at("after_query_directories")
          .on(cluster)
          .restart("namenode")
          .withIndex(0)
          .withMode(RestartMode.GRACEFUL)
          .execute();
      CompositeData[] snapshots =
          (CompositeData[]) mbs.getAttribute(mxbeanName, "Snapshots");
      int numSnapshots = Array.getLength(snapshots);
      assertEquals(sm.getNumSnapshots(), numSnapshots);

      CompositeData d = (CompositeData) Array.get(directories, 0);
      CompositeData s = (CompositeData) Array.get(snapshots, 0);
      assertTrue(((String) d.get("path")).contains(pathName));
      assertTrue(((String) s.get("snapshotDirectory")).contains(pathName));
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}

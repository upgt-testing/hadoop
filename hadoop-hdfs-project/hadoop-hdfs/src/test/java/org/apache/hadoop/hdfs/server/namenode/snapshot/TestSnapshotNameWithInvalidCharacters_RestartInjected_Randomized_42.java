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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.ipc.RemoteException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestSnapshotNameWithInvalidCharacters_RestartInjected_Randomized_42 {

    private static final long SEED = 0;

    private static final short REPLICATION = 1;

    private static final int BLOCKSIZE = 1024;

    private static final Configuration conf = new Configuration();

    private static MiniDFSCluster cluster;

    private static DistributedFileSystem hdfs;

    private final Path dir1 = new Path("/");

    private final String file1Name = "file1";

    private final String snapshot1 = "a:b:c";

    private final String snapshot2 = "a/b/c";

    @Before
    public void setUp() throws Exception {
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION).build();
        cluster.waitActive();
        hdfs = cluster.getFileSystem();
    }

    @After
    public void tearDown() throws Exception {
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }
    }

    @Test
    public void TestSnapshotWithInvalidName() throws Exception {
        Path file1 = new Path(dir1, file1Name);
        RestartFramework.at("after_create_snapshot_attempt").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);
        hdfs.allowSnapshot(dir1);
        RestartFramework.at("after_allow_snapshot").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            hdfs.createSnapshot(dir1, snapshot1);
        } catch (RemoteException e) {
        }
    }

    @Test
    public void TestSnapshotWithInvalidName1() throws Exception {
        Path file1 = new Path(dir1, file1Name);
        DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, SEED);
        RestartFramework.at("after_create_snapshot_attempt").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        hdfs.allowSnapshot(dir1);
        RestartFramework.at("after_allow_snapshot").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            hdfs.createSnapshot(dir1, snapshot2);
        } catch (RemoteException e) {
        }
    }
}

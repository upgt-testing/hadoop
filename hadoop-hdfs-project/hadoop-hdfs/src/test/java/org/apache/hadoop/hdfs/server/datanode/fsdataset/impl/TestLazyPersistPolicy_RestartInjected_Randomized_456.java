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
package org.apache.hadoop.hdfs.server.datanode.fsdataset.impl;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class TestLazyPersistPolicy_RestartInjected_Randomized_456 extends LazyPersistTestCase {

    @Test
    public void testPolicyNotSetByDefault() throws IOException {
        getClusterBuilder().build();
        final String METHOD_NAME = GenericTestUtils.getMethodName();
        Path path = new Path("/" + METHOD_NAME + ".dat");
        makeTestFile(path, 0, false);
        RestartFramework.at("after_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        HdfsFileStatus status = client.getFileInfo(path.toString());
        RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertThat(status.getStoragePolicy(), not(LAZY_PERSIST_POLICY_ID));
    }

    @Test
    public void testPolicyPropagation() throws IOException {
        getClusterBuilder().build();
        RestartFramework.at("after_lazy_persist_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final String METHOD_NAME = GenericTestUtils.getMethodName();
        Path path = new Path("/" + METHOD_NAME + ".dat");
        RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        makeTestFile(path, 0, true);
        HdfsFileStatus status = client.getFileInfo(path.toString());
        assertThat(status.getStoragePolicy(), is(LAZY_PERSIST_POLICY_ID));
    }

    @Test
    public void testPolicyPersistenceInEditLog() throws IOException {
        getClusterBuilder().build();
        final String METHOD_NAME = GenericTestUtils.getMethodName();
        Path path = new Path("/" + METHOD_NAME + ".dat");
        makeTestFile(path, 0, true);
        RestartFramework.at("after_namenode_restart").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_lazy_persist_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        cluster.restartNameNode(true);
        HdfsFileStatus status = client.getFileInfo(path.toString());
        assertThat(status.getStoragePolicy(), is(LAZY_PERSIST_POLICY_ID));
    }

    @Test
    public void testPolicyPersistenceInFsImage() throws IOException {
        RestartFramework.at("after_cluster_start").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_namenode_restart").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        getClusterBuilder().build();
        RestartFramework.at("after_lazy_persist_file_creation").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        final String METHOD_NAME = GenericTestUtils.getMethodName();
        Path path = new Path("/" + METHOD_NAME + ".dat");
        RestartFramework.at("after_checkpoint").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        makeTestFile(path, 0, true);
        fs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_ENTER);
        fs.saveNamespace();
        fs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_LEAVE);
        cluster.restartNameNode(true);
        HdfsFileStatus status = client.getFileInfo(path.toString());
        assertThat(status.getStoragePolicy(), is(LAZY_PERSIST_POLICY_ID));
    }
}

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
package org.apache.hadoop.fs;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.security.auth.login.LoginException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestFcHdfsCreateMkdir_RestartInjected_Randomized_42 extends FileContextCreateMkdirBaseTest {

    private static MiniDFSCluster cluster;

    private static Path defaultWorkingDirectory;

    @Override
    protected FileContextTestHelper createFileContextHelper() {
        return new FileContextTestHelper("/tmp/TestFcHdfsCreateMkdir_RestartInjected");
    }

    @BeforeClass
    public static void clusterSetupAtBegining() throws IOException, LoginException, URISyntaxException {
        Configuration conf = new HdfsConfiguration();
        cluster = new MiniDFSCluster.Builder(conf).numDataNodes(2).build();
        fc = FileContext.getFileContext(cluster.getURI(0), conf);
        defaultWorkingDirectory = fc.makeQualified(new Path("/user/" + UserGroupInformation.getCurrentUser().getShortUserName()));
        fc.mkdir(defaultWorkingDirectory, FileContext.DEFAULT_PERM, true);
    }

    @AfterClass
    public static void ClusterShutdownAtEnd() throws IOException {
        if (cluster != null) {
            cluster.shutdown();
        }
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @Override
    public void testMkdirRecursiveWithNonExistingDir() throws IOException {
        RestartFramework.at("before_mkdir_recursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirRecursiveWithNonExistingDir();
        RestartFramework.at("after_mkdir_recursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMkdirRecursiveWithExistingDir() throws IOException {
        RestartFramework.at("after_mkdir_existing").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_mkdir_existing").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirRecursiveWithExistingDir();
    }

    @Test
    @Override
    public void testMkdirNonRecursiveWithNonExistingDir() {
        RestartFramework.at("after_mkdir_nonrecursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_mkdir_nonrecursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirNonRecursiveWithNonExistingDir();
    }

    @Test
    @Override
    public void testMkdirNonRecursiveWithExistingDir() throws IOException {
        super.testMkdirNonRecursiveWithExistingDir();
        RestartFramework.at("before_mkdir_nonrecursive_existing").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_mkdir_nonrecursive_existing").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testCreateRecursiveWithNonExistingDir() throws IOException {
        RestartFramework.at("after_create_recursive").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_create_recursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testCreateRecursiveWithNonExistingDir();
    }

    @Test
    @Override
    public void testCreateRecursiveWithExistingDir() throws IOException {
        super.testCreateRecursiveWithExistingDir();
        RestartFramework.at("before_create_existing_dir").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_create_existing_dir").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testCreateNonRecursiveWithNonExistingDir() {
        RestartFramework.at("before_create_nonrecursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_create_nonrecursive").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testCreateNonRecursiveWithNonExistingDir();
    }

    @Test
    @Override
    public void testCreateNonRecursiveWithExistingDir() throws IOException {
        super.testCreateNonRecursiveWithExistingDir();
        RestartFramework.at("before_create_nonrecursive_existing").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_create_nonrecursive_existing").on(cluster).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMkdirRecursiveWithExistingFile() throws IOException {
        RestartFramework.at("before_mkdir_existing_file").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_mkdir_existing_file").on(cluster).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirRecursiveWithExistingFile();
    }
}

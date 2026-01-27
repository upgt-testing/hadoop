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
package org.apache.hadoop.fs.contract.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathHandle;
import org.apache.hadoop.fs.contract.AbstractContractPathHandleTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

public class TestHDFSContractPathHandle_RestartInjected_Randomized_123 extends AbstractContractPathHandleTest {

    public TestHDFSContractPathHandle_RestartInjected_Randomized_123(String testname, Options.HandleOpt[] opts, boolean serialized) {
        super(testname, opts, serialized);
    }

    @BeforeClass
    public static void createCluster() throws IOException {
        HDFSContract.createCluster();
    }

    @AfterClass
    public static void teardownCluster() throws IOException {
        HDFSContract.destroyCluster();
    }

    @Override
    protected AbstractFSContract createContract(Configuration conf) {
        return new HDFSContract(conf);
    }

    @Test
    public void testGetPathHandle() throws Exception {
        RestartFramework.at("before_file_creation").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("test getting path handle with restart injection");
        FileSystem fs = getFileSystem();
        Path path = path("testGetPathHandle");
        byte[] data = dataset(1024, 'a', 'z');
        ContractTestUtils.writeDataset(fs, path, data, data.length, 1024, false);
        assertPathExists("file not created", path);
        PathHandle pathHandle = fs.getPathHandle(fs.getFileStatus(path));
        RestartFramework.at("after_file_creation").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_get_path_handle").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertNotNull("path handle is null", pathHandle);
    }

    @Test
    public void testOpenWithPathHandle() throws Exception {
        describe("test opening file with path handle and restart injection");
        FileSystem fs = getFileSystem();
        Path path = path("testOpenWithPathHandle");
        byte[] data = dataset(256, 'a', 'z');
        ContractTestUtils.writeDataset(fs, path, data, data.length, 1024, false);
        PathHandle pathHandle = fs.getPathHandle(fs.getFileStatus(path));
        RestartFramework.at("after_open_with_handle").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FSDataInputStream in = fs.open(pathHandle, 4096);
        byte[] result = new byte[data.length];
        RestartFramework.at("after_get_handle").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_create_for_handle").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        in.readFully(result);
        in.close();
        assertArrayEquals(data, result);
    }
}

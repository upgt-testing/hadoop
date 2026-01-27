/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.hadoop.fs.contract.router.web;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractOpenTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

/**
 * Test open operations on a Router WebHDFS FS.
 */
public class TestRouterWebHDFSContractOpen_RestartInjected_Randomized_456 extends AbstractContractOpenTest {

    @BeforeClass
    public static void createCluster() throws IOException {
        RouterWebHDFSContract.createCluster();
    }

    @AfterClass
    public static void teardownCluster() throws IOException {
        RouterWebHDFSContract.destroyCluster();
    }

    @Override
    protected AbstractFSContract createContract(Configuration conf) {
        return new RouterWebHDFSContract(conf);
    }

    @Override
    @Test
    public void testOpenReadDir() throws Throwable {
        // WebHDFS itself allows open read on directory, we may need to
        // fix this first before make this test work
    }

    @Override
    @Test
    public void testOpenReadDirWithChild() throws Throwable {
        // WebHDFS itself allows open read on directory, we may need to
        // fix this first before make this test work
    }

    @Test
    @Override
    public void testOpenFile() throws Throwable {
        describe("test open file with restart injection");
        FileSystem fs = getFileSystem();
        RestartFramework.at("before_file_creation").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Path path = path("testOpenFile");
        byte[] data = dataset(1024, 'a', 'z');
        RestartFramework.at("after_file_creation").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.writeDataset(fs, path, data, data.length, 1024, false);
        assertPathExists("file not created", path);
        RestartFramework.at("after_read_file").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FSDataInputStream in = fs.open(path);
        RestartFramework.at("after_open_file").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        byte[] result = new byte[data.length];
        in.readFully(0, result);
        in.close();
        assertArrayEquals(data, result);
    }

    @Test
    @Override
    public void testOpenReadClose() throws Throwable {
        RestartFramework.at("before_create_for_read").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("test open, read, and close file with restart injection");
        RestartFramework.at("after_create_before_open").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FileSystem fs = getFileSystem();
        Path path = path("testOpenReadClose");
        byte[] data = dataset(256, 'a', 'z');
        RestartFramework.at("after_close").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.writeDataset(fs, path, data, data.length, 1024, false);
        FSDataInputStream in = fs.open(path);
        byte[] result = new byte[data.length];
        in.readFully(result);
        RestartFramework.at("after_read_before_close").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        in.close();
        assertArrayEquals(data, result);
    }
}

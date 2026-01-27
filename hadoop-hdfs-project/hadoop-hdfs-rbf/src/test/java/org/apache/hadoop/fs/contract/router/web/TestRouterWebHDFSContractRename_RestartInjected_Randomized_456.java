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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractRenameTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

/**
 * Test rename operations on a Router WebHDFS FS.
 */
public class TestRouterWebHDFSContractRename_RestartInjected_Randomized_456 extends AbstractContractRenameTest {

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

    @Test
    @Override
    public void testRenameFile() throws Throwable {
        describe("test renaming a file with restart injection");
        FileSystem fs = getFileSystem();
        RestartFramework.at("before_file_creation").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Path src = path("testRenameFile/src");
        Path dst = path("testRenameFile/dst");
        byte[] data = dataset(256, 'a', 'z');
        RestartFramework.at("after_file_creation").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.writeDataset(fs, src, data, data.length, 1024, false);
        assertPathExists("source file not created", src);
        RestartFramework.at("after_rename").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_rename").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        boolean renamed = fs.rename(src, dst);
        assertTrue("rename failed", renamed);
        assertPathDoesNotExist("source still exists", src);
        assertPathExists("destination does not exist", dst);
    }

    @Test
    @Override
    public void testRenameFileOverExistingFile() throws Throwable {
        describe("test renaming a file over existing file with restart injection");
        RestartFramework.at("before_create_src_dst").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FileSystem fs = getFileSystem();
        Path src = path("testRenameFileOverExistingFile/src");
        Path dst = path("testRenameFileOverExistingFile/dst");
        RestartFramework.at("after_rename_over_existing").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        byte[] srcData = dataset(256, 'a', 'z');
        byte[] dstData = dataset(128, '0', '9');
        ContractTestUtils.writeDataset(fs, src, srcData, srcData.length, 1024, false);
        ContractTestUtils.writeDataset(fs, dst, dstData, dstData.length, 1024, false);
        RestartFramework.at("after_create_both_files").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        boolean renamed = fs.rename(src, dst);
        assertPathDoesNotExist("source still exists", src);
        assertPathExists("destination does not exist", dst);
    }

    @Test
    @Override
    public void testRenameDirectory() throws Throwable {
        describe("test renaming a directory with restart injection");
        FileSystem fs = getFileSystem();
        Path src = path("testRenameDirectory/src");
        Path dst = path("testRenameDirectory/dst");
        RestartFramework.at("before_mkdir").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_mkdir_before_rename").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        fs.mkdirs(src);
        assertPathExists("source directory not created", src);
        RestartFramework.at("after_rename_directory").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        boolean renamed = fs.rename(src, dst);
        assertTrue("rename failed", renamed);
        assertPathDoesNotExist("source still exists", src);
        assertPathExists("destination does not exist", dst);
    }
}

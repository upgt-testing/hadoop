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
package org.apache.hadoop.fs.contract.router;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.contract.AbstractContractRenameTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

/**
 * Test rename operations on the Router-based FS.
 */
public class TestRouterHDFSContractRename_RestartInjected_Randomized_123 extends AbstractContractRenameTest {

    @BeforeClass
    public static void createCluster() throws IOException {
        RouterHDFSContract.createCluster();
    }

    @AfterClass
    public static void teardownCluster() throws IOException {
        RouterHDFSContract.destroyCluster();
    }

    @Override
    protected AbstractFSContract createContract(Configuration conf) {
        return new RouterHDFSContract(conf);
    }

    @Test
    @Override
    public void testRenameNewFileSameDir() throws Throwable {
        super.testRenameNewFileSameDir();
        RestartFramework.at("before_rename_file_same_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_rename_file_same_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testRenameNonexistentFile() throws Throwable {
        super.testRenameNonexistentFile();
    }

    @Test
    @Override
    public void testRenameFileOverExistingFile() throws Throwable {
        RestartFramework.at("before_rename_over_existing").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testRenameFileOverExistingFile();
    }

    @Test
    @Override
    public void testRenameDirIntoExistingDir() throws Throwable {
        RestartFramework.at("before_rename_dir_into_existing").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testRenameDirIntoExistingDir();
        RestartFramework.at("after_rename_dir_into_existing").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testRenameFileNonexistentDir() throws Throwable {
        super.testRenameFileNonexistentDir();
    }

    @Test
    @Override
    public void testRenameWithNonEmptySubDir() throws Throwable {
        RestartFramework.at("before_rename_nonempty_subdir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testRenameWithNonEmptySubDir();
        RestartFramework.at("after_rename_nonempty_subdir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testRenamePopulatesDirectoryAncestors() throws Throwable {
        RestartFramework.at("before_rename_dir_ancestors").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testRenamePopulatesDirectoryAncestors();
    }

    @Test
    @Override
    public void testRenamePopulatesFileAncestors() throws Throwable {
        RestartFramework.at("before_rename_file_ancestors").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testRenamePopulatesFileAncestors();
    }

    @Test
    @Override
    public void testRenameFileUnderFile() throws Throwable {
        super.testRenameFileUnderFile();
    }

    @Test
    @Override
    public void testRenameFileUnderFileSubdir() throws Throwable {
        super.testRenameFileUnderFileSubdir();
    }
}

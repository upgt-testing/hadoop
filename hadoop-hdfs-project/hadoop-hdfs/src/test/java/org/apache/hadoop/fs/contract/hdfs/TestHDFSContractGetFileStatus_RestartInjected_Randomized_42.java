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
package org.apache.hadoop.fs.contract.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractGetFileStatusTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

public class TestHDFSContractGetFileStatus_RestartInjected_Randomized_42 extends AbstractContractGetFileStatusTest {

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
    public void testGetFileStatusFile() throws Throwable {
        RestartFramework.at("before_file_creation").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("test getFileStatus on a file with restart injection");
        FileSystem fs = getFileSystem();
        RestartFramework.at("after_get_file_status").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_file_creation").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Path dir = path("testGetFileStatusFile");
        mkdirs(dir);
        Path path = new Path(dir, "file");
        ContractTestUtils.writeTextFile(fs, path, "test file content", true);
        assertPathExists("file not created", path);
        FileStatus status = fs.getFileStatus(path);
        assertIsFile(path);
    }

    @Test
    public void testGetFileStatusDir() throws Throwable {
        RestartFramework.at("after_dir_creation").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("test getFileStatus on a directory with restart injection");
        FileSystem fs = getFileSystem();
        Path dir = path("testGetFileStatusDir");
        RestartFramework.at("before_dir_creation").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("after_get_dir_status").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        mkdirs(dir);
        assertPathExists("dir not created", dir);
        FileStatus status = fs.getFileStatus(dir);
        assertIsDirectory(dir);
    }
}

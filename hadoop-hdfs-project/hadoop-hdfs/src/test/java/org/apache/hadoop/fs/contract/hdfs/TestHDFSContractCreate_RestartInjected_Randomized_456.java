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
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractCreateTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.skip;
import static org.apache.hadoop.fs.contract.ContractTestUtils.writeDataset;

public class TestHDFSContractCreate_RestartInjected_Randomized_456 extends AbstractContractCreateTest {

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
    @Override
    public void testCreateNewFile() throws Throwable {
        describe("Foundational 'create a file' test with restart injection");
        Path path = path("testCreateNewFile", true);
        byte[] data = dataset(256, 'a', 'z');
        writeDataset(getFileSystem(), path, data, data.length, 1024 * 1024, false, true);
        RestartFramework.at("after_file_write").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        RestartFramework.at("before_file_creation").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.verifyFileContents(getFileSystem(), path, data);
    }

    @Test
    @Override
    public void testCreatedFileIsVisibleOnFlush() throws Throwable {
        describe("verify that a newly created file exists once a flush has taken place");
        Path path = path("testCreatedFileIsVisibleOnFlush");
        FileSystem fs = getFileSystem();
        FSDataOutputStream out = fs.create(path, false, 4096, (short) 1, 1024);
        RestartFramework.at("after_flush").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        out.write('a');
        RestartFramework.at("after_file_create_before_write").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        out.flush();
        if (!fs.exists(path)) {
            if (isSupported(IS_BLOBSTORE) || isSupported(CREATE_VISIBILITY_DELAYED)) {
                skip("For object store or some file systems, newly created files are not immediately visible");
            }
            assertPathExists("expected path to be visible before file closed", path);
        }
        out.close();
    }
}

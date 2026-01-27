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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractConcatTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;
import static org.apache.hadoop.fs.contract.ContractTestUtils.assertFileHasLength;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;

/**
 * Test concat operations on the Router-based WebHDFS FS.
 */
public class TestRouterWebHDFSContractConcat_RestartInjected_Randomized_456 extends AbstractContractConcatTest {

    private Path testPath;

    private Path srcFile;

    private Path target;

    @BeforeClass
    public static void createCluster() throws IOException {
        RouterWebHDFSContract.createCluster();
        // perform a simple operation on the cluster to verify it is up
        RouterWebHDFSContract.getFileSystem().getDefaultBlockSize(new Path("/"));
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
    public void setup() throws Exception {
        super.setup();
        testPath = path("test");
        srcFile = new Path(testPath, "small.txt");
        target = new Path(testPath, "target");
    }

    @Test
    @Override
    public void testConcatFileOnFile() throws Throwable {
        byte[] block = dataset(TEST_FILE_LEN, 0, 255);
        createFile(getFileSystem(), target, false, block);
        RestartFramework.at("after_target_file_creation").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        getFileSystem().concat(target, new Path[] { srcFile });
        RestartFramework.at("after_concat_operation").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertFileHasLength(getFileSystem(), target, TEST_FILE_LEN * 2);
        RestartFramework.at("before_concat_operation").on(RouterWebHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.validateFileContent(ContractTestUtils.readDataset(getFileSystem(), target, TEST_FILE_LEN * 2), new byte[][] { block, block });
    }
}

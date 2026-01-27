/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License. See accompanying LICENSE file.
 */
package org.apache.hadoop.fs.contract.router;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractDeleteTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

/**
 * Test secure delete operations on the Router-based FS.
 */
public class TestRouterHDFSContractDeleteSecure_RestartInjected_Randomized_42 extends AbstractContractDeleteTest {

    @BeforeClass
    public static void createCluster() throws Exception {
        RouterHDFSContract.createCluster(true);
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
    public void testDeleteSingleFile() throws Throwable {
        RestartFramework.at("before_delete_single_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Path path = path("testDeleteSingleFile/d1/d2");
        mkdirs(path);
        Path file = new Path(path, "childfile");
        ContractTestUtils.writeTextFile(getFileSystem(), file, "single file to be deleted.", true);
        assertPathExists("single file not created", file);
        RestartFramework.at("after_delete_single_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertDeleted(file, false);
    }

    @Test
    @Override
    public void testDeleteNonEmptyDirRecursive() throws Throwable {
        RestartFramework.at("before_delete_directory").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        Path path = path("testDeleteNonEmptyDirRecursive");
        mkdirs(path);
        Path file = new Path(path, "childfile");
        ContractTestUtils.writeTextFile(getFileSystem(), file, "goodbye, world", true);
        RestartFramework.at("after_delete_directory").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertDeleted(path, true);
        assertPathDoesNotExist("not deleted", file);
    }
}

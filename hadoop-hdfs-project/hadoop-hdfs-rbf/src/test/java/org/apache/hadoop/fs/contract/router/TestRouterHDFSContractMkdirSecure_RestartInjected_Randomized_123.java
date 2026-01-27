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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.contract.AbstractContractMkdirTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;

public class TestRouterHDFSContractMkdirSecure_RestartInjected_Randomized_123 extends AbstractContractMkdirTest {

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
    public void testMkDirRmDir() throws Throwable {
        super.testMkDirRmDir();
        RestartFramework.at("after_mkdir_rmdir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMkDirRmRfDir() throws Throwable {
        RestartFramework.at("after_mkdir_recursive_delete").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkDirRmRfDir();
    }

    @Test
    @Override
    public void testNoMkdirOverFile() throws Throwable {
        super.testNoMkdirOverFile();
        RestartFramework.at("after_mkdir_over_file_test").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMkdirOverParentFile() throws Throwable {
        RestartFramework.at("after_mkdir_over_parent_file_test").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirOverParentFile();
    }

    @Test
    @Override
    public void testMkdirSlashHandling() throws Throwable {
        RestartFramework.at("after_mkdir_slash_handling").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testMkdirSlashHandling();
    }

    @Test
    @Override
    public void testMkdirsPopulatingAllNonexistentAncestors() throws IOException {
        super.testMkdirsPopulatingAllNonexistentAncestors();
        RestartFramework.at("after_mkdir_nested_ancestors").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testMkdirsDoesNotRemoveParentDirectories() throws IOException {
        super.testMkdirsDoesNotRemoveParentDirectories();
        RestartFramework.at("after_mkdir_preserve_parents").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testCreateDirWithExistingDir() throws Exception {
        RestartFramework.at("after_mkdir_existing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testCreateDirWithExistingDir();
    }
}

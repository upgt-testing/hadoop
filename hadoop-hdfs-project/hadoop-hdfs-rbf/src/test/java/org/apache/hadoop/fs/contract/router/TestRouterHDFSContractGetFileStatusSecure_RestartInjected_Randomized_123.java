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
import org.apache.hadoop.fs.contract.AbstractContractGetFileStatusTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

public class TestRouterHDFSContractGetFileStatusSecure_RestartInjected_Randomized_123 extends AbstractContractGetFileStatusTest {

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
    public void testGetFileStatusRoot() throws Throwable {
        RestartFramework.at("before_get_root_status").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testGetFileStatusRoot();
    }

    @Test
    @Override
    public void testListStatusEmptyDirectory() throws IOException {
        RestartFramework.at("after_list_empty_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusEmptyDirectory();
    }

    @Test
    @Override
    public void testListFilesEmptyDirectoryNonrecursive() throws IOException {
        super.testListFilesEmptyDirectoryNonrecursive();
        RestartFramework.at("after_list_files_nonrecursive").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListFilesEmptyDirectoryRecursive() throws IOException {
        RestartFramework.at("after_list_files_recursive").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListFilesEmptyDirectoryRecursive();
    }

    @Test
    @Override
    public void testListLocatedStatusEmptyDirectory() throws IOException {
        RestartFramework.at("after_list_located_status_empty").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListLocatedStatusEmptyDirectory();
    }

    @Test
    @Override
    public void testComplexDirActions() throws Throwable {
        RestartFramework.at("before_complex_dir_operations").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testComplexDirActions();
        RestartFramework.at("after_complex_dir_operations").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListFilesNoDir() throws Throwable {
        RestartFramework.at("before_list_files_missing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListFilesNoDir();
    }

    @Test
    @Override
    public void testListStatusIteratorNoDir() throws Throwable {
        RestartFramework.at("before_list_status_iterator_missing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusIteratorNoDir();
    }

    @Test
    @Override
    public void testLocatedStatusNoDir() throws Throwable {
        RestartFramework.at("before_located_status_missing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testLocatedStatusNoDir();
    }

    @Test
    @Override
    public void testListStatusNoDir() throws Throwable {
        RestartFramework.at("before_list_status_missing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusNoDir();
    }

    @Test
    @Override
    public void testListStatusFilteredNoDir() throws Throwable {
        RestartFramework.at("before_list_status_filtered_missing_dir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusFilteredNoDir();
    }

    @Test
    @Override
    public void testListStatusFilteredFile() throws Throwable {
        super.testListStatusFilteredFile();
        RestartFramework.at("after_list_status_filtered_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListStatusFile() throws Throwable {
        RestartFramework.at("after_list_status_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusFile();
    }

    @Test
    @Override
    public void testListStatusIteratorFile() throws Throwable {
        RestartFramework.at("after_list_status_iterator_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListStatusIteratorFile();
    }

    @Test
    @Override
    public void testListFilesFile() throws Throwable {
        super.testListFilesFile();
        RestartFramework.at("after_list_files_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListFilesFileRecursive() throws Throwable {
        super.testListFilesFileRecursive();
        RestartFramework.at("after_list_files_file_recursive").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListLocatedStatusFile() throws Throwable {
        RestartFramework.at("after_list_located_status_file").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListLocatedStatusFile();
    }

    @Test
    @Override
    public void testListStatusFiltering() throws Throwable {
        super.testListStatusFiltering();
        RestartFramework.at("after_list_status_filtering").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
    }

    @Test
    @Override
    public void testListLocatedStatusFiltering() throws Throwable {
        RestartFramework.at("after_list_located_status_filtering").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        super.testListLocatedStatusFiltering();
    }
}

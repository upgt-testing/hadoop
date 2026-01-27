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
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractAppendTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.touch;

/**
 * Test secure append operations on the Router-based FS.
 */
public class TestRouterHDFSContractAppendSecure_RestartInjected_Randomized_123 extends AbstractContractAppendTest {

    private Path testPath;

    private Path target;

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

    @Override
    public void setup() throws Exception {
        super.setup();
        testPath = path("test");
        target = new Path(testPath, "target");
    }

    @Test
    @Override
    public void testAppendToEmptyFile() throws Throwable {
        RestartFramework.at("after_append_open").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        touch(getFileSystem(), target);
        byte[] dataset = dataset(256, 'a', 'z');
        RestartFramework.at("before_append_close").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FSDataOutputStream outputStream = getFileSystem().append(target);
        RestartFramework.at("after_create_empty_file").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        outputStream.write(dataset);
        outputStream.close();
        byte[] bytes = ContractTestUtils.readDataset(getFileSystem(), target, dataset.length);
        ContractTestUtils.compareByteArrays(dataset, bytes, dataset.length);
    }

    @Test
    @Override
    public void testAppendToExistingFile() throws Throwable {
        RestartFramework.at("after_append_open_existing").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        byte[] original = dataset(8192, 'A', 'Z');
        byte[] appended = dataset(8192, '0', '9');
        createFile(getFileSystem(), target, false, original);
        FSDataOutputStream out = getFileSystem().append(target);
        out.write(appended);
        out.close();
        RestartFramework.at("before_close_appended").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        byte[] bytes = ContractTestUtils.readDataset(getFileSystem(), target, original.length + appended.length);
        RestartFramework.at("after_file_creation").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.validateFileContent(bytes, new byte[][] { original, appended });
    }

    @Test
    @Override
    public void testRenameFileBeingAppended() throws Throwable {
        touch(getFileSystem(), target);
        assertPathExists("original file does not exist", target);
        byte[] dataset = dataset(256, 'a', 'z');
        FSDataOutputStream outputStream = getFileSystem().append(target);
        RestartFramework.at("after_append_before_write").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        if (isSupported(CREATE_VISIBILITY_DELAYED)) {
            Thread.sleep(100);
        }
        outputStream.write(dataset);
        Path renamed = new Path(testPath, "renamed");
        RestartFramework.at("before_rename_during_append").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        rename(target, renamed);
        outputStream.close();
        String listing = ls(testPath);
        assertPathExists("renamed destination file does not exist", renamed);
        assertPathDoesNotExist("Source file found after rename during append:\n" + listing, target);
        byte[] bytes = ContractTestUtils.readDataset(getFileSystem(), renamed, dataset.length);
        ContractTestUtils.compareByteArrays(dataset, bytes, dataset.length);
    }
}

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
import org.apache.hadoop.fs.contract.AbstractContractRootDirectoryTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.test.LambdaTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assertj.core.api.Assertions;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.deleteChildren;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dumpStats;
import static org.apache.hadoop.fs.contract.ContractTestUtils.iteratorToList;
import static org.apache.hadoop.fs.contract.ContractTestUtils.listChildren;
import static org.apache.hadoop.fs.contract.ContractTestUtils.toList;
import static org.apache.hadoop.fs.contract.ContractTestUtils.treeWalk;

public class TestHDFSContractRootDirectory_RestartInjected_Randomized_123 extends AbstractContractRootDirectoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestHDFSContractRootDirectory_RestartInjected.class);

    public static final int OBJECTSTORE_RETRY_TIMEOUT = 30000;

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
    public void testMkDirDepth1() throws Throwable {
        FileSystem fs = getFileSystem();
        Path dir = new Path("/testmkdirdepth1");
        assertPathDoesNotExist("directory already exists", dir);
        fs.mkdirs(dir);
        RestartFramework.at("after_mkdir").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(dir);
        assertPathExists("directory already exists", dir);
        RestartFramework.at("before_delete").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertDeleted(dir, true);
    }

    @Test
    public void testRmEmptyRootDirRecursive() throws Throwable {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        RestartFramework.at("before_delete_root_recursive").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(root);
        boolean deleted = getFileSystem().delete(root, true);
        LOG.info("rm -r / of empty dir result is {}", deleted);
        RestartFramework.at("after_delete_root_recursive").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(root);
    }

    @Test
    public void testRmEmptyRootDirNonRecursive() throws Throwable {
        RestartFramework.at("after_cleanup_before_delete").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        final Path root = new Path("/");
        assertIsDirectory(root);
        final FileSystem fs = getFileSystem();
        final AtomicInteger iterations = new AtomicInteger(0);
        final FileStatus[] originalChildren = listChildren(fs, root);
        LambdaTestUtils.eventually(OBJECTSTORE_RETRY_TIMEOUT, new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                FileStatus[] deleted = deleteChildren(fs, root, true);
                FileStatus[] children = listChildren(fs, root);
                if (children.length > 0) {
                    fail(String.format("After %d attempts: listing after rm /* not empty" + "\n%s\n%s\n%s", iterations.incrementAndGet(), dumpStats("final", children), dumpStats("deleted", deleted), dumpStats("original", originalChildren)));
                }
                return null;
            }
        }, new LambdaTestUtils.ProportionalRetryInterval(50, 1000));
        boolean deleted = fs.delete(root, false);
        LOG.info("rm / of empty dir result is {}", deleted);
        assertIsDirectory(root);
    }

    @Test
    public void testRmNonEmptyRootDirNonRecursive() throws Throwable {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        String touchfile = "/testRmNonEmptyRootDirNonRecursive";
        Path file = new Path(touchfile);
        ContractTestUtils.touch(getFileSystem(), file);
        RestartFramework.at("after_touch_file").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(root);
        try {
            RestartFramework.at("before_delete_nonempty_root").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            boolean deleted = getFileSystem().delete(root, false);
            fail("non recursive delete should have raised an exception," + " but completed with exit code " + deleted);
        } catch (IOException e) {
            handleExpectedException(e);
            assertIsFile(file);
        } finally {
            getFileSystem().delete(file, false);
        }
        assertIsDirectory(root);
    }

    @Test
    public void testRmRootRecursive() throws Throwable {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        assertIsDirectory(root);
        Path file = new Path("/testRmRootRecursive");
        try {
            ContractTestUtils.touch(getFileSystem(), file);
            RestartFramework.at("after_touch_file").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            RestartFramework.at("before_delete_root_with_file").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            boolean deleted = getFileSystem().delete(root, true);
            assertIsDirectory(root);
            LOG.info("rm -rf / result is {}", deleted);
            RestartFramework.at("after_delete_root_with_file").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            if (deleted) {
                assertPathDoesNotExist("expected file to be deleted", file);
            } else {
                assertPathExists("expected file to be preserved", file);
            }
        } finally {
            getFileSystem().delete(file, false);
        }
    }

    @Test
    public void testCreateFileOverRoot() throws Throwable {
        RestartFramework.at("before_create_file_over_root").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        byte[] dataset = dataset(1024, ' ', 'z');
        try {
            createFile(getFileSystem(), root, false, dataset);
            fail("expected an exception, got a file created over root: " + ls(root));
        } catch (IOException e) {
            handleExpectedException(e);
        }
        assertIsDirectory(root);
    }

    @Test
    public void testListEmptyRootDirectory() throws IOException {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        RestartFramework.at("after_cleanup_before_listing").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FileSystem fs = getFileSystem();
        Path root = new Path("/");
        FileStatus[] statuses = fs.listStatus(root);
        for (FileStatus status : statuses) {
            ContractTestUtils.assertDeleted(fs, status.getPath(), false, true, false);
        }
        FileStatus[] rootListStatus = fs.listStatus(root);
        assertEquals("listStatus on empty root-directory returned found: " + join("\n", rootListStatus), 0, rootListStatus.length);
        assertNoElements("listFiles(/, false)", fs.listFiles(root, false));
        assertNoElements("listFiles(/, true)", fs.listFiles(root, true));
        assertNoElements("listLocatedStatus(/)", fs.listLocatedStatus(root));
        assertIsDirectory(root);
    }

    @Test
    public void testSimpleRootListing() throws IOException {
        describe("test the nonrecursive root listing calls");
        FileSystem fs = getFileSystem();
        Path root = new Path("/");
        FileStatus[] statuses = fs.listStatus(root);
        String listStatusResult = join(statuses, "\n");
        List<LocatedFileStatus> locatedStatusList = toList(fs.listLocatedStatus(root));
        String locatedStatusResult = join(locatedStatusList, "\n");
        assertEquals("listStatus(/) vs listLocatedStatus(/) with \n" + "listStatus =" + listStatusResult + " listLocatedStatus = " + locatedStatusResult, statuses.length, locatedStatusList.size());
        RestartFramework.at("before_listing_operations").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        List<LocatedFileStatus> fileList = toList(fs.listFiles(root, false));
        String listFilesResult = join(fileList, "\n");
        assertTrue("listStatus(/) vs listFiles(/, false) with \n" + "listStatus = " + listStatusResult + "listFiles = " + listFilesResult, fileList.size() <= statuses.length);
        List<FileStatus> statusList = (List<FileStatus>) iteratorToList(fs.listStatusIterator(root));
        Assertions.assertThat(statusList).describedAs("Result of listStatus(/) and listStatusIterator(/)" + " must match").hasSameElementsAs(Arrays.stream(statuses).collect(Collectors.toList()));
    }

    @Test
    public void testRecursiveRootListing() throws IOException {
        describe("test a recursive root directory listing");
        FileSystem fs = getFileSystem();
        Path root = new Path("/");
        RestartFramework.at("before_recursive_listing").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        ContractTestUtils.TreeScanResults listing = new ContractTestUtils.TreeScanResults(fs.listFiles(root, true));
        describe("verifying consistency with treewalk's files");
        ContractTestUtils.TreeScanResults treeWalk = treeWalk(fs, root);
        treeWalk.assertFieldsEquivalent("treewalk vs listFiles(/, true)", listing, treeWalk.getFiles(), listing.getFiles());
    }
}

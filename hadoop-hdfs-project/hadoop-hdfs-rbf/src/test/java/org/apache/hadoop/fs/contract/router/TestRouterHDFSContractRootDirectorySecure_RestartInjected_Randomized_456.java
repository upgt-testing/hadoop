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
import org.apache.hadoop.fs.contract.AbstractContractRootDirectoryTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
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
import java.util.stream.Collectors;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.iteratorToList;
import static org.apache.hadoop.fs.contract.ContractTestUtils.toList;

/**
 * Test secure root dir operations on the Router-based FS.
 */
public class TestRouterHDFSContractRootDirectorySecure_RestartInjected_Randomized_456 extends AbstractContractRootDirectoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestRouterHDFSContractRootDirectorySecure_RestartInjected.class);

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
    public void testListEmptyRootDirectory() throws IOException {
        // It doesn't apply because we still have the mount points here
    }

    @Override
    public void testRmEmptyRootDirNonRecursive() throws IOException {
        // It doesn't apply because we still have the mount points here
    }

    @Override
    public void testRecursiveRootListing() throws IOException {
        // It doesn't apply because we still have the mount points here
    }

    @Override
    public void testRmRootRecursive() {
        // It doesn't apply because we still have the mount points here
    }

    @Override
    public void testRmEmptyRootDirRecursive() {
        // It doesn't apply because we still have the mount points here
    }

    @Test
    public void testMkDirDepth1() throws Throwable {
        FileSystem fs = getFileSystem();
        Path dir = new Path("/testmkdirdepth1");
        assertPathDoesNotExist("directory already exists", dir);
        fs.mkdirs(dir);
        RestartFramework.at("before_delete").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(dir);
        RestartFramework.at("after_mkdir").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertPathExists("directory already exists", dir);
        assertDeleted(dir, true);
    }

    @Test
    public void testRmNonEmptyRootDirNonRecursive() throws Throwable {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        String touchfile = "/testRmNonEmptyRootDirNonRecursive";
        Path file = new Path(touchfile);
        ContractTestUtils.touch(getFileSystem(), file);
        assertIsDirectory(root);
        try {
            RestartFramework.at("before_delete_nonempty_root").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            boolean deleted = getFileSystem().delete(root, false);
            fail("non recursive delete should have raised an exception," + " but completed with exit code " + deleted);
        } catch (IOException e) {
            handleExpectedException(e);
            assertIsFile(file);
        } finally {
            getFileSystem().delete(file, false);
        }
        RestartFramework.at("after_touch_file").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(root);
    }

    @Test
    public void testCreateFileOverRoot() throws Throwable {
        skipIfUnsupported(TEST_ROOT_TESTS_ENABLED);
        Path root = new Path("/");
        byte[] dataset = dataset(1024, ' ', 'z');
        try {
            createFile(getFileSystem(), root, false, dataset);
            fail("expected an exception, got a file created over root: " + ls(root));
        } catch (IOException e) {
            handleExpectedException(e);
        }
        RestartFramework.at("before_create_file_over_root").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertIsDirectory(root);
    }

    @Test
    public void testSimpleRootListing() throws IOException {
        describe("test the nonrecursive root listing calls");
        FileSystem fs = getFileSystem();
        Path root = new Path("/");
        RestartFramework.at("before_listing_operations").on(RouterHDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        FileStatus[] statuses = fs.listStatus(root);
        String listStatusResult = join(statuses, "\n");
        List<LocatedFileStatus> locatedStatusList = toList(fs.listLocatedStatus(root));
        String locatedStatusResult = join(locatedStatusList, "\n");
        assertEquals("listStatus(/) vs listLocatedStatus(/) with \n" + "listStatus =" + listStatusResult + " listLocatedStatus = " + locatedStatusResult, statuses.length, locatedStatusList.size());
        List<LocatedFileStatus> fileList = toList(fs.listFiles(root, false));
        String listFilesResult = join(fileList, "\n");
        assertTrue("listStatus(/) vs listFiles(/, false) with \n" + "listStatus = " + listStatusResult + "listFiles = " + listFilesResult, fileList.size() <= statuses.length);
        List<FileStatus> statusList = (List<FileStatus>) iteratorToList(fs.listStatusIterator(root));
        Assertions.assertThat(statusList).describedAs("Result of listStatus(/) and listStatusIterator(/)" + " must match").hasSameElementsAs(Arrays.stream(statuses).collect(Collectors.toList()));
    }
}

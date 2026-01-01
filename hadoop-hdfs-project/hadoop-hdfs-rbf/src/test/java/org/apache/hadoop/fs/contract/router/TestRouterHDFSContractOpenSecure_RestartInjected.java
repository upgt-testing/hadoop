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
import org.apache.hadoop.fs.contract.AbstractContractOpenTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import java.io.IOException;

/**
 * Test secure open operations on the Router-based FS.
 */
public class TestRouterHDFSContractOpenSecure_RestartInjected extends AbstractContractOpenTest {

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
  public void testOpenReadZeroByteFile() throws Throwable {
    RestartFramework.at("before_open_zero_byte")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenReadZeroByteFile();
  }

  @Test
  @Override
  public void testFsIsEncrypted() throws Exception {
    super.testFsIsEncrypted();
    RestartFramework.at("after_check_encryption")
        .on(RouterHDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  @Override
  public void testOpenReadDir() throws Throwable {
    super.testOpenReadDir();
    RestartFramework.at("after_open_dir_error")
        .on(RouterHDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  @Override
  public void testOpenReadDirWithChild() throws Throwable {
    super.testOpenReadDirWithChild();
    RestartFramework.at("after_open_dir_with_child_error")
        .on(RouterHDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  @Override
  public void testOpenFileTwice() throws Throwable {
    RestartFramework.at("before_open_file_twice")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenFileTwice();
    RestartFramework.at("after_open_file_twice")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  @Override
  public void testSequentialRead() throws Throwable {
    RestartFramework.at("before_sequential_read")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testSequentialRead();
  }

  @Test
  @Override
  public void testOpenFileReadZeroByte() throws Throwable {
    RestartFramework.at("before_openfile_zero_byte")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenFileReadZeroByte();
  }

  @Test
  @Override
  public void testOpenFileUnknownOption() throws Throwable {
    super.testOpenFileUnknownOption();
  }

  @Test
  @Override
  public void testOpenFileLazyFail() throws Throwable {
    super.testOpenFileLazyFail();
  }

  @Test
  @Override
  public void testOpenFileFailExceptionally() throws Throwable {
    super.testOpenFileFailExceptionally();
  }

  @Test
  @Override
  public void testAwaitFutureFailToFNFE() throws Throwable {
    super.testAwaitFutureFailToFNFE();
  }

  @Test
  @Override
  public void testAwaitFutureTimeoutFailToFNFE() throws Throwable {
    super.testAwaitFutureTimeoutFailToFNFE();
  }

  @Test
  @Override
  public void testOpenFileExceptionallyTranslating() throws Throwable {
    super.testOpenFileExceptionallyTranslating();
  }

  @Test
  @Override
  public void testChainedFailureAwaitFuture() throws Throwable {
    super.testChainedFailureAwaitFuture();
  }

  @Test
  @Override
  public void testOpenFileApplyRead() throws Throwable {
    RestartFramework.at("before_openfile_apply_read")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenFileApplyRead();
    RestartFramework.at("after_openfile_apply_read")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  @Override
  public void testOpenFileApplyAsyncRead() throws Throwable {
    RestartFramework.at("before_openfile_async_read")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenFileApplyAsyncRead();
  }

  @Test
  @Override
  public void testOpenFileNullStatusButFileLength() throws Throwable {
    RestartFramework.at("before_openfile_null_status")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    super.testOpenFileNullStatusButFileLength();
    RestartFramework.at("after_openfile_null_status")
        .on(RouterHDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }
}

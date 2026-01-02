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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractMkdirTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import java.io.IOException;

import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;

public class TestHDFSContractMkdir_RestartInjected extends AbstractContractMkdirTest {

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
  public void testMkdirRootDir() throws Throwable {
    describe("test mkdir on root directory with restart injection");
    FileSystem fs = getFileSystem();
    Path root = new Path("/");

    RestartFramework.at("before_mkdir_root")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    boolean made = fs.mkdirs(root);

    RestartFramework.at("after_mkdir_root")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertPathExists("root directory", root);
  }

  @Test
  public void testMkdirNonRecursive() throws Throwable {
    describe("test non-recursive mkdir with restart injection");
    FileSystem fs = getFileSystem();
    Path parent = path("testMkdirNonRecursive");
    Path nested = new Path(parent, "nested");

    RestartFramework.at("before_parent_mkdir")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    fs.mkdirs(parent);
    assertPathExists("parent directory not created", parent);

    RestartFramework.at("after_parent_mkdir")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    try {
      boolean made = fs.mkdirs(nested);
    } catch (IOException e) {
    }

    RestartFramework.at("after_nested_mkdir_attempt")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }

  @Test
  public void testMkdirOverFile() throws Throwable {
    describe("test mkdir over existing file with restart injection");
    FileSystem fs = getFileSystem();
    Path path = path("testMkdirOverFile");

    RestartFramework.at("before_file_creation")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    byte[] data = dataset(1024, 'a', 'z');
    createFile(fs, path, false, data);
    assertPathExists("file not created", path);

    RestartFramework.at("after_file_creation")
        .on(HDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    try {
      boolean made = fs.mkdirs(path);
    } catch (IOException e) {
    }

    RestartFramework.at("after_mkdir_over_file_attempt")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
  }
}

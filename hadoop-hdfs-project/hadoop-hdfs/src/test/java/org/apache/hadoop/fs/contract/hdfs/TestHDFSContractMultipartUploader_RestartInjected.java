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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.MultipartUploader;
import org.apache.hadoop.fs.MultipartUploaderFactory;
import org.apache.hadoop.fs.PartHandle;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.UploadHandle;
import org.apache.hadoop.fs.contract.AbstractContractMultipartUploaderTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;

public class TestHDFSContractMultipartUploader_RestartInjected extends
    AbstractContractMultipartUploaderTest {

  protected static final Logger LOG =
      LoggerFactory.getLogger(TestHDFSContractMultipartUploader_RestartInjected.class);

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

  @Override
  protected int partSizeInBytes() {
    return 1024;
  }

  @Override
  protected boolean finalizeConsumesUploadIdImmediately() {
    return true;
  }

  @Override
  protected boolean supportsConcurrentUploadsToSamePath() {
    return true;
  }

  @Test
  @Override
  public void testMultipartUpload() throws Exception {
    describe("test multipart upload with restart injection");
    FileSystem fs = getFileSystem();
    Path file = path("testMultipartUpload");
    MultipartUploader mpu = MultipartUploaderFactory.get(fs, null);

    RestartFramework.at("before_initiate_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    UploadHandle uploadHandle = mpu.initialize(file);

    RestartFramework.at("after_initiate_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    List<PartHandle> partHandles = new ArrayList<>();
    ByteBuffer data = ByteBuffer.wrap(
        generateTestData(partSizeInBytes(), 'a', 'z'));

    PartHandle partHandle1 = mpu.putPart(file, data, 1, uploadHandle, data.remaining());
    partHandles.add(partHandle1);

    RestartFramework.at("after_upload_first_part")
        .on(HDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    data.rewind();
    PartHandle partHandle2 = mpu.putPart(file, data, 2, uploadHandle, data.remaining());
    partHandles.add(partHandle2);

    RestartFramework.at("after_upload_second_part")
        .on(HDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    RestartFramework.at("before_complete_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    mpu.complete(file, partHandles, uploadHandle);

    RestartFramework.at("after_complete_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertPathExists("uploaded file", file);
  }

  @Test
  @Override
  public void testMultipartUploadAbort() throws Exception {
    describe("test multipart upload abort with restart injection");
    FileSystem fs = getFileSystem();
    Path file = path("testMultipartUploadAbort");
    MultipartUploader mpu = MultipartUploaderFactory.get(fs, null);

    RestartFramework.at("before_initiate_abort_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    UploadHandle uploadHandle = mpu.initialize(file);

    ByteBuffer data = ByteBuffer.wrap(
        generateTestData(partSizeInBytes(), 'a', 'z'));
    mpu.putPart(file, data, 1, uploadHandle, data.remaining());

    RestartFramework.at("after_upload_part_before_abort")
        .on(HDFSContract.getCluster())
        .restart("datanode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    mpu.abort(file, uploadHandle);

    RestartFramework.at("after_abort_upload")
        .on(HDFSContract.getCluster())
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    assertPathDoesNotExist("aborted file should not exist", file);
  }

  private byte[] generateTestData(int size, char start, char end) {
    byte[] data = new byte[size];
    int range = end - start + 1;
    for (int i = 0; i < size; i++) {
      data[i] = (byte) (start + (i % range));
    }
    return data;
  }
}

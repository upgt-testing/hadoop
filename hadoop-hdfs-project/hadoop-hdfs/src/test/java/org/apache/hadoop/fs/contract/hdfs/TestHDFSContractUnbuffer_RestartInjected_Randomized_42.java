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
import org.apache.hadoop.fs.contract.AbstractContractUnbufferTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;
import java.util.Arrays;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;

public class TestHDFSContractUnbuffer_RestartInjected_Randomized_42 extends AbstractContractUnbufferTest {

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

    private Path file;

    private byte[] fileBytes;

    @Override
    public void setup() throws Exception {
        super.setup();
        skipIfUnsupported(SUPPORTS_UNBUFFER);
        RestartFramework.at("after_file_creation_for_unbuffer").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        file = path("unbufferFile");
        fileBytes = dataset(TEST_FILE_LEN, 0, 255);
        createFile(getFileSystem(), file, true, fileBytes);
    }

    @Test
    public void testUnbufferAfterRead() throws IOException {
        describe("unbuffer a file after a single read");
        try (FSDataInputStream stream = getFileSystem().open(file)) {
            RestartFramework.at("after_open_before_read").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            validateFullFileContents(stream);
            RestartFramework.at("before_unbuffer_after_read").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            unbuffer(stream);
        }
    }

    @Test
    public void testUnbufferBeforeRead() throws IOException {
        describe("unbuffer a file before a read");
        try (FSDataInputStream stream = getFileSystem().open(file)) {
            RestartFramework.at("after_open_before_unbuffer").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            unbuffer(stream);
            RestartFramework.at("after_unbuffer_before_read").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            validateFullFileContents(stream);
        }
    }

    @Test
    public void testUnbufferEmptyFile() throws IOException {
        Path emptyFile = path("emptyUnbufferFile");
        getFileSystem().create(emptyFile, true).close();
        RestartFramework.at("after_empty_file_creation").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("unbuffer an empty file");
        try (FSDataInputStream stream = getFileSystem().open(emptyFile)) {
            RestartFramework.at("before_unbuffer_empty_file").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            unbuffer(stream);
        }
    }

    @Test
    public void testUnbufferOnClosedFile() throws IOException {
        describe("unbuffer a file before a read");
        FSDataInputStream stream = null;
        try {
            stream = getFileSystem().open(file);
            RestartFramework.at("after_open_before_validate").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            validateFullFileContents(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        RestartFramework.at("before_unbuffer_closed_file").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        if (stream != null) {
            unbuffer(stream);
        }
    }

    @Test
    public void testMultipleUnbuffers() throws IOException {
        describe("unbuffer a file multiple times");
        try (FSDataInputStream stream = getFileSystem().open(file)) {
            RestartFramework.at("after_open_before_first_unbuffer").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            unbuffer(stream);
            unbuffer(stream);
            RestartFramework.at("after_unbuffers_before_read").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            validateFullFileContents(stream);
            unbuffer(stream);
            unbuffer(stream);
        }
    }

    @Test
    public void testUnbufferMultipleReads() throws IOException {
        describe("unbuffer a file multiple times");
        try (FSDataInputStream stream = getFileSystem().open(file)) {
            RestartFramework.at("after_open_for_multiple_reads").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            unbuffer(stream);
            validateFileContents(stream, TEST_FILE_LEN / 8, 0);
            unbuffer(stream);
            RestartFramework.at("mid_multiple_reads").on(HDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
            validateFileContents(stream, TEST_FILE_LEN / 8, TEST_FILE_LEN / 8);
            validateFileContents(stream, TEST_FILE_LEN / 4, TEST_FILE_LEN / 4);
            unbuffer(stream);
            validateFileContents(stream, TEST_FILE_LEN / 2, TEST_FILE_LEN / 2);
            unbuffer(stream);
            assertEquals("stream should be at end of file", TEST_FILE_LEN, stream.getPos());
        }
    }

    private void unbuffer(FSDataInputStream stream) throws IOException {
        long pos = stream.getPos();
        stream.unbuffer();
        assertEquals("unbuffer unexpectedly changed the stream position", pos, stream.getPos());
    }

    protected void validateFullFileContents(FSDataInputStream stream) throws IOException {
        validateFileContents(stream, TEST_FILE_LEN, 0);
    }

    protected void validateFileContents(FSDataInputStream stream, int length, int startIndex) throws IOException {
        byte[] streamData = new byte[length];
        assertEquals("failed to read expected number of bytes from " + "stream. This may be transient", length, stream.read(streamData));
        byte[] validateFileBytes;
        if (startIndex == 0 && length == fileBytes.length) {
            validateFileBytes = fileBytes;
        } else {
            validateFileBytes = Arrays.copyOfRange(fileBytes, startIndex, startIndex + length);
        }
        assertArrayEquals("invalid file contents", validateFileBytes, streamData);
    }

    protected Path getFile() {
        return file;
    }
}

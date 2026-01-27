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
package org.apache.hadoop.fs.contract.router.web;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractContractSeekTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.IOException;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.skip;
import static org.apache.hadoop.fs.contract.ContractTestUtils.touch;

/**
 * Test seek operations on a Router WebHDFS FS.
 */
public class TestRouterWebHDFSContractSeek_RestartInjected_Randomized_123 extends AbstractContractSeekTest {

    private Path smallSeekFile;

    private Path zeroByteFile;

    private FSDataInputStream instream;

    @BeforeClass
    public static void createCluster() throws IOException {
        RouterWebHDFSContract.createCluster();
    }

    @AfterClass
    public static void teardownCluster() throws IOException {
        RouterWebHDFSContract.destroyCluster();
    }

    @Override
    protected AbstractFSContract createContract(Configuration conf) {
        return new RouterWebHDFSContract(conf);
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        skipIfUnsupported(SUPPORTS_SEEK);
        smallSeekFile = path("seekfile.txt");
        zeroByteFile = path("zero.txt");
        byte[] block = dataset(TEST_FILE_LEN, 0, 255);
        FileSystem fs = getFileSystem();
        createFile(fs, smallSeekFile, true, block);
        touch(fs, zeroByteFile);
    }

    @Override
    protected Configuration createConfiguration() {
        Configuration conf = super.createConfiguration();
        conf.setInt(CommonConfigurationKeysPublic.IO_FILE_BUFFER_SIZE_KEY, 4096);
        return conf;
    }

    @Override
    public void teardown() throws Exception {
        IOUtils.closeStream(instream);
        instream = null;
        super.teardown();
    }

    @Override
    public void testNegativeSeek() throws Throwable {
        System.out.println("Not supported");
    }

    @Override
    public void testSeekReadClosedFile() throws Throwable {
        System.out.println("Not supported");
    }

    @Override
    public void testSeekPastEndOfFileThenReseekAndRead() throws Throwable {
        System.out.println("Not supported");
    }

    @Test
    public void testSeekZeroByteFile() throws Throwable {
        describe("seek and read a 0 byte file");
        instream = getFileSystem().open(zeroByteFile);
        RestartFramework.at("after_open_zero_byte_file").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(0, instream.getPos());
        int result = instream.read();
        assertMinusOne("initial byte read", result);
        byte[] buffer = new byte[1];
        instream.seek(0);
        result = instream.read();
        assertMinusOne("post-seek byte read", result);
        result = instream.read(buffer, 0, 1);
        assertMinusOne("post-seek buffer read", result);
    }

    @Test
    public void testBlockReadZeroByteFile() throws Throwable {
        describe("do a block read on a 0 byte file");
        instream = getFileSystem().open(zeroByteFile);
        assertEquals(0, instream.getPos());
        byte[] buffer = new byte[1];
        int result = instream.read(buffer, 0, 1);
        RestartFramework.at("after_open_for_block_read").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertMinusOne("block read zero byte file", result);
    }

    @Test
    public void testSeekFile() throws Throwable {
        describe("basic seek operations");
        instream = getFileSystem().open(smallSeekFile);
        RestartFramework.at("after_seek_operations").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(0, instream.getPos());
        instream.seek(0);
        assertEquals(0, instream.getPos());
        instream.seek(1);
        RestartFramework.at("after_open_seek_file").on(RouterWebHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(1, instream.getPos());
    }
}

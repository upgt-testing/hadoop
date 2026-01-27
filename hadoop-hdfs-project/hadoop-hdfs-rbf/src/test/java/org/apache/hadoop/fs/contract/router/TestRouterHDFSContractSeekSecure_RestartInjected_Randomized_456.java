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
package org.apache.hadoop.fs.contract.router;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.contract.AbstractContractSeekTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.contract.ContractOptions;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;
import static org.apache.hadoop.fs.contract.ContractTestUtils.createFile;
import static org.apache.hadoop.fs.contract.ContractTestUtils.dataset;
import static org.apache.hadoop.fs.contract.ContractTestUtils.skip;
import static org.apache.hadoop.fs.contract.ContractTestUtils.touch;
import static org.apache.hadoop.fs.contract.ContractTestUtils.verifyRead;

public class TestRouterHDFSContractSeekSecure_RestartInjected_Randomized_456 extends AbstractContractSeekTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestRouterHDFSContractSeekSecure_RestartInjected.class);

    public static final int DEFAULT_RANDOM_SEEK_COUNT = 100;

    private Path smallSeekFile;

    private Path zeroByteFile;

    private FSDataInputStream instream;

    @BeforeClass
    public static void createCluster() throws IOException {
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

    protected void assumeSupportsPositionedReadable() throws IOException {
        if (!getContract().isSupported(SUPPORTS_POSITIONED_READABLE, true)) {
            skip("Skipping as unsupported feature: " + SUPPORTS_POSITIONED_READABLE);
        }
    }

    @Test
    public void testSeekZeroByteFile() throws Throwable {
        describe("seek and read a 0 byte file");
        instream = getFileSystem().open(zeroByteFile);
        assertEquals(0, instream.getPos());
        int result = instream.read();
        assertMinusOne("initial byte read", result);
        byte[] buffer = new byte[1];
        instream.seek(0);
        RestartFramework.at("after_open_zero_byte_file").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
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
        RestartFramework.at("after_open_for_block_read").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertMinusOne("block read zero byte file", result);
    }

    @Test
    public void testSeekReadClosedFile() throws Throwable {
        instream = getFileSystem().open(smallSeekFile);
        getLogger().debug("Stream is of type " + instream.getClass().getCanonicalName());
        instream.close();
        try {
            instream.seek(0);
            if (!isSupported(SUPPORTS_SEEK_ON_CLOSED_FILE)) {
                fail("seek succeeded on a closed stream");
            }
        } catch (IOException e) {
        }
        RestartFramework.at("after_open_before_close").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            int data = instream.available();
            if (!isSupported(SUPPORTS_AVAILABLE_ON_CLOSED_FILE)) {
                fail("available() succeeded on a closed stream, got " + data);
            }
        } catch (IOException e) {
        }
        try {
            int data = instream.read();
            fail("read() succeeded on a closed stream, got " + data);
        } catch (IOException e) {
        }
        try {
            byte[] buffer = new byte[1];
            int result = instream.read(buffer, 0, 1);
            fail("read(buffer, 0, 1) succeeded on a closed stream, got " + result);
        } catch (IOException e) {
        }
        try {
            long offset = instream.getPos();
        } catch (IOException e) {
        }
        instream.close();
    }

    @Test
    public void testNegativeSeek() throws Throwable {
        instream = getFileSystem().open(smallSeekFile);
        RestartFramework.at("after_open_for_negative_seek").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(0, instream.getPos());
        try {
            instream.seek(-1);
            long p = instream.getPos();
            LOG.warn("Seek to -1 returned a position of " + p);
            int result = instream.read();
            fail("expected an exception, got data " + result + " at a position of " + p);
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException e) {
            handleRelaxedException("a negative seek", "EOFException", e);
        }
        assertEquals(0, instream.getPos());
    }

    @Test
    public void testSeekFile() throws Throwable {
        describe("basic seek operations");
        instream = getFileSystem().open(smallSeekFile);
        assertEquals(0, instream.getPos());
        instream.seek(0);
        int result = instream.read();
        assertEquals(0, result);
        assertEquals(1, instream.read());
        assertEquals(2, instream.getPos());
        RestartFramework.at("mid_seek_operations").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(2, instream.read());
        RestartFramework.at("after_open_for_seek").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(3, instream.getPos());
        instream.seek(128);
        assertEquals(128, instream.getPos());
        assertEquals(128, instream.read());
        instream.seek(63);
        assertEquals(63, instream.read());
    }

    @Test
    public void testSeekAndReadPastEndOfFile() throws Throwable {
        describe("verify that reading past the last bytes in the file returns -1");
        instream = getFileSystem().open(smallSeekFile);
        RestartFramework.at("after_open_before_seek_to_end").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(0, instream.getPos());
        instream.seek(TEST_FILE_LEN - 2);
        assertTrue("Premature EOF", instream.read() != -1);
        assertTrue("Premature EOF", instream.read() != -1);
        assertMinusOne("read past end of file", instream.read());
    }

    @Test
    public void testSeekPastEndOfFileThenReseekAndRead() throws Throwable {
        RestartFramework.at("before_seek_past_eof").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("do a seek past the EOF, then verify the stream recovers");
        instream = getFileSystem().open(smallSeekFile);
        boolean canSeekPastEOF = !getContract().isSupported(ContractOptions.REJECTS_SEEK_PAST_EOF, true);
        try {
            instream.seek(TEST_FILE_LEN + 1);
            assertMinusOne("read after seeking past EOF", instream.read());
        } catch (EOFException e) {
            if (canSeekPastEOF) {
                throw e;
            }
            handleExpectedException(e);
        } catch (IOException e) {
            if (canSeekPastEOF) {
                throw e;
            }
            handleRelaxedException("a seek past the end of the file", "EOFException", e);
        }
        instream.seek(1);
        assertTrue("Premature EOF", instream.read() != -1);
    }

    @Test
    public void testSeekBigFile() throws Throwable {
        describe("Seek round a large file and verify the bytes are what is expected");
        Path testSeekFile = path("bigseekfile.txt");
        byte[] block = dataset(100 * 1024, 0, 255);
        createFile(getFileSystem(), testSeekFile, true, block);
        RestartFramework.at("mid_big_file_seeks").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        instream = getFileSystem().open(testSeekFile);
        assertEquals(0, instream.getPos());
        instream.seek(0);
        int result = instream.read();
        assertEquals(0, result);
        assertEquals(1, instream.read());
        assertEquals(2, instream.read());
        instream.seek(32768);
        assertEquals("@32768", block[32768], (byte) instream.read());
        instream.seek(40000);
        assertEquals("@40000", block[40000], (byte) instream.read());
        RestartFramework.at("after_big_file_creation").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        instream.seek(8191);
        assertEquals("@8191", block[8191], (byte) instream.read());
        instream.seek(0);
        assertEquals("@0", 0, (byte) instream.read());
        instream.seek(0);
        assertEquals(0, instream.getPos());
        instream.read();
        assertEquals(1, instream.getPos());
        byte[] buf = new byte[80 * 1024];
        instream.readFully(1, buf, 0, buf.length);
        assertEquals(1, instream.getPos());
    }

    @Test
    public void testPositionedBulkReadDoesntChangePosition() throws Throwable {
        describe("verify that a positioned read does not change the getPos() value");
        assumeSupportsPositionedReadable();
        Path testSeekFile = path("bigseekfile.txt");
        byte[] block = dataset(65536, 0, 255);
        createFile(getFileSystem(), testSeekFile, true, block);
        RestartFramework.at("after_file_creation_for_positioned_read").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        instream = getFileSystem().open(testSeekFile);
        instream.seek(39999);
        assertTrue(-1 != instream.read());
        assertEquals(40000, instream.getPos());
        int v = 256;
        byte[] readBuffer = new byte[v];
        instream.readFully(128, readBuffer, 0, v);
        assertEquals(40000, instream.getPos());
        assertEquals("@40000", block[40000], (byte) instream.read());
        for (int i = 0; i < 256; i++) {
            assertEquals("@" + i, block[i + 128], readBuffer[i]);
        }
    }

    @Test
    public void testRandomSeeks() throws Throwable {
        int limit = getContract().getLimit(TEST_RANDOM_SEEK_COUNT, DEFAULT_RANDOM_SEEK_COUNT);
        describe("Testing " + limit + " random seeks");
        int filesize = 10 * 1024;
        byte[] buf = dataset(filesize, 0, 255);
        Path randomSeekFile = path("testrandomseeks.bin");
        createFile(getFileSystem(), randomSeekFile, true, buf);
        Random r = new Random();
        int[] seeks = new int[10];
        RestartFramework.at("after_file_creation_for_random_seeks").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        int[] reads = new int[10];
        try (FSDataInputStream stm = getFileSystem().open(randomSeekFile)) {
            for (int i = 0; i < limit; i++) {
                int seekOff = r.nextInt(buf.length);
                int toRead = r.nextInt(Math.min(buf.length - seekOff, 32000));
                seeks[i % seeks.length] = seekOff;
                reads[i % reads.length] = toRead;
                verifyRead(stm, buf, seekOff, toRead);
                if (i == limit / 2) {
                    RestartFramework.at("mid_random_seeks").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
                }
            }
        } catch (AssertionError afe) {
            StringBuilder sb = new StringBuilder();
            sb.append("Sequence of actions:\n");
            for (int j = 0; j < seeks.length; j++) {
                sb.append("seek @ ").append(seeks[j]).append("  ").append("read ").append(reads[j]).append("\n");
            }
            LOG.error(sb.toString());
            throw afe;
        }
    }

    @Test
    public void testReadFullyZeroByteFile() throws Throwable {
        describe("readFully against a 0 byte file");
        assumeSupportsPositionedReadable();
        instream = getFileSystem().open(zeroByteFile);
        assertEquals(0, instream.getPos());
        byte[] buffer = new byte[1];
        instream.readFully(0, buffer, 0, 0);
        RestartFramework.at("after_open_zero_byte_for_readfully").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals(0, instream.getPos());
        instream.seek(0);
        assertEquals(0, instream.read(buffer, 0, 0));
    }

    @Test
    public void testReadFullyPastEOFZeroByteFile() throws Throwable {
        assumeSupportsPositionedReadable();
        describe("readFully past the EOF of a 0 byte file");
        RestartFramework.at("before_readfully_past_eof_zero").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        instream = getFileSystem().open(zeroByteFile);
        byte[] buffer = new byte[1];
        try {
            instream.readFully(0, buffer, 0, 16);
            fail("Expected an exception");
        } catch (IllegalArgumentException | IndexOutOfBoundsException | EOFException e) {
        }
    }

    @Test
    public void testReadFullySmallFile() throws Throwable {
        RestartFramework.at("after_open_for_readfully").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("readFully operations");
        assumeSupportsPositionedReadable();
        instream = getFileSystem().open(smallSeekFile);
        byte[] buffer = new byte[256];
        try {
            instream.readFully(0, buffer, 0, -16);
            fail("Expected an exception");
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        try {
            instream.readFully(0, buffer, -1, 16);
            fail("Expected an exception");
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        try {
            instream.readFully(-1, buffer);
            fail("Expected an exception");
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            handleRelaxedException("readFully with a negative position ", "EOFException", e);
        }
        try {
            instream.readFully(0, buffer, buffer.length - 8, 16);
            fail("Expected an exception");
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        assertEquals(0, instream.getPos());
        instream.readFully(0, buffer);
        assertEquals(0, instream.getPos());
        byte[] fullFile = new byte[TEST_FILE_LEN];
        instream.readFully(0, fullFile);
        assertEquals(0, instream.getPos());
        try {
            instream.readFully(16, fullFile);
            fail("Expected an exception");
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException e) {
            handleRelaxedException("readFully which reads past EOF ", "EOFException", e);
        }
    }

    @Test
    public void testReadFullyPastEOF() throws Throwable {
        describe("readFully past the EOF of a file");
        assumeSupportsPositionedReadable();
        instream = getFileSystem().open(smallSeekFile);
        byte[] buffer = new byte[256];
        RestartFramework.at("before_readfully_past_eof").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            instream.readFully(TEST_FILE_LEN + 1, buffer);
            fail("Expected an exception");
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException e) {
            handleRelaxedException("readFully with an offset past EOF ", "EOFException", e);
        }
        try {
            instream.readFully(TEST_FILE_LEN + 1, buffer, 0, 0);
            LOG.info("Filesystem short-circuits 0-byte reads");
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException e) {
            handleRelaxedException("readFully(0 bytes) with an offset past EOF ", "EOFException", e);
        }
    }

    @Test
    public void testReadFullyZeroBytebufferPastEOF() throws Throwable {
        RestartFramework.at("before_zero_byte_readfully_past_eof").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("readFully zero bytes from an offset past EOF");
        assumeSupportsPositionedReadable();
        instream = getFileSystem().open(smallSeekFile);
        byte[] buffer = new byte[256];
        try {
            instream.readFully(TEST_FILE_LEN + 1, buffer, 0, 0);
            LOG.info("Filesystem short-circuits 0-byte reads");
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException e) {
            handleRelaxedException("readFully(0 bytes) with an offset past EOF ", "EOFException", e);
        }
    }

    @Test
    public void testReadNullBuffer() throws Throwable {
        RestartFramework.at("before_null_buffer_read").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("try to read a null buffer ");
        assumeSupportsPositionedReadable();
        try (FSDataInputStream in = getFileSystem().open(smallSeekFile)) {
            int r = in.read(0, null, 0, 16);
            fail("Expected an exception from a read into a null buffer, got " + r);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testReadSmallFile() throws Throwable {
        describe("PositionedRead.read operations");
        assumeSupportsPositionedReadable();
        instream = getFileSystem().open(smallSeekFile);
        byte[] buffer = new byte[256];
        int r;
        try {
            r = instream.read(0, buffer, 0, -16);
            fail("Expected an exception, got " + r);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        try {
            r = instream.read(0, buffer, -1, 16);
            fail("Expected an exception, got " + r);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        try {
            r = instream.read(-1, buffer, 0, 16);
            fail("Expected an exception, got " + r);
        } catch (EOFException e) {
            handleExpectedException(e);
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            handleRelaxedException("read() with a negative position ", "EOFException", e);
        }
        RestartFramework.at("after_open_for_positioned_read").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            r = instream.read(0, buffer, buffer.length - 8, 16);
            fail("Expected an exception, got " + r);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
        }
        assertEquals(0, instream.getPos());
        instream.readFully(0, buffer);
        assertEquals(0, instream.getPos());
        byte[] fullFile = new byte[TEST_FILE_LEN];
        instream.readFully(0, fullFile, 0, fullFile.length);
        assertEquals(0, instream.getPos());
        assertEquals(-1, instream.read(TEST_FILE_LEN + 16, buffer, 0, 1));
    }

    @Test
    public void testReadAtExactEOF() throws Throwable {
        RestartFramework.at("after_read_at_eof").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        describe("read at the end of the file");
        instream = getFileSystem().open(smallSeekFile);
        instream.seek(TEST_FILE_LEN - 1);
        assertTrue("read at last byte", instream.read() > 0);
        RestartFramework.at("before_seek_to_exact_eof").on(RouterHDFSContract.getCluster()).restart("datanode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        assertEquals("read just past EOF", -1, instream.read());
    }
}

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
import org.apache.hadoop.fs.contract.AbstractContractSetTimesTest;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restarttest.api.RestartFramework;
import org.restarttest.core.RestartMode;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestHDFSContractSetTimes_RestartInjected_Randomized_42 extends AbstractContractSetTimesTest {

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
    public void testSetTimesNonexistentFile() throws Throwable {
        Path target = new Path(path("test"), "target");
        RestartFramework.at("before_settimes_on_nonexistent").on(HDFSContract.getCluster()).restart("namenode").withIndex(0).withMode(RestartMode.GRACEFUL).execute();
        try {
            long time = System.currentTimeMillis();
            getFileSystem().setTimes(target, time, time);
            fail("expected a failure");
        } catch (FileNotFoundException e) {
            handleExpectedException(e);
        }
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppend}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class contains selective transformations of test methods from the
 * original TestFileAppend class.
 *
 * @see TestFileAppend Original test using MiniDFSCluster
 */
public class TestFileAppend_ProcessBased {

  private static byte[] fileContents = null;

  /**
   * Verify that the data written to the full blocks are sane.
   * This helper method uses only FileSystem APIs and works with ProcessBasedMiniDFSCluster.
   */
  private void checkFile(DistributedFileSystem fileSys, Path name, int repl)
      throws IOException {
    boolean done = false;

    // wait till all full blocks are confirmed by the datanodes.
    while (!done) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {;}
      done = true;
      BlockLocation[] locations = fileSys.getFileBlockLocations(
          fileSys.getFileStatus(name), 0, AppendTestUtil.FILE_SIZE);
      if (locations.length < AppendTestUtil.NUM_BLOCKS) {
        System.out.println("Number of blocks found " + locations.length);
        done = false;
        continue;
      }
      for (int idx = 0; idx < AppendTestUtil.NUM_BLOCKS; idx++) {
        if (locations[idx].getHosts().length < repl) {
          System.out.println("Block index " + idx + " not yet replicated.");
          done = false;
          break;
        }
      }
    }
    byte[] expected =
        new byte[AppendTestUtil.NUM_BLOCKS * AppendTestUtil.BLOCK_SIZE];
    System.arraycopy(fileContents, 0, expected, 0, expected.length);
    // do a sanity check. Read the file
    // do not check file status since the file is not yet closed.
    AppendTestUtil.checkFullFile(fileSys, name,
        AppendTestUtil.NUM_BLOCKS * AppendTestUtil.BLOCK_SIZE,
        expected, "Read 1", false);
  }

  /**
   * Test a simple flush on a simple HDFS file.
   * @throws Exception an exception might be thrown
   */
  @Test
  public void testSimpleFlush() throws Exception {
    Configuration conf = new HdfsConfiguration();
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      cluster.waitClusterUp();

      // create a new file.
      Path file1 = new Path("/simpleFlush.dat");
      FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
      System.out.println("Created file simpleFlush.dat");

      // write to file
      int mid = AppendTestUtil.FILE_SIZE /2;
      stm.write(fileContents, 0, mid);
      stm.hflush();
      System.out.println("Wrote and Flushed first part of file.");

      // write the remainder of the file
      stm.write(fileContents, mid, AppendTestUtil.FILE_SIZE - mid);
      System.out.println("Written second part of file");
      stm.hflush();
      stm.hflush();
      System.out.println("Wrote and Flushed second part of file.");

      // verify that full blocks are sane
      checkFile(fs, file1, 1);

      stm.close();
      System.out.println("Closed file.");

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
          fileContents, "Read 2");

    } catch (IOException e) {
      System.out.println("Exception :" + e);
      throw e;
    } catch (Throwable e) {
      System.out.println("Throwable :" + e);
      e.printStackTrace();
      throw new IOException("Throwable : " + e);
    } finally {
      fs.close();
      cluster.shutdown();
    }
  }

  /**
   * Test that file data can be flushed.
   * @throws Exception an exception might be thrown
   */
  @Test
  public void testComplexFlush() throws Exception {
    Configuration conf = new HdfsConfiguration();
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .format(true)
        .build();
    DistributedFileSystem fs = cluster.getFileSystem();
    try {
      cluster.waitClusterUp();

      // create a new file.
      Path file1 = new Path("/complexFlush.dat");
      FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
      System.out.println("Created file complexFlush.dat");

      int start = 0;
      for (start = 0; (start + 29) < AppendTestUtil.FILE_SIZE; ) {
        stm.write(fileContents, start, 29);
        stm.hflush();
        start += 29;
      }
      stm.write(fileContents, start, AppendTestUtil.FILE_SIZE -start);
      // need to make sure we completely write out all full blocks before
      // the checkFile() call (see FSOutputSummer#flush)
      stm.flush();
      // verify that full blocks are sane
      checkFile(fs, file1, 1);
      stm.close();

      // verify that entire file is good
      AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
          fileContents, "Read 2");
    } catch (IOException e) {
      System.out.println("Exception :" + e);
      throw e;
    } catch (Throwable e) {
      System.out.println("Throwable :" + e);
      e.printStackTrace();
      throw new IOException("Throwable : " + e);
    } finally {
      fs.close();
      cluster.shutdown();
    }
  }
}

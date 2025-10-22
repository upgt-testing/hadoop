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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestFileAppend2}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class contains selective transformations of test methods from the
 * original TestFileAppend2 class.
 *
 * @see TestFileAppend2 Original test using MiniDFSCluster
 */
public class TestFileAppend2_ProcessBased {

  private byte[] fileContents = null;

  /**
   * Creates one file, writes a few bytes to it and then closed it.
   * Reopens the same file for appending, write all blocks and then close.
   * Verify that all data exists in file.
   * @throws Exception an exception might be thrown
   */
  @Test
  public void testSimpleAppend() throws Exception {
    final Configuration conf = new HdfsConfiguration();
    conf.setInt(DFSConfigKeys.DFS_DATANODE_HANDLER_COUNT_KEY, 50);
    fileContents = AppendTestUtil.initBuffer(AppendTestUtil.FILE_SIZE);

    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(1)
        .allNodesHadoopDistribution(hadoopHome)
        .format(true)
        .build();
    FileSystem fs = cluster.getFileSystem();
    try {
      cluster.waitClusterUp();

      { // test appending to a file.

        // create a new file.
        Path file1 = new Path("/simpleAppend.dat");
        FSDataOutputStream stm = AppendTestUtil.createFile(fs, file1, 1);
        System.out.println("Created file simpleAppend.dat");

        // write to file
        int mid = 186;   // io.bytes.per.checksum bytes
        System.out.println("Writing " + mid + " bytes to file " + file1);
        stm.write(fileContents, 0, mid);
        stm.close();
        System.out.println("Wrote and Closed first part of file.");

        // write to file
        int mid2 = 607;   // io.bytes.per.checksum bytes
        System.out.println("Writing " + mid + " bytes to file " + file1);
        stm = fs.append(file1);
        stm.write(fileContents, mid, mid2-mid);
        stm.close();
        System.out.println("Wrote and Closed second part of file.");

        // write the remainder of the file
        stm = fs.append(file1);

        // ensure getPos is set to reflect existing size of the file
        assertTrue(stm.getPos() > 0);

        System.out.println("Writing " + (AppendTestUtil.FILE_SIZE - mid2) +
            " bytes to file " + file1);
        stm.write(fileContents, mid2, AppendTestUtil.FILE_SIZE - mid2);
        System.out.println("Written second part of file");
        stm.close();
        System.out.println("Wrote and Closed second part of file.");

        // verify that entire file is good
        AppendTestUtil.checkFullFile(fs, file1, AppendTestUtil.FILE_SIZE,
            fileContents, "Read 2");
      }

      { // test appending to an non-existing file.
        FSDataOutputStream out = null;
        try {
          out = fs.append(new Path("/non-existing.dat"));
          fail("Expected to have FileNotFoundException");
        }
        catch(java.io.FileNotFoundException fnfe) {
          System.out.println("Good: got " + fnfe);
          fnfe.printStackTrace(System.out);
        }
        finally {
          IOUtils.closeStream(out);
        }
      }

      { // test append permission.

        //set root to all writable
        Path root = new Path("/");
        fs.setPermission(root, new FsPermission((short)0777));
        fs.close();

        // login as a different user
        final UserGroupInformation superuser =
          UserGroupInformation.getCurrentUser();
        String username = "testappenduser";
        String group = "testappendgroup";
        assertFalse(superuser.getShortUserName().equals(username));
        assertFalse(Arrays.asList(superuser.getGroupNames()).contains(group));
        UserGroupInformation appenduser =
          UserGroupInformation.createUserForTesting(username, new String[]{group});

        fs = DFSTestUtil.getFileSystemAs(appenduser, conf);

        // create a file
        Path dir = new Path(root, getClass().getSimpleName());
        Path foo = new Path(dir, "foo.dat");
        FSDataOutputStream out = null;
        int offset = 0;
        try {
          out = fs.create(foo);
          int len = 10 + AppendTestUtil.nextInt(100);
          out.write(fileContents, offset, len);
          offset += len;
        }
        finally {
          IOUtils.closeStream(out);
        }

        // change dir and foo to minimal permissions.
        fs.setPermission(dir, new FsPermission((short)0100));
        fs.setPermission(foo, new FsPermission((short)0200));

        // try append, should success
        out = null;
        try {
          out = fs.append(foo);
          int len = 10 + AppendTestUtil.nextInt(100);
          out.write(fileContents, offset, len);
          offset += len;
        }
        finally {
          IOUtils.closeStream(out);
        }

        // change dir and foo to all but no write on foo.
        fs.setPermission(foo, new FsPermission((short)0577));
        fs.setPermission(dir, new FsPermission((short)0777));

        // try append, should fail
        out = null;
        try {
          out = fs.append(foo);
          fail("Expected to have AccessControlException");
        }
        catch(AccessControlException ace) {
          System.out.println("Good: got " + ace);
          ace.printStackTrace(System.out);
        }
        finally {
          IOUtils.closeStream(out);
        }
      }
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

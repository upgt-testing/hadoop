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
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestSnapshotFileLength}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * Only testSnapshotfileLength is transformed.
 *
 * @see TestSnapshotFileLength Original test using MiniDFSCluster
 */
public class TestSnapshotFileLength_ProcessBased {

  private static final long SEED = 0;
  private static final short REPLICATION = 1;
  private static final int BLOCKSIZE = 1024;

  /**
   * Test that we cannot read a file beyond its snapshot length
   * when accessing it via a snapshot path.
   */
  @Test(timeout=300000)
  public void testSnapshotfileLength() throws Exception {
    String hadoopHome = System.getenv("HADOOP_HOME");
    assumeNotNull("HADOOP_HOME must be set for ProcessBasedMiniDFSCluster", hadoopHome);

    final Configuration conf = new Configuration();
    conf.setLong(DFSConfigKeys.DFS_NAMENODE_MIN_BLOCK_SIZE_KEY, BLOCKSIZE);
    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, BLOCKSIZE);

    final Path dir = new Path("/TestSnapshotFileLength");
    final Path sub = new Path(dir, "sub1");
    final String file1Name = "file1";
    final String snapshot1 = "snapshot1";

    ProcessBasedMiniDFSCluster cluster = null;
    try {
      cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
          .numDataNodes(REPLICATION)
          .allNodesHadoopDistribution(hadoopHome)
          .format(true)
          .build();
      cluster.waitClusterUp();

      final DistributedFileSystem hdfs = cluster.getFileSystem();
      hdfs.mkdirs(sub);

      int bytesRead;
      byte[] buffer = new byte[BLOCKSIZE * 8];
      int origLen = BLOCKSIZE + 1;
      int toAppend = BLOCKSIZE;
      FSDataInputStream fis = null;
      FileStatus fileStatus = null;

      // Create and write a file.
      Path file1 = new Path(sub, file1Name);
      DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, 0, BLOCKSIZE, REPLICATION, SEED);
      DFSTestUtil.appendFile(hdfs, file1, origLen);

      // Create a snapshot on the parent directory.
      hdfs.allowSnapshot(sub);
      hdfs.createSnapshot(sub, snapshot1);

      Path file1snap1
          = SnapshotTestHelper.getSnapshotPath(sub, snapshot1, file1Name);

      final FileChecksum snapChksum1 = hdfs.getFileChecksum(file1snap1);
      assertThat("file and snapshot file checksums are not equal",
          hdfs.getFileChecksum(file1), is(snapChksum1));

      // Append to the file.
      FSDataOutputStream out = hdfs.append(file1);
      // Nothing has been appended yet. All checksums should still be equal.
      // HDFS-8150:Fetching checksum for file under construction should fail
      try {
        hdfs.getFileChecksum(file1);
        fail("getFileChecksum should fail for files "
            + "with blocks under construction");
      } catch (IOException ie) {
        assertTrue(ie.getMessage().contains(
            "Fail to get checksum, since file " + file1
                + " is under construction."));
      }
      assertThat("snapshot checksum (post-open for append) has changed",
          hdfs.getFileChecksum(file1snap1), is(snapChksum1));
      try {
        AppendTestUtil.write(out, 0, toAppend);
        out.hflush();
        // Test reading from snapshot of file that is open for append
        byte[] dataFromSnapshot = DFSTestUtil.readFileBuffer(hdfs, file1snap1);
        assertThat("Wrong data size in snapshot.",
            dataFromSnapshot.length, is(origLen));
        // Verify that checksum didn't change
        assertThat("snapshot checksum (post-append) has changed",
            hdfs.getFileChecksum(file1snap1), is(snapChksum1));
      } finally {
        out.close();
      }
      assertThat("file and snapshot file checksums (post-close) are equal",
          hdfs.getFileChecksum(file1), not(snapChksum1));
      assertThat("snapshot file checksum (post-close) has changed",
          hdfs.getFileChecksum(file1snap1), is(snapChksum1));

      // Make sure we can read the entire file via its non-snapshot path.
      fileStatus = hdfs.getFileStatus(file1);
      assertThat(fileStatus.getLen(), is((long) origLen + toAppend));
      fis = hdfs.open(file1);
      bytesRead = fis.read(0, buffer, 0, buffer.length);
      assertThat(bytesRead, is(origLen + toAppend));
      fis.close();

      // Try to open the file via its snapshot path.
      fis = hdfs.open(file1snap1);
      fileStatus = hdfs.getFileStatus(file1snap1);
      assertThat(fileStatus.getLen(), is((long) origLen));

      // Make sure we can only read up to the snapshot length.
      bytesRead = fis.read(0, buffer, 0, buffer.length);
      assertThat(bytesRead, is(origLen));
      fis.close();

      byte[] dataFromSnapshot = DFSTestUtil.readFileBuffer(hdfs,
          file1snap1);
      assertThat("Wrong data size in snapshot.",
          dataFromSnapshot.length, is(origLen));
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }
}

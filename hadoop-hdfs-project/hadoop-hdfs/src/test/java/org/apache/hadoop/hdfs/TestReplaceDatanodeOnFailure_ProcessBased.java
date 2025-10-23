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

import java.util.function.Supplier;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream;
import org.apache.hadoop.hdfs.protocol.datatransfer.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.datatransfer.ReplaceDatanodeOnFailure;
import org.apache.hadoop.hdfs.protocol.datatransfer.ReplaceDatanodeOnFailure.Policy;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

/**
 * Process-based version of TestReplaceDatanodeOnFailure.
 * Tests that data nodes are correctly replaced on failure using ProcessBasedMiniDFSCluster.
 */
public class TestReplaceDatanodeOnFailure_ProcessBased {
  static final Logger LOG =
      LoggerFactory.getLogger(TestReplaceDatanodeOnFailure_ProcessBased.class);

  static final String DIR = "/" + TestReplaceDatanodeOnFailure_ProcessBased.class.getSimpleName() + "/";
  static final short REPLICATION = 3;
  final private static String RACK0 = "/rack0";
  final private static String RACK1 = "/rack1";

  {
    GenericTestUtils.setLogLevel(DataTransferProtocol.LOG, Level.ALL);
  }

  /**
   * Test replace datanode on failure with ProcessBasedMiniDFSCluster.
   * This test verifies that when a DataNode fails during write operations,
   * it is correctly replaced with another DataNode from a different rack.
   */
  @Test
  public void testReplaceDatanodeOnFailure() throws Exception {
    final Configuration conf = new HdfsConfiguration();
    // do not consider load factor when selecting a data node
    conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY,
        false);
    //always replace a datanode
    ReplaceDatanodeOnFailure.write(Policy.ALWAYS, true, conf);

    // Specify Hadoop distribution for all nodes
    String hadoopHome = System.getenv("HADOOP_HOME");
    if (hadoopHome == null) {
      hadoopHome = System.getProperty("hadoop.home.dir");
    }
    if (hadoopHome == null) {
      throw new RuntimeException("HADOOP_HOME not set. Please set HADOOP_HOME environment variable.");
    }

    final String[] racks = new String[REPLICATION];
    Arrays.fill(racks, RACK0);
    final ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .racks(racks)
        .numDataNodes(REPLICATION)
        .format(true)
        .build();

    try {
      cluster.waitClusterUp();
      final DistributedFileSystem fs = cluster.getFileSystem();
      final Path dir = new Path(DIR);
      final int NUM_WRITERS = 10;
      final int FIRST_BATCH = 5;
      final SlowWriter[] slowwriters = new SlowWriter[NUM_WRITERS];
      for(int i = 1; i <= slowwriters.length; i++) {
        //create slow writers in different speed
        slowwriters[i - 1] = new SlowWriter(fs, new Path(dir, "file" + i), i*200L);
      }

      for(int i = 0; i < FIRST_BATCH; i++) {
        slowwriters[i].start();
      }

      // Let slow writers write something.
      // Some of them are too slow and will be not yet started.
      sleepSeconds(3);

      //start new datanodes
      cluster.startDataNodes(conf, 2, true, new String[]{RACK1, RACK1}, null, null);
      cluster.waitClusterUp();
      // Note: ProcessBasedMiniDFSCluster doesn't have waitFirstBRCompleted,
      // but waitClusterUp() ensures DataNodes are registered with NameNode

      //stop an old datanode
      cluster.shutdownDataNode(AppendTestUtil.nextInt(REPLICATION));

      for(int i = FIRST_BATCH; i < slowwriters.length; i++) {
        slowwriters[i].start();
      }

      waitForBlockReplication(slowwriters);

      //check replication and interrupt.
      for(SlowWriter s : slowwriters) {
        s.checkReplication();
        s.interruptRunning();
      }

      //close files
      for(SlowWriter s : slowwriters) {
        s.joinAndClose();
      }

      //Verify the file
      LOG.info("Verify the file");
      for(int i = 0; i < slowwriters.length; i++) {
        LOG.info(slowwriters[i].filepath + ": length="
            + fs.getFileStatus(slowwriters[i].filepath).getLen());
        FSDataInputStream in = null;
        try {
          in = fs.open(slowwriters[i].filepath);
          for(int j = 0, x; (x = in.read()) != -1; j++) {
            Assert.assertEquals(j, x);
          }
        }
        finally {
          IOUtils.closeStream(in);
        }
      }
    } finally {
      if (cluster != null) {cluster.shutdown();}
    }
  }

  void waitForBlockReplication(final SlowWriter[] slowwriters) throws
      TimeoutException, InterruptedException {
    GenericTestUtils.waitFor(new Supplier<Boolean>() {
      @Override public Boolean get() {
        try {
          for (SlowWriter s : slowwriters) {
            if (s.out.getCurrentBlockReplication() < REPLICATION) {
              return false;
            }
          }
        } catch (IOException e) {
          LOG.warn("IOException is thrown while getting the file block " +
              "replication factor", e);
          return false;
        }
        return true;
      }
    }, 1000, 10000);
  }

  static void sleepSeconds(final int waittime) throws InterruptedException {
    LOG.info("Wait " + waittime + " seconds");
    Thread.sleep(waittime * 1000L);
  }

  static class SlowWriter extends Thread {
    final Path filepath;
    final HdfsDataOutputStream out;
    final long sleepms;
    private volatile boolean running = true;

    SlowWriter(DistributedFileSystem fs, Path filepath, final long sleepms
        ) throws IOException {
      super(SlowWriter.class.getSimpleName() + ":" + filepath);
      this.filepath = filepath;
      this.out = (HdfsDataOutputStream)fs.create(filepath, REPLICATION);
      this.sleepms = sleepms;
    }

    @Override
    public void run() {
      int i = 0;

      try {
        sleep(sleepms);
        for(; running; i++) {
          LOG.info(getName() + " writes " + i);
          out.write(i);
          out.hflush();

          sleep(sleepms);
        }
      } catch(InterruptedException e) {
        LOG.info(getName() + " interrupted:" + e);
      } catch(IOException e) {
        throw new RuntimeException(getName(), e);
      } finally {
        LOG.info(getName() + " terminated: i=" + i);
      }
    }

    void interruptRunning() {
      running = false;
      interrupt();
    }

    void joinAndClose() throws InterruptedException {
      LOG.info(getName() + " join and close");
      join();
      IOUtils.closeStream(out);
    }

    void checkReplication() throws IOException {
      Assert.assertEquals(REPLICATION, out.getCurrentBlockReplication());
    }
  }
}

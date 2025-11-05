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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.metrics2.util.Quantile;
import org.apache.hadoop.metrics2.util.SampleQuantiles;
import org.apache.hadoop.util.StopWatch;
import org.junit.Test;

/**
 * ProcessBasedMiniDFSCluster version of {@link TestMultiThreadedHflush}.
 *
 * Transformed from MiniDFSCluster to ProcessBasedMiniDFSCluster to enable
 * process-based testing and multi-version upgrade scenarios.
 *
 * This class tests hflushing concurrently from many threads.
 *
 * @see TestMultiThreadedHflush Original test using MiniDFSCluster
 */
public class TestMultiThreadedHflush_ProcessBased {
  static final int blockSize = 1024*1024;

  private static final int NUM_THREADS = 10;
  private static final int WRITE_SIZE = 517;
  private static final int NUM_WRITES_PER_THREAD = 1000;

  private byte[] toWrite = null;

  private final SampleQuantiles quantiles = new SampleQuantiles(
      new Quantile[] {
        new Quantile(0.50, 0.050),
        new Quantile(0.75, 0.025), new Quantile(0.90, 0.010),
        new Quantile(0.95, 0.005), new Quantile(0.99, 0.001) });

  /*
   * creates a file but does not close it
   */
  private FSDataOutputStream createFile(FileSystem fileSys, Path name, int repl)
    throws IOException {
    FSDataOutputStream stm = fileSys.create(name, true, fileSys.getConf()
        .getInt(CommonConfigurationKeys.IO_FILE_BUFFER_SIZE_KEY, 4096),
        (short) repl, blockSize);
    return stm;
  }

  private void initBuffer(int size) {
    long seed = AppendTestUtil.nextLong();
    toWrite = AppendTestUtil.randomBytes(seed, size);
  }

  private class WriterThread extends Thread {
    private final FSDataOutputStream stm;
    private final AtomicReference<Throwable> thrown;
    private final int numWrites;
    private final CountDownLatch countdown;

    public WriterThread(FSDataOutputStream stm,
      AtomicReference<Throwable> thrown,
      CountDownLatch countdown, int numWrites) {
      this.stm = stm;
      this.thrown = thrown;
      this.numWrites = numWrites;
      this.countdown = countdown;
    }

    @Override
    public void run() {
      try {
        countdown.await();
        for (int i = 0; i < numWrites && thrown.get() == null; i++) {
          doAWrite();
        }
      } catch (Throwable t) {
        thrown.compareAndSet(null, t);
      }
    }

    private void doAWrite() throws IOException {
      StopWatch sw = new StopWatch().start();
      stm.write(toWrite);
      stm.hflush();
      long micros = sw.now(TimeUnit.MICROSECONDS);
      quantiles.insert(micros);
    }
  }


  /**
   * Test case where a bunch of threads are both appending and flushing.
   * They all finish before the file is closed.
   */
  @Test
  public void testMultipleHflushersRepl1() throws Exception {
    doTestMultipleHflushers(1);
  }

  @Test
  public void testMultipleHflushersRepl3() throws Exception {
    doTestMultipleHflushers(3);
  }

  private void doTestMultipleHflushers(int repl) throws Exception {
    Configuration conf = new Configuration();
    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(repl)
        .build();
    cluster.waitClusterUp();

    FileSystem fs = cluster.getFileSystem();
    Path p = new Path("/multiple-hflushers.dat");
    try {
      doMultithreadedWrites(conf, p, NUM_THREADS, WRITE_SIZE,
          NUM_WRITES_PER_THREAD, repl);
      System.out.println("Latency quantiles (in microseconds):\n" +
          quantiles);
    } finally {
      fs.close();
      cluster.shutdown();
    }
  }

  /**
   * Test case where a bunch of threads are continuously calling hflush() while another
   * thread appends some data and then closes the file.
   *
   * The hflushing threads should eventually catch an IOException stating that the stream
   * was closed -- and not an NPE or anything like that.
   */
  @Test
  public void testHflushWhileClosing() throws Throwable {
    Configuration conf = new Configuration();
    ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf).build();
    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();
    Path p = new Path("/hflush-and-close.dat");

    final FSDataOutputStream stm = createFile(fs, p, 1);


    ArrayList<Thread> flushers = new ArrayList<Thread>();
    final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>();
    try {
      for (int i = 0; i < 10; i++) {
        Thread flusher = new Thread() {
            @Override
            public void run() {
              try {
                while (true) {
                  try {
                    stm.hflush();
                  } catch (ClosedChannelException ioe) {
                    // Expected exception caught. Ignoring.
                    return;
                  }
                }
              } catch (Throwable t) {
                thrown.set(t);
              }
            }
          };
        flusher.start();
        flushers.add(flusher);
      }

      // Write some data
      for (int i = 0; i < 10000; i++) {
        stm.write(1);
      }

      // Close it while the flushing threads are still flushing
      stm.close();

      // Wait for the flushers to all die.
      for (Thread t : flushers) {
        t.join();
      }

      // They should have all gotten the expected exception, not anything
      // else.
      if (thrown.get() != null) {
        throw thrown.get();
      }

    } finally {
      fs.close();
      cluster.shutdown();
    }
  }

  public void doMultithreadedWrites(
      Configuration conf, Path p, int numThreads, int bufferSize, int numWrites,
      int replication) throws Exception {
    initBuffer(bufferSize);

    // create a new file.
    FileSystem fs = p.getFileSystem(conf);
    FSDataOutputStream stm = createFile(fs, p, replication);
    System.out.println("Created file simpleFlush.dat");

    // There have been a couple issues with flushing empty buffers, so do
    // some empty flushes first.
    stm.hflush();
    stm.hflush();
    stm.write(1);
    stm.hflush();
    stm.hflush();

    CountDownLatch countdown = new CountDownLatch(1);
    ArrayList<Thread> threads = new ArrayList<Thread>();
    AtomicReference<Throwable> thrown = new AtomicReference<Throwable>();
    for (int i = 0; i < numThreads; i++) {
      Thread t = new WriterThread(stm, thrown, countdown, numWrites);
      threads.add(t);
      t.start();
    }

    // Start all the threads at the same time for maximum raciness!
    countdown.countDown();

    for (Thread t : threads) {
      t.join();
    }
    if (thrown.get() != null) {
      throw new RuntimeException("Deferred", thrown.get());
    }
    stm.close();
    System.out.println("Closed file.");
  }
}

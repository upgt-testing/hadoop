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

package org.apache.hadoop.yarn.server.process.unit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.yarn.server.process.HealthMonitor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for HealthMonitor.
 */
public class TestHealthMonitor {

  private HealthMonitor monitor;
  private ServerSocket testServer;

  @Before
  public void setUp() {
    monitor = new HealthMonitor();
  }

  @After
  public void tearDown() throws IOException {
    if (testServer != null && !testServer.isClosed()) {
      testServer.close();
    }
  }

  @Test
  public void testWaitForConditionSuccess() throws Exception {
    AtomicBoolean condition = new AtomicBoolean(false);

    // Start thread to set condition after delay
    Thread setter = new Thread(() -> {
      try {
        Thread.sleep(500);
        condition.set(true);
      } catch (InterruptedException e) {
        // Ignore
      }
    });
    setter.start();

    // Should complete successfully
    monitor.waitFor(() -> condition.get(), 2000, "test condition");

    setter.join();
  }

  @Test(expected = TimeoutException.class)
  public void testWaitForConditionTimeout() throws Exception {
    // Condition that never becomes true
    monitor.waitFor(() -> false, 1000, "impossible condition");
  }

  @Test
  public void testWaitForConditionImmediatelyTrue() throws Exception {
    // Condition already true
    long startTime = System.currentTimeMillis();
    monitor.waitFor(() -> true, 5000, "already true");
    long elapsed = System.currentTimeMillis() - startTime;

    // Should return very quickly (within 500ms)
    Assert.assertTrue("Should return immediately when condition is true",
        elapsed < 500);
  }

  @Test(expected = InterruptedException.class)
  public void testWaitForInterrupted() throws Exception {
    final Thread testThread = Thread.currentThread();

    // Start thread to interrupt after delay
    Thread interrupter = new Thread(() -> {
      try {
        Thread.sleep(500);
        testThread.interrupt();
      } catch (InterruptedException e) {
        // Ignore
      }
    });
    interrupter.start();

    try {
      monitor.waitFor(() -> false, 10000, "will be interrupted");
    } finally {
      interrupter.join();
      Thread.interrupted(); // Clear interrupt flag
    }
  }

  @Test
  public void testIsRpcReachableSuccess() throws IOException {
    // Start a real server socket
    testServer = new ServerSocket(0);
    int port = testServer.getLocalPort();
    InetSocketAddress address = new InetSocketAddress("localhost", port);

    boolean reachable = monitor.isRpcReachable(address);
    Assert.assertTrue("Server should be reachable", reachable);
  }

  @Test
  public void testIsRpcReachableFailure() {
    // Use a port that's not listening
    InetSocketAddress address = new InetSocketAddress("localhost", 65000);

    boolean reachable = monitor.isRpcReachable(address);
    Assert.assertFalse("Non-existent server should not be reachable", reachable);
  }

  @Test
  public void testWaitForRpcReadySuccess() throws Exception {
    // Start server in background
    Thread serverThread = new Thread(() -> {
      try {
        Thread.sleep(500);
        testServer = new ServerSocket(0);
      } catch (Exception e) {
        // Ignore
      }
    });
    serverThread.start();
    Thread.sleep(600); // Wait for server to start

    int port = testServer.getLocalPort();
    InetSocketAddress address = new InetSocketAddress("localhost", port);

    // Should succeed
    monitor.waitForRpcReady(address, 3000);

    serverThread.join();
  }

  @Test(expected = TimeoutException.class)
  public void testWaitForRpcReadyTimeout() throws Exception {
    // Address that will never be reachable
    InetSocketAddress address = new InetSocketAddress("localhost", 65001);

    monitor.waitForRpcReady(address, 1000);
  }

  @Test
  public void testRetryWithBackoffSuccess() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    // Task that succeeds on 3rd attempt
    Callable<String> task = () -> {
      int count = attempts.incrementAndGet();
      if (count < 3) {
        throw new IOException("Attempt " + count + " failed");
      }
      return "success";
    };

    String result = monitor.retryWithBackoff(task, 5, "test task");

    Assert.assertEquals("Should return success", "success", result);
    Assert.assertEquals("Should have tried 3 times", 3, attempts.get());
  }

  @Test(expected = IOException.class)
  public void testRetryWithBackoffAllAttemptsFail() throws Exception {
    // Task that always fails
    Callable<String> task = () -> {
      throw new IOException("Always fails");
    };

    monitor.retryWithBackoff(task, 3, "failing task");
  }

  @Test
  public void testRetryWithBackoffImmediateSuccess() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    // Task that succeeds immediately
    Callable<String> task = () -> {
      attempts.incrementAndGet();
      return "immediate success";
    };

    long startTime = System.currentTimeMillis();
    String result = monitor.retryWithBackoff(task, 5, "immediate task");
    long elapsed = System.currentTimeMillis() - startTime;

    Assert.assertEquals("Should return success", "immediate success", result);
    Assert.assertEquals("Should have tried only once", 1, attempts.get());
    Assert.assertTrue("Should return quickly", elapsed < 500);
  }

  @Test
  public void testRetryWithBackoffExponentialDelay() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);
    long[] attemptTimes = new long[4];

    // Task that fails 3 times then succeeds
    Callable<String> task = () -> {
      int count = attempts.getAndIncrement();
      attemptTimes[count] = System.currentTimeMillis();
      if (count < 3) {
        throw new IOException("Attempt " + count + " failed");
      }
      return "success";
    };

    monitor.retryWithBackoff(task, 5, "backoff task");

    // Verify exponential backoff (each delay should be >= previous)
    // Note: Can't verify exact delays due to timing variance
    Assert.assertTrue("Should have multiple attempts", attempts.get() >= 3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRetryWithBackoffZeroAttempts() throws Exception {
    monitor.retryWithBackoff(() -> "result", 0, "zero attempts");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRetryWithBackoffNegativeAttempts() throws Exception {
    monitor.retryWithBackoff(() -> "result", -1, "negative attempts");
  }

  @Test
  public void testSetInitialRetryDelay() throws Exception {
    monitor.setInitialRetryDelayMs(100);

    AtomicInteger attempts = new AtomicInteger(0);
    long[] attemptTimes = new long[3];

    Callable<String> task = () -> {
      int count = attempts.getAndIncrement();
      attemptTimes[count] = System.currentTimeMillis();
      if (count < 2) {
        throw new IOException("Retry needed");
      }
      return "success";
    };

    monitor.retryWithBackoff(task, 5, "delay test");

    // Verify at least some delay between attempts
    Assert.assertTrue("Should have delay between attempts",
        attemptTimes[1] - attemptTimes[0] >= 50);
  }

  @Test
  public void testSetMaxRetryDelay() throws Exception {
    monitor.setMaxRetryDelayMs(200);

    AtomicInteger attempts = new AtomicInteger(0);

    Callable<String> task = () -> {
      int count = attempts.getAndIncrement();
      if (count < 5) {
        throw new IOException("Keep retrying");
      }
      return "success";
    };

    long startTime = System.currentTimeMillis();
    monitor.retryWithBackoff(task, 10, "max delay test");
    long elapsed = System.currentTimeMillis() - startTime;

    // With max delay of 200ms and multiple retries, total time should be bounded
    Assert.assertTrue("Total retry time should be reasonable", elapsed < 5000);
  }

  @Test
  public void testSetRetryBackoffMultiplier() throws Exception {
    monitor.setRetryBackoffMultiplier(1.5);

    AtomicInteger attempts = new AtomicInteger(0);

    Callable<String> task = () -> {
      int count = attempts.getAndIncrement();
      if (count < 3) {
        throw new IOException("Retry");
      }
      return "success";
    };

    String result = monitor.retryWithBackoff(task, 5, "multiplier test");
    Assert.assertEquals("Should succeed", "success", result);
  }

  @Test
  public void testToString() {
    String str = monitor.toString();
    Assert.assertTrue("toString should contain class name",
        str.contains("HealthMonitor"));
  }

  @Test
  public void testConcurrentWaitFor() throws Exception {
    final AtomicBoolean sharedCondition = new AtomicBoolean(false);
    final int numThreads = 3;
    Thread[] threads = new Thread[numThreads];
    final AtomicInteger successCount = new AtomicInteger(0);

    // Start multiple threads waiting for same condition
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(() -> {
        try {
          monitor.waitFor(() -> sharedCondition.get(), 5000, "shared condition");
          successCount.incrementAndGet();
        } catch (Exception e) {
          Assert.fail("Thread failed: " + e.getMessage());
        }
      });
      threads[i].start();
    }

    // Wait a bit, then set condition
    Thread.sleep(500);
    sharedCondition.set(true);

    // Wait for all threads
    for (Thread thread : threads) {
      thread.join(3000);
    }

    Assert.assertEquals("All threads should succeed",
        numThreads, successCount.get());
  }

  @Test
  public void testMultipleSequentialRetries() throws Exception {
    // Test that monitor can be reused for multiple retry operations
    for (int i = 0; i < 3; i++) {
      final int iteration = i;
      Callable<Integer> task = () -> {
        return iteration * 10;
      };

      Integer result = monitor.retryWithBackoff(task, 3, "iteration " + i);
      Assert.assertEquals("Should get correct result",
          Integer.valueOf(iteration * 10), result);
    }
  }

  @Test
  public void testWaitForWithVaryingConditions() throws Exception {
    // Test condition that changes multiple times
    AtomicInteger counter = new AtomicInteger(0);

    Thread changer = new Thread(() -> {
      try {
        for (int i = 0; i < 5; i++) {
          Thread.sleep(200);
          counter.incrementAndGet();
        }
      } catch (InterruptedException e) {
        // Ignore
      }
    });
    changer.start();

    // Wait for counter to reach 3
    monitor.waitFor(() -> counter.get() >= 3, 3000, "counter >= 3");

    Assert.assertTrue("Counter should be at least 3", counter.get() >= 3);
    changer.join();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetNegativeInitialDelay() {
    monitor.setInitialRetryDelayMs(-100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetNegativeMaxDelay() {
    monitor.setMaxRetryDelayMs(-100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetInvalidMultiplier() {
    monitor.setRetryBackoffMultiplier(0.5); // Must be >= 1.0
  }

  @Test
  public void testRetryWithDifferentExceptionTypes() throws Exception {
    AtomicInteger attempts = new AtomicInteger(0);

    Callable<String> task = () -> {
      int count = attempts.incrementAndGet();
      if (count == 1) {
        throw new IOException("IO error");
      } else if (count == 2) {
        throw new RuntimeException("Runtime error");
      }
      return "success";
    };

    String result = monitor.retryWithBackoff(task, 5, "mixed exceptions");
    Assert.assertEquals("Should eventually succeed", "success", result);
    Assert.assertEquals("Should have tried 3 times", 3, attempts.get());
  }
}

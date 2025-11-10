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

package org.apache.hadoop.yarn.server.process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.hadoop.test.GenericTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Health monitoring and retry logic for YARN node processes. This class
 * provides comprehensive health checking with exponential backoff retry
 * strategies for process-based YARN clusters.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Socket-based RPC connectivity checks</li>
 *   <li>Process liveness monitoring</li>
 *   <li>Exponential backoff retry with configurable parameters</li>
 *   <li>Cluster readiness verification</li>
 *   <li>Flexible health check predicates</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * HealthMonitor monitor = new HealthMonitor();
 *
 * // Wait for RM RPC port to be listening
 * InetSocketAddress rmAddress = new InetSocketAddress("localhost", 8032);
 * monitor.waitForRpcReady(rmAddress, 60000);
 *
 * // Wait for condition with exponential backoff
 * monitor.waitFor(() -&gt; {
 *   // Check some condition
 *   return cluster.isReady();
 * }, 1000, 60000, "cluster ready");
 *
 * // Check process health
 * boolean healthy = monitor.isProcessHealthy(process, rmAddress);
 * </pre>
 *
 * @see ProcessNodeManager
 * @see ProcessBasedMiniYARNCluster
 */
public class HealthMonitor {

  private static final Logger LOG =
      LoggerFactory.getLogger(HealthMonitor.class);

  /** Default initial retry delay (ms) */
  private static final long DEFAULT_INITIAL_DELAY_MS = 100;

  /** Default maximum retry delay (ms) */
  private static final long DEFAULT_MAX_DELAY_MS = 5000;

  /** Default backoff multiplier */
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 1.5;

  /** Socket connection timeout for RPC checks (ms) */
  private static final int RPC_SOCKET_TIMEOUT_MS = 1000;

  /** Initial delay for exponential backoff (ms) */
  private final long initialDelayMs;

  /** Maximum delay for exponential backoff (ms) */
  private final long maxDelayMs;

  /** Backoff multiplier (typically 1.5 - 2.0) */
  private final double backoffMultiplier;

  /**
   * Creates a HealthMonitor with default retry parameters.
   */
  public HealthMonitor() {
    this(DEFAULT_INITIAL_DELAY_MS, DEFAULT_MAX_DELAY_MS,
        DEFAULT_BACKOFF_MULTIPLIER);
  }

  /**
   * Creates a HealthMonitor with custom retry parameters.
   *
   * @param initialDelayMs Initial delay between retries
   * @param maxDelayMs Maximum delay between retries
   * @param backoffMultiplier Multiplier for exponential backoff (e.g., 1.5)
   */
  public HealthMonitor(long initialDelayMs, long maxDelayMs,
      double backoffMultiplier) {
    if (initialDelayMs <= 0) {
      throw new IllegalArgumentException(
          "Initial delay must be positive (got: " + initialDelayMs + ")");
    }
    if (maxDelayMs < initialDelayMs) {
      throw new IllegalArgumentException(
          "Max delay must be >= initial delay (got: " + maxDelayMs +
          " < " + initialDelayMs + ")");
    }
    if (backoffMultiplier < 1.0) {
      throw new IllegalArgumentException(
          "Backoff multiplier must be >= 1.0 (got: " + backoffMultiplier + ")");
    }

    this.initialDelayMs = initialDelayMs;
    this.maxDelayMs = maxDelayMs;
    this.backoffMultiplier = backoffMultiplier;

    LOG.debug("Created HealthMonitor: initialDelay={}ms, maxDelay={}ms, " +
        "multiplier={}", initialDelayMs, maxDelayMs, backoffMultiplier);
  }

  /**
   * Waits for a condition to become true with exponential backoff retry.
   * This is a wrapper around GenericTestUtils.waitFor with logging.
   *
   * @param condition Supplier that returns true when condition is met
   * @param timeoutMs Total timeout in milliseconds
   * @param description Description of what we're waiting for (for logging)
   * @throws TimeoutException if condition not met within timeout
   * @throws InterruptedException if wait is interrupted
   */
  public void waitFor(Supplier<Boolean> condition, long timeoutMs,
      String description)
      throws TimeoutException, InterruptedException {
    LOG.info("Waiting for: {} (timeout={}ms)", description, timeoutMs);

    long startTime = System.currentTimeMillis();
    try {
      GenericTestUtils.waitFor(condition, initialDelayMs, timeoutMs);
      long elapsed = System.currentTimeMillis() - startTime;
      LOG.info("Condition met: {} (took {}ms)", description, elapsed);
    } catch (TimeoutException e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOG.error("Timeout waiting for: {} (waited {}ms)", description, elapsed);
      throw new TimeoutException(
          "Timeout waiting for: " + description +
          " (waited " + elapsed + "ms)");
    }
  }

  /**
   * Waits for a condition with custom polling interval.
   *
   * @param condition Supplier that returns true when condition is met
   * @param pollIntervalMs Polling interval in milliseconds
   * @param timeoutMs Total timeout in milliseconds
   * @param description Description of what we're waiting for
   * @throws TimeoutException if condition not met within timeout
   * @throws InterruptedException if wait is interrupted
   */
  public void waitFor(Supplier<Boolean> condition, long pollIntervalMs,
      long timeoutMs, String description)
      throws TimeoutException, InterruptedException {
    LOG.info("Waiting for: {} (poll={}ms, timeout={}ms)",
        description, pollIntervalMs, timeoutMs);

    long startTime = System.currentTimeMillis();
    try {
      GenericTestUtils.waitFor(condition, pollIntervalMs, timeoutMs);
      long elapsed = System.currentTimeMillis() - startTime;
      LOG.info("Condition met: {} (took {}ms)", description, elapsed);
    } catch (TimeoutException e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOG.error("Timeout waiting for: {} (waited {}ms)", description, elapsed);
      throw new TimeoutException(
          "Timeout waiting for: " + description +
          " (waited " + elapsed + "ms)");
    }
  }

  /**
   * Waits for an RPC server to become ready (port listening).
   * Uses socket connectivity as a readiness check.
   *
   * @param rpcAddress RPC address to check
   * @param timeoutMs Timeout in milliseconds
   * @throws TimeoutException if RPC not ready within timeout
   * @throws InterruptedException if wait is interrupted
   */
  public void waitForRpcReady(InetSocketAddress rpcAddress, long timeoutMs)
      throws TimeoutException, InterruptedException {
    String description = "RPC ready at " + rpcAddress.getHostString() +
        ":" + rpcAddress.getPort();

    waitFor(() -> isRpcReachable(rpcAddress), timeoutMs, description);
  }

  /**
   * Waits for multiple RPC servers to become ready.
   *
   * @param rpcAddresses Array of RPC addresses to check
   * @param timeoutMs Timeout in milliseconds
   * @throws TimeoutException if any RPC not ready within timeout
   * @throws InterruptedException if wait is interrupted
   */
  public void waitForAllRpcReady(InetSocketAddress[] rpcAddresses,
      long timeoutMs) throws TimeoutException, InterruptedException {
    String description = "All " + rpcAddresses.length + " RPC servers ready";

    waitFor(() -> {
      for (InetSocketAddress addr : rpcAddresses) {
        if (!isRpcReachable(addr)) {
          return false;
        }
      }
      return true;
    }, timeoutMs, description);
  }

  /**
   * Checks if an RPC server is reachable via socket connection.
   *
   * @param rpcAddress RPC address to check
   * @return true if socket connection succeeds, false otherwise
   */
  public boolean isRpcReachable(InetSocketAddress rpcAddress) {
    try (Socket socket = new Socket()) {
      socket.connect(rpcAddress, RPC_SOCKET_TIMEOUT_MS);
      LOG.trace("RPC reachable: {}:{}", rpcAddress.getHostString(),
          rpcAddress.getPort());
      return true;
    } catch (IOException e) {
      LOG.trace("RPC not reachable: {}:{} - {}",
          rpcAddress.getHostString(), rpcAddress.getPort(), e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a process is healthy (alive and RPC reachable).
   *
   * @param process The process to check
   * @param rpcAddress RPC address for the process
   * @return true if process is alive and RPC is reachable
   */
  public boolean isProcessHealthy(Process process,
      InetSocketAddress rpcAddress) {
    if (process == null) {
      LOG.trace("Process health check: process is null");
      return false;
    }

    // Check if process is alive
    if (!process.isAlive()) {
      LOG.trace("Process health check: process is not alive");
      return false;
    }

    // Check if RPC is reachable
    if (rpcAddress != null && !isRpcReachable(rpcAddress)) {
      LOG.trace("Process health check: RPC not reachable");
      return false;
    }

    return true;
  }

  /**
   * Executes a task with exponential backoff retry.
   *
   * @param task Task to execute
   * @param maxAttempts Maximum number of attempts
   * @param description Description of the task (for logging)
   * @return Result from the task
   * @throws Exception if all attempts fail
   */
  public <T> T retryWithBackoff(Callable<T> task, int maxAttempts,
      String description) throws Exception {
    if (maxAttempts <= 0) {
      throw new IllegalArgumentException(
          "Max attempts must be positive (got: " + maxAttempts + ")");
    }

    long currentDelay = initialDelayMs;
    Exception lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        LOG.debug("Attempting: {} (attempt {}/{})",
            description, attempt, maxAttempts);
        T result = task.call();
        LOG.debug("Succeeded: {} (attempt {}/{})",
            description, attempt, maxAttempts);
        return result;
      } catch (Exception e) {
        lastException = e;

        if (attempt < maxAttempts) {
          LOG.debug("Failed: {} (attempt {}/{}) - retrying in {}ms: {}",
              description, attempt, maxAttempts, currentDelay, e.getMessage());

          Thread.sleep(currentDelay);

          // Calculate next delay with exponential backoff
          currentDelay = Math.min(
              (long) (currentDelay * backoffMultiplier),
              maxDelayMs);
        } else {
          LOG.error("Failed: {} (all {} attempts exhausted)",
              description, maxAttempts);
        }
      }
    }

    throw new IOException(
        "Failed after " + maxAttempts + " attempts: " + description,
        lastException);
  }

  /**
   * Calculates the delay for a given attempt number using exponential backoff.
   *
   * @param attemptNumber Attempt number (1-based)
   * @return Delay in milliseconds
   */
  public long calculateBackoffDelay(int attemptNumber) {
    if (attemptNumber <= 0) {
      throw new IllegalArgumentException(
          "Attempt number must be positive (got: " + attemptNumber + ")");
    }

    // Calculate: initialDelay * multiplier^(attempt-1)
    long delay = (long) (initialDelayMs *
        Math.pow(backoffMultiplier, attemptNumber - 1));

    return Math.min(delay, maxDelayMs);
  }

  /**
   * Gets the initial delay for retries.
   *
   * @return Initial delay in milliseconds
   */
  public long getInitialDelayMs() {
    return initialDelayMs;
  }

  /**
   * Gets the maximum delay for retries.
   *
   * @return Maximum delay in milliseconds
   */
  public long getMaxDelayMs() {
    return maxDelayMs;
  }

  /**
   * Gets the backoff multiplier.
   *
   * @return Backoff multiplier
   */
  public double getBackoffMultiplier() {
    return backoffMultiplier;
  }

  @Override
  public String toString() {
    return "HealthMonitor{" +
        "initialDelayMs=" + initialDelayMs +
        ", maxDelayMs=" + maxDelayMs +
        ", backoffMultiplier=" + backoffMultiplier +
        '}';
  }
}

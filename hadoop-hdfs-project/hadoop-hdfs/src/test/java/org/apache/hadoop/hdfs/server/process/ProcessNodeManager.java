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
package org.apache.hadoop.hdfs.server.process;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for managing Hadoop node processes (NameNode, DataNode) in
 * separate JVM processes. This enables testing with different Hadoop versions
 * and provides process isolation.
 *
 * <p>Each node process is launched in its own JVM with an isolated classpath
 * corresponding to a specific Hadoop distribution. The manager handles:
 * <ul>
 *   <li>Process lifecycle (start, stop, restart)</li>
 *   <li>Health monitoring via RPC</li>
 *   <li>Configuration management</li>
 *   <li>Logging and cleanup</li>
 * </ul>
 */
public abstract class ProcessNodeManager {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessNodeManager.class);

  /** Default timeout for process operations in milliseconds */
  protected static final long DEFAULT_TIMEOUT_MS = 60000; // 60 seconds

  /** Default polling interval for process readiness checks */
  protected static final long POLL_INTERVAL_MS = 500; // 0.5 seconds

  /** Maximum time to wait for graceful shutdown before forcing */
  protected static final long GRACEFUL_SHUTDOWN_TIMEOUT_MS = 30000; // 30 seconds

  /** The actual process object */
  protected Process process;

  /** Configuration for this node */
  protected final Configuration nodeConfig;

  /** Path to Hadoop distribution (e.g., /opt/hadoop-3.3.5) */
  protected final String hadoopHome;

  /** Working directory for this node (conf, data, logs) */
  protected final File workDir;

  /** Node index (0, 1, 2, ...) */
  protected final int nodeIndex;

  /** PID file for the process */
  protected File pidFile;

  /** Thread for monitoring process health */
  protected Thread monitorThread;

  /** Flag to stop monitoring thread */
  protected volatile boolean shouldMonitor = false;

  /** Flag indicating if process was started */
  protected volatile boolean started = false;

  /**
   * Constructor for ProcessNodeManager.
   *
   * @param nodeConfig Configuration for this node
   * @param hadoopHome Path to Hadoop distribution directory
   * @param workDir Working directory for this node
   * @param nodeIndex Index of this node
   */
  protected ProcessNodeManager(Configuration nodeConfig, String hadoopHome,
                                File workDir, int nodeIndex) {
    this.nodeConfig = nodeConfig;
    this.hadoopHome = hadoopHome;
    this.workDir = workDir;
    this.nodeIndex = nodeIndex;
    this.pidFile = new File(workDir, "pid");
  }

  /**
   * Start the node process.
   *
   * @throws IOException if process fails to start
   * @throws TimeoutException if process doesn't become ready in time
   */
  public void start() throws IOException, TimeoutException {
    if (started) {
      LOG.warn("Node {} already started, ignoring start request", nodeIndex);
      return;
    }

    LOG.info("Starting {} node {} with Hadoop from {}",
        getNodeType(), nodeIndex, hadoopHome);

    // Validate Hadoop distribution
    validateHadoopDistribution();

    // Build classpath for this node
    List<String> classpath = buildClasspath();

    // Build command line
    List<String> command = buildCommand(classpath);

    // Log the command for debugging
    LOG.info("Launching process with command: {}", String.join(" ", command));

    // Start the process
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true); // Merge stderr into stdout

    // Set environment variables for the subprocess
    Map<String, String> env = pb.environment();
    setupProcessEnvironment(env);

    // Redirect output to log file
    File logFile = new File(workDir, "logs/" + getNodeType().toLowerCase() + ".log");
    logFile.getParentFile().mkdirs();
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

    process = pb.start();
    started = true;

    // Write PID to file
    writePid();

    // Wait for process to be ready
    waitForProcessReady(DEFAULT_TIMEOUT_MS);

    // Start monitoring thread
    startMonitoring();

    LOG.info("{} node {} started successfully (PID: {})",
        getNodeType(), nodeIndex, getPid());
  }

  /**
   * Stop the node process gracefully.
   *
   * @throws IOException if stop operation fails
   */
  public void stop() throws IOException {
    if (!started || process == null) {
      LOG.warn("Node {} not started, ignoring stop request", nodeIndex);
      return;
    }

    LOG.info("Stopping {} node {}", getNodeType(), nodeIndex);

    // Stop monitoring
    stopMonitoring();

    try {
      // Try graceful shutdown first
      if (tryGracefulShutdown()) {
        LOG.info("Node {} shutdown gracefully", nodeIndex);
        return;
      }

      // If graceful shutdown fails, try SIGTERM
      LOG.warn("Graceful shutdown failed for node {}, sending SIGTERM", nodeIndex);
      process.destroy();

      if (waitForExit(GRACEFUL_SHUTDOWN_TIMEOUT_MS)) {
        LOG.info("Node {} terminated via SIGTERM", nodeIndex);
        return;
      }

      // Last resort: SIGKILL
      LOG.error("SIGTERM failed for node {}, sending SIGKILL", nodeIndex);
      process.destroyForcibly();
      waitForExit(10000); // Wait up to 10 seconds

    } finally {
      started = false;
      process = null;
      cleanup();
    }
  }

  /**
   * Restart the node process.
   *
   * @throws IOException if restart fails
   * @throws TimeoutException if node doesn't become ready
   */
  public void restart() throws IOException, TimeoutException {
    LOG.info("Restarting {} node {}", getNodeType(), nodeIndex);
    stop();
    try {
      Thread.sleep(1000); // Brief pause between stop and start
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during restart", e);
    }
    start();
  }

  /**
   * Check if the process is alive.
   *
   * @return true if process is running
   */
  public boolean isAlive() {
    return process != null && process.isAlive();
  }

  /**
   * Check if the node is healthy (process alive + RPC responding).
   *
   * @return true if node is healthy
   * @throws IOException if health check fails
   */
  public boolean isHealthy() throws IOException {
    if (!isAlive()) {
      return false;
    }

    try {
      return checkRpcHealth();
    } catch (Exception e) {
      LOG.warn("Health check failed for node {}: {}", nodeIndex, e.getMessage());
      return false;
    }
  }

  /**
   * Get the RPC address for this node.
   *
   * @return RPC address
   */
  public abstract InetSocketAddress getRpcAddress();

  /**
   * Setup environment variables for the node process.
   * This method configures the subprocess environment with all necessary
   * variables that Hadoop needs to run properly.
   *
   * @param env the environment map from ProcessBuilder
   */
  protected void setupProcessEnvironment(Map<String, String> env) {
    // 1. JAVA_HOME - Critical for Hadoop scripts
    String javaHome = System.getProperty("java.home");
    env.put("JAVA_HOME", javaHome);
    LOG.debug("Set JAVA_HOME={}", javaHome);

    // 2. HADOOP_HOME - Required by Hadoop
    env.put("HADOOP_HOME", hadoopHome);
    env.put("HADOOP_PREFIX", hadoopHome);  // Some versions use this
    LOG.debug("Set HADOOP_HOME={}", hadoopHome);

    // 3. HADOOP_CONF_DIR - Configuration directory
    File confDir = new File(workDir, "conf");
    env.put("HADOOP_CONF_DIR", confDir.getAbsolutePath());
    LOG.debug("Set HADOOP_CONF_DIR={}", confDir.getAbsolutePath());

    // 4. USER - Required by Hadoop for user identification
    String user = System.getProperty("user.name", "hadoop");
    env.put("USER", user);
    env.put("HADOOP_USER_NAME", user);
    LOG.debug("Set USER={}", user);

    // 5. PATH - Include Hadoop bin directory
    String existingPath = env.get("PATH");
    String hadoopBin = new File(hadoopHome, "bin").getAbsolutePath();
    String newPath = existingPath != null ?
        hadoopBin + File.pathSeparator + existingPath : hadoopBin;
    env.put("PATH", newPath);
    LOG.debug("Set PATH with Hadoop bin: {}", hadoopBin);

    // 6. Library path for native libraries
    String osName = System.getProperty("os.name").toLowerCase();
    String libPathVar = osName.contains("mac") ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH";

    File nativeLib = new File(hadoopHome, "lib/native");
    if (nativeLib.exists()) {
      String existingLibPath = env.get(libPathVar);
      String newLibPath = existingLibPath != null ?
          nativeLib.getAbsolutePath() + File.pathSeparator + existingLibPath :
          nativeLib.getAbsolutePath();
      env.put(libPathVar, newLibPath);
      LOG.debug("Set {}={}", libPathVar, nativeLib.getAbsolutePath());
    } else {
      LOG.warn("Native library directory not found: {}", nativeLib.getAbsolutePath());
    }

    // 7. Disable native library warnings if natives not available
    env.put("HADOOP_OPTS", "-Djava.library.path=" +
        new File(hadoopHome, "lib/native").getAbsolutePath());

    // 8. Set process-specific environment
    env.put("HADOOP_IDENT_STRING", getNodeType().toLowerCase() + nodeIndex);

    // 9. Inherit critical system environment variables
    String[] inheritVars = {"LANG", "LC_ALL", "TZ"};
    for (String var : inheritVars) {
      String value = System.getenv(var);
      if (value != null) {
        env.put(var, value);
      }
    }

    LOG.info("Process environment configured with {} variables", env.size());
  }

  /**
   * Get the node type (e.g., "NameNode", "DataNode").
   *
   * @return node type string
   */
  protected abstract String getNodeType();

  /**
   * Build the classpath for this node process.
   *
   * @return list of classpath entries
   * @throws IOException if classpath construction fails
   */
  protected abstract List<String> buildClasspath() throws IOException;

  /**
   * Build the full command line to launch the node process.
   *
   * @param classpath classpath entries
   * @return command line as list of strings
   */
  protected abstract List<String> buildCommand(List<String> classpath);

  /**
   * Check if the node is healthy via RPC.
   * Subclasses should implement node-specific health checks.
   *
   * @return true if RPC health check passes
   * @throws IOException if health check fails
   */
  protected abstract boolean checkRpcHealth() throws IOException;

  /**
   * Wait for the process to be ready to accept connections.
   *
   * @param timeoutMs maximum time to wait in milliseconds
   * @throws TimeoutException if node doesn't become ready in time
   * @throws IOException if readiness check fails
   */
  protected void waitForProcessReady(long timeoutMs)
      throws TimeoutException, IOException {
    LOG.info("Waiting for {} node {} to be ready (timeout: {}ms)",
        getNodeType(), nodeIndex, timeoutMs);

    long startTime = System.currentTimeMillis();
    long deadline = startTime + timeoutMs;
    int attempts = 0;

    while (System.currentTimeMillis() < deadline) {
      attempts++;

      // Check if process is still alive
      if (!isAlive()) {
        // Try to get exit code and log file contents
        String exitInfo = "";
        if (process != null) {
          try {
            int exitCode = process.exitValue();
            exitInfo = String.format(" (exit code: %d)", exitCode);
          } catch (IllegalThreadStateException e) {
            // Process still running, shouldn't happen
          }
        }

        // Try to read log file for error details
        File logFile = new File(workDir, "logs/" + getNodeType().toLowerCase() + ".log");
        if (logFile.exists()) {
          try {
            String logContent = new String(java.nio.file.Files.readAllBytes(logFile.toPath()));
            LOG.error("Process log file contents:\n{}", logContent);
          } catch (Exception e) {
            LOG.warn("Failed to read log file: {}", e.getMessage());
          }
        }

        throw new IOException(String.format(
            "Process for node %d died during startup%s", nodeIndex, exitInfo));
      }

      // Try health check
      try {
        if (checkRpcHealth()) {
          long elapsed = System.currentTimeMillis() - startTime;
          LOG.info("Node {} ready after {}ms ({} attempts)",
              nodeIndex, elapsed, attempts);
          return;
        }
      } catch (Exception e) {
        // Expected during startup, keep trying
        LOG.trace("Health check attempt {}: {}", attempts, e.getMessage());
      }

      // Wait before next attempt
      try {
        Thread.sleep(POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for node ready", e);
      }
    }

    // Try to read log file for troubleshooting
    File logFile = new File(workDir, "logs/" + getNodeType().toLowerCase() + ".log");
    if (logFile.exists()) {
      try {
        String logContent = new String(java.nio.file.Files.readAllBytes(logFile.toPath()));
        LOG.error("Process still alive but RPC not responding. Log file contents:\n{}", logContent);
      } catch (Exception e) {
        LOG.warn("Failed to read log file: {}", e.getMessage());
      }
    }

    throw new TimeoutException(String.format(
        "Node %d did not become ready within %dms (%d attempts)",
        nodeIndex, timeoutMs, attempts));
  }

  /**
   * Attempt graceful shutdown of the node.
   * Subclasses can override to implement node-specific shutdown.
   *
   * @return true if graceful shutdown succeeded
   */
  protected boolean tryGracefulShutdown() {
    // Default implementation: just check if process exits
    // Subclasses can implement RPC-based shutdown
    return waitForExit(GRACEFUL_SHUTDOWN_TIMEOUT_MS);
  }

  /**
   * Wait for the process to exit.
   *
   * @param timeoutMs maximum time to wait
   * @return true if process exited within timeout
   */
  protected boolean waitForExit(long timeoutMs) {
    if (process == null) {
      return true;
    }

    try {
      return process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Validate that the Hadoop distribution exists and is complete.
   *
   * @throws IOException if validation fails
   */
  protected void validateHadoopDistribution() throws IOException {
    File hadoopHomeDir = new File(hadoopHome);
    if (!hadoopHomeDir.exists() || !hadoopHomeDir.isDirectory()) {
      throw new IOException(String.format(
          "Hadoop home directory does not exist: %s", hadoopHome));
    }

    // Check for required directories
    String[] requiredDirs = {"share/hadoop/common", "share/hadoop/hdfs"};
    for (String dir : requiredDirs) {
      File requiredDir = new File(hadoopHomeDir, dir);
      if (!requiredDir.exists()) {
        throw new IOException(String.format(
            "Required directory missing in Hadoop distribution: %s",
            requiredDir.getAbsolutePath()));
      }
    }
  }

  /**
   * Write the process PID to a file.
   * Note: PID extraction is Java version-dependent.
   */
  protected void writePid() {
    if (process != null) {
      try {
        // Try to get PID using reflection (works in Java 9+)
        // For Java 8, this will fail gracefully
        long pid = getPidReflectively(process);
        if (pid > 0) {
          java.io.FileWriter writer = new java.io.FileWriter(pidFile);
          writer.write(String.valueOf(pid));
          writer.close();
          LOG.debug("Wrote PID {} to {}", pid, pidFile);
        }
      } catch (Exception e) {
        LOG.trace("Failed to write PID file: {}", e.getMessage());
      }
    }
  }

  /**
   * Get process PID using reflection (Java 9+ compatible).
   *
   * @param process process to get PID from
   * @return PID or -1 if unavailable
   */
  private long getPidReflectively(Process process) {
    try {
      // Try Java 9+ method: process.pid()
      java.lang.reflect.Method pidMethod = process.getClass().getMethod("pid");
      return (Long) pidMethod.invoke(process);
    } catch (Exception e) {
      // Java 8 - PID not easily available
      LOG.trace("PID not available: {}", e.getMessage());
      return -1;
    }
  }

  /**
   * Get the process PID.
   *
   * @return PID or -1 if unknown
   */
  protected long getPid() {
    if (process != null) {
      return getPidReflectively(process);
    }

    // Try reading from PID file
    if (pidFile.exists()) {
      try {
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.FileReader(pidFile));
        String line = reader.readLine();
        reader.close();
        if (line != null) {
          return Long.parseLong(line.trim());
        }
      } catch (Exception e) {
        LOG.trace("Failed to read PID from file: {}", e.getMessage());
      }
    }

    return -1;
  }

  /**
   * Start monitoring thread for this process.
   */
  protected void startMonitoring() {
    shouldMonitor = true;
    monitorThread = new Thread(() -> {
      LOG.debug("Monitor thread started for node {}", nodeIndex);

      while (shouldMonitor && isAlive()) {
        try {
          Thread.sleep(5000); // Check every 5 seconds

          if (!isHealthy()) {
            LOG.warn("Node {} failed health check", nodeIndex);
          }

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          LOG.debug("Monitor thread exception: {}", e.getMessage());
        }
      }

      if (!shouldMonitor) {
        LOG.debug("Monitor thread stopped for node {}", nodeIndex);
      } else {
        LOG.error("Process for node {} died unexpectedly", nodeIndex);
      }
    }, getNodeType() + "-" + nodeIndex + "-monitor");

    monitorThread.setDaemon(true);
    monitorThread.start();
  }

  /**
   * Stop the monitoring thread.
   */
  protected void stopMonitoring() {
    shouldMonitor = false;
    if (monitorThread != null && monitorThread.isAlive()) {
      monitorThread.interrupt();
      try {
        monitorThread.join(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Cleanup resources after node stops.
   */
  protected void cleanup() {
    // Delete PID file
    if (pidFile != null && pidFile.exists()) {
      try {
        Files.delete(pidFile.toPath());
      } catch (IOException e) {
        LOG.trace("Failed to delete PID file: {}", e.getMessage());
      }
    }

    // Subclasses can override for additional cleanup
  }

  /**
   * Get the working directory for this node.
   *
   * @return working directory
   */
  public File getWorkDir() {
    return workDir;
  }

  /**
   * Get the configuration for this node.
   *
   * @return node configuration
   */
  public Configuration getConfiguration() {
    return nodeConfig;
  }

  /**
   * Get the node index.
   *
   * @return node index
   */
  public int getNodeIndex() {
    return nodeIndex;
  }

  /**
   * Get the Hadoop home directory.
   *
   * @return Hadoop distribution path
   */
  public String getHadoopHome() {
    return hadoopHome;
  }
}

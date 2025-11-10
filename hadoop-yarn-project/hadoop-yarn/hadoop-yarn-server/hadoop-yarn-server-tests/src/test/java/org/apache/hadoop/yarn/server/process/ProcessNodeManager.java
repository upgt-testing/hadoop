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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for managing YARN node processes (ResourceManager, NodeManager).
 * This class provides common functionality for starting, stopping, and
 * monitoring YARN nodes that run in separate JVM processes.
 *
 * <p>Each process is isolated with its own classpath, configuration, and
 * working directory, enabling multi-version testing and upgrade scenarios.</p>
 *
 * <p>Subclasses must implement:</p>
 * <ul>
 *   <li>{@link #start()} - Start the YARN node process</li>
 *   <li>{@link #stop()} - Stop the YARN node process gracefully</li>
 *   <li>{@link #isHealthy()} - Check if the node is healthy via RPC</li>
 *   <li>{@link #getRpcAddress()} - Get the RPC address of the node</li>
 * </ul>
 *
 * @see ResourceManagerProcessManager
 * @see NodeManagerProcessManager
 */
public abstract class ProcessNodeManager {

  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessNodeManager.class);

  /** Default timeout for process startup in milliseconds */
  protected static final long DEFAULT_STARTUP_TIMEOUT_MS = 60000;

  /** Default timeout for process shutdown in milliseconds */
  protected static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 30000;

  /** Poll interval for health checks in milliseconds */
  protected static final long HEALTH_CHECK_POLL_INTERVAL_MS = 500;

  /** The actual process running the YARN node */
  protected Process process;

  /** Configuration for this node */
  protected YarnConfiguration nodeConfig;

  /** Path to the Hadoop distribution (e.g., /opt/hadoop-3.3.6) */
  protected String hadoopHome;

  /** Working directory for this node (conf, data, logs) */
  protected File workDir;

  /** Index of this node (for multi-node setups) */
  protected int nodeIndex;

  /** Thread that reads stdout from the process */
  protected Thread stdoutReaderThread;

  /** Thread that reads stderr from the process */
  protected Thread stderrReaderThread;

  /**
   * Constructs a new ProcessNodeManager.
   *
   * @param nodeConfig Configuration for this node
   * @param hadoopHome Path to Hadoop distribution
   * @param workDir Working directory for this node
   * @param nodeIndex Index of this node
   */
  protected ProcessNodeManager(YarnConfiguration nodeConfig,
      String hadoopHome, File workDir, int nodeIndex) {
    this.nodeConfig = nodeConfig;
    this.hadoopHome = hadoopHome;
    this.workDir = workDir;
    this.nodeIndex = nodeIndex;
  }

  /**
   * Starts the YARN node process. This method should:
   * <ol>
   *   <li>Build the classpath for the process</li>
   *   <li>Build the command line to launch the process</li>
   *   <li>Start the process</li>
   *   <li>Wait for the process to be ready (RPC server up)</li>
   * </ol>
   *
   * @throws IOException if the process fails to start
   */
  public abstract void start() throws IOException;

  /**
   * Stops the YARN node process gracefully. This method should:
   * <ol>
   *   <li>Try graceful shutdown via RPC (e.g., RMAdminProtocol)</li>
   *   <li>If graceful fails, send SIGTERM</li>
   *   <li>If SIGTERM fails, send SIGKILL</li>
   *   <li>Clean up resources</li>
   * </ol>
   *
   * @throws IOException if the process cannot be stopped
   */
  public abstract void stop() throws IOException;

  /**
   * Checks if the node is healthy and responding to RPC requests.
   *
   * @return true if the node is healthy, false otherwise
   * @throws IOException if health check fails
   */
  public abstract boolean isHealthy() throws IOException;

  /**
   * Gets the RPC address of this node.
   *
   * @return the RPC address (hostname:port)
   */
  public abstract InetSocketAddress getRpcAddress();

  /**
   * Waits for the process to be ready. This polls {@link #isHealthy()}
   * until it returns true or the timeout expires.
   *
   * @param timeoutMs Timeout in milliseconds
   * @throws IOException if process fails during startup
   * @throws TimeoutException if timeout expires before process is ready
   */
  protected void waitForProcessReady(long timeoutMs)
      throws IOException, TimeoutException {
    LOG.info("Waiting for {} process {} to be ready (timeout: {}ms)",
        getNodeType(), nodeIndex, timeoutMs);

    try {
      GenericTestUtils.waitFor(() -> {
        try {
          // Check if process is still alive
          if (!isProcessAlive()) {
            throw new RuntimeException(
                getNodeType() + " process " + nodeIndex + " died during startup");
          }

          // Check if node is healthy via RPC
          return isHealthy();
        } catch (IOException e) {
          // RPC not ready yet, keep waiting
          LOG.debug("{} process {} not ready yet: {}",
              getNodeType(), nodeIndex, e.getMessage());
          return false;
        }
      }, (int) HEALTH_CHECK_POLL_INTERVAL_MS, (int) timeoutMs);

      LOG.info("{} process {} is ready", getNodeType(), nodeIndex);
    } catch (TimeoutException e) {
      throw new TimeoutException(
          getNodeType() + " process " + nodeIndex + " did not become ready within " +
          timeoutMs + "ms");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(
          "Interrupted while waiting for " + getNodeType() + " process " +
          nodeIndex + " to be ready", e);
    }
  }

  /**
   * Checks if the process is still alive.
   *
   * @return true if the process is alive, false otherwise
   */
  public boolean isProcessAlive() {
    return process != null && process.isAlive();
  }

  /**
   * Kills the process forcefully using SIGKILL (or equivalent).
   * This is a last resort if graceful shutdown fails.
   */
  protected void killProcess() {
    if (process != null && process.isAlive()) {
      LOG.warn("Force killing {} process {}", getNodeType(), nodeIndex);
      process.destroyForcibly();

      try {
        boolean exited = process.waitFor(5, TimeUnit.SECONDS);
        if (!exited) {
          LOG.error("{} process {} did not exit after force kill",
              getNodeType(), nodeIndex);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Interrupted while waiting for {} process {} to die",
            getNodeType(), nodeIndex, e);
      }
    }
  }

  /**
   * Builds the classpath for the YARN node process. The classpath should
   * include:
   * <ol>
   *   <li>Process launcher classes (from test classpath)</li>
   *   <li>YARN libraries (share/hadoop/yarn/*.jar)</li>
   *   <li>YARN dependencies (share/hadoop/yarn/lib/*.jar)</li>
   *   <li>Hadoop Common libraries (share/hadoop/common/*.jar)</li>
   *   <li>Hadoop Common dependencies (share/hadoop/common/lib/*.jar)</li>
   *   <li>HDFS libraries (share/hadoop/hdfs/*.jar)</li>
   *   <li>Configuration directory</li>
   * </ol>
   *
   * @return list of classpath entries (JAR files and directories)
   * @throws IOException if classpath cannot be built
   */
  protected List<String> buildClasspath() throws IOException {
    List<String> classpath = new ArrayList<>();

    File hadoopHomeDir = new File(hadoopHome);
    if (!hadoopHomeDir.exists() || !hadoopHomeDir.isDirectory()) {
      throw new IOException(
          "Hadoop home directory does not exist: " + hadoopHome);
    }

    // Add process launcher classes (from current test classpath)
    // This ensures the launcher entry point classes are available
    String testClasspath = System.getProperty("java.class.path");
    if (testClasspath != null) {
      // Only add the test JAR that contains the launcher classes
      for (String entry : testClasspath.split(File.pathSeparator)) {
        if (entry.contains("hadoop-yarn-server-tests")) {
          classpath.add(entry);
          break;
        }
      }
    }

    // Add YARN libraries
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/yarn"));
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/yarn/lib"));

    // Add YARN timeline service (in subdirectory)
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/yarn/timelineservice"));

    // Add Hadoop Common libraries
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/common"));
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/common/lib"));

    // Add HDFS libraries (YARN depends on HDFS)
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/hdfs"));
    addJarsFromDirectory(classpath,
        new File(hadoopHomeDir, "share/hadoop/hdfs/lib"));

    // Add configuration directory
    File confDir = new File(workDir, "conf");
    if (confDir.exists()) {
      classpath.add(confDir.getAbsolutePath());
    }

    LOG.debug("Built classpath with {} entries for {} process {}",
        classpath.size(), getNodeType(), nodeIndex);

    return classpath;
  }

  /**
   * Adds all JAR files from a directory to the classpath.
   *
   * @param classpath The classpath list to add to
   * @param dir The directory to search for JARs
   */
  protected void addJarsFromDirectory(List<String> classpath, File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      LOG.debug("Directory does not exist, skipping: {}", dir);
      return;
    }

    File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
    if (jars != null) {
      for (File jar : jars) {
        classpath.add(jar.getAbsolutePath());
      }
    }
  }

  /**
   * Starts threads to read stdout and stderr from the process.
   * This prevents the process from blocking on full output buffers.
   *
   * @param logPrefix Prefix for log messages (e.g., "RM-0")
   */
  protected void startOutputReaderThreads(String logPrefix) {
    if (process == null) {
      return;
    }

    // Read stdout
    stdoutReaderThread = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          LOG.info("[{} stdout] {}", logPrefix, line);
        }
      } catch (IOException e) {
        LOG.debug("[{} stdout] Stream closed", logPrefix);
      }
    }, logPrefix + "-stdout-reader");
    stdoutReaderThread.setDaemon(true);
    stdoutReaderThread.start();

    // Read stderr
    stderrReaderThread = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getErrorStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          LOG.warn("[{} stderr] {}", logPrefix, line);
        }
      } catch (IOException e) {
        LOG.debug("[{} stderr] Stream closed", logPrefix);
      }
    }, logPrefix + "-stderr-reader");
    stderrReaderThread.setDaemon(true);
    stderrReaderThread.start();
  }

  /**
   * Gets the type of this node (e.g., "ResourceManager", "NodeManager").
   *
   * @return the node type
   */
  protected abstract String getNodeType();

  /**
   * Gets the configuration for this node.
   *
   * @return the node configuration
   */
  public Configuration getConfiguration() {
    return nodeConfig;
  }

  /**
   * Gets the working directory for this node.
   *
   * @return the working directory
   */
  public File getWorkDir() {
    return workDir;
  }

  /**
   * Gets the index of this node.
   *
   * @return the node index
   */
  public int getNodeIndex() {
    return nodeIndex;
  }

  /**
   * Gets the Hadoop home directory for this node.
   *
   * @return the Hadoop home path
   */
  public String getHadoopHome() {
    return hadoopHome;
  }
}

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
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for running HAAdmin commands against ProcessBasedMiniDFSCluster.
 *
 * <p>This class provides methods to control HA NameNode state transitions and
 * health monitoring using the Hadoop HAAdmin tool. It executes commands as
 * separate processes, similar to how a user would run them from the command line.
 *
 * <p>Supported operations:
 * <ul>
 *   <li>transitionToActive - Transition a NameNode to active state</li>
 *   <li>transitionToStandby - Transition a NameNode to standby state</li>
 *   <li>getServiceState - Get the current state of a NameNode</li>
 *   <li>checkHealth - Check the health of a NameNode</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * HAAdminCommandRunner runner = new HAAdminCommandRunner(
 *     hadoopHome, configuration, workDir);
 *
 * // Transition nn1 to active
 * runner.transitionToActive("hdfs-ha", "nn1");
 *
 * // Get state of nn2
 * String state = runner.getServiceState("hdfs-ha", "nn2");
 * </pre>
 */
public class HAAdminCommandRunner {
  private static final Logger LOG = LoggerFactory.getLogger(HAAdminCommandRunner.class);

  private final String hadoopHome;
  private final Configuration config;
  private final File workDir;
  private final List<String> classpath;

  /**
   * Creates a new HAAdminCommandRunner.
   *
   * @param hadoopHome path to Hadoop distribution
   * @param config Hadoop configuration
   * @param workDir working directory for command execution
   * @throws IOException if classpath building fails
   */
  public HAAdminCommandRunner(String hadoopHome, Configuration config, File workDir)
      throws IOException {
    this.hadoopHome = hadoopHome;
    this.config = config;
    this.workDir = workDir;
    this.classpath = buildClasspath();
  }

  /**
   * Transitions a NameNode to active state.
   *
   * @param nameservice the nameservice ID
   * @param nnId the NameNode ID (e.g., "nn1", "nn2")
   * @throws IOException if transition fails
   */
  public void transitionToActive(String nameservice, String nnId) throws IOException {
    LOG.info("Transitioning {} in nameservice {} to active", nnId, nameservice);

    List<String> args = new ArrayList<>();
    args.add("-transitionToActive");
    args.add(nnId);

    runHAAdminCommand(nameservice, args);
    LOG.info("Successfully transitioned {} to active", nnId);
  }

  /**
   * Transitions a NameNode to standby state.
   *
   * @param nameservice the nameservice ID
   * @param nnId the NameNode ID (e.g., "nn1", "nn2")
   * @throws IOException if transition fails
   */
  public void transitionToStandby(String nameservice, String nnId) throws IOException {
    LOG.info("Transitioning {} in nameservice {} to standby", nnId, nameservice);

    List<String> args = new ArrayList<>();
    args.add("-transitionToStandby");
    args.add(nnId);

    runHAAdminCommand(nameservice, args);
    LOG.info("Successfully transitioned {} to standby", nnId);
  }

  /**
   * Gets the current service state of a NameNode.
   *
   * @param nameservice the nameservice ID
   * @param nnId the NameNode ID (e.g., "nn1", "nn2")
   * @return the service state ("active", "standby", or other)
   * @throws IOException if command fails
   */
  public String getServiceState(String nameservice, String nnId) throws IOException {
    LOG.debug("Getting service state for {} in nameservice {}", nnId, nameservice);

    List<String> args = new ArrayList<>();
    args.add("-getServiceState");
    args.add(nnId);

    String output = runHAAdminCommand(nameservice, args);
    String state = output.trim().toLowerCase();
    LOG.debug("Service state for {}: {}", nnId, state);
    return state;
  }

  /**
   * Checks the health of a NameNode.
   *
   * @param nameservice the nameservice ID
   * @param nnId the NameNode ID (e.g., "nn1", "nn2")
   * @return true if healthy, false otherwise
   */
  public boolean checkHealth(String nameservice, String nnId) {
    LOG.debug("Checking health for {} in nameservice {}", nnId, nameservice);

    List<String> args = new ArrayList<>();
    args.add("-checkHealth");
    args.add(nnId);

    try {
      runHAAdminCommand(nameservice, args);
      LOG.debug("Health check passed for {}", nnId);
      return true;
    } catch (IOException e) {
      LOG.warn("Health check failed for {}: {}", nnId, e.getMessage());
      return false;
    }
  }

  /**
   * Runs an HAAdmin command with the given arguments.
   *
   * @param nameservice the nameservice ID
   * @param args command arguments (e.g., ["-transitionToActive", "nn1"])
   * @return command output
   * @throws IOException if command fails
   */
  private String runHAAdminCommand(String nameservice, List<String> args) throws IOException {
    // Build command
    List<String> command = new ArrayList<>();
    String javaHome = System.getProperty("java.home");
    command.add(new File(javaHome, "bin/java").getAbsolutePath());
    command.add("-Xmx512m");
    command.add("-Djava.library.path=" + new File(hadoopHome, "lib/native").getAbsolutePath());
    command.add("-Dhadoop.home.dir=" + hadoopHome);
    command.add("-Dhadoop.log.dir=" + new File(workDir, "logs").getAbsolutePath());
    command.add("-Dhadoop.log.file=hadoop-haadmin.log");
    command.add("-cp");
    command.add(String.join(File.pathSeparator, classpath));
    command.add("org.apache.hadoop.hdfs.tools.DFSHAAdmin");
    command.add("-ns");
    command.add(nameservice);
    command.addAll(args);

    LOG.debug("HAAdmin command: {}", String.join(" ", command));

    // Run command
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(workDir);
    pb.redirectErrorStream(true);

    // Set up environment - copy configuration
    java.util.Map<String, String> env = pb.environment();
    env.put("HADOOP_CONF_DIR", new File(workDir, "conf").getAbsolutePath());

    Process process = pb.start();

    // Capture output
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
        LOG.trace("HAAdmin output: {}", line);
      }
    }

    // Wait for completion
    try {
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new IOException(
            "HAAdmin command failed with exit code " + exitCode + ": " + output.toString());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while running HAAdmin command", e);
    }

    return output.toString();
  }

  /**
   * Builds classpath for HAAdmin commands.
   *
   * @return list of classpath entries
   */
  private List<String> buildClasspath() throws IOException {
    List<String> cp = new ArrayList<>();

    File hadoopHomeDir = new File(hadoopHome);

    // Hadoop common JARs
    addJarsFromDir(cp, new File(hadoopHomeDir, "share/hadoop/common"));
    addJarsFromDir(cp, new File(hadoopHomeDir, "share/hadoop/common/lib"));

    // HDFS JARs
    addJarsFromDir(cp, new File(hadoopHomeDir, "share/hadoop/hdfs"));
    addJarsFromDir(cp, new File(hadoopHomeDir, "share/hadoop/hdfs/lib"));

    // Configuration directory
    File confDir = new File(workDir, "conf");
    cp.add(confDir.getAbsolutePath());

    return cp;
  }

  /**
   * Adds all JAR files from a directory to the classpath.
   *
   * @param classpath classpath list to add to
   * @param dir directory to scan
   */
  private void addJarsFromDir(List<String> classpath, File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      LOG.warn("Directory does not exist, skipping: {}", dir);
      return;
    }

    File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
    if (jars != null) {
      java.util.Arrays.sort(jars); // Consistent ordering
      for (File jar : jars) {
        classpath.add(jar.getAbsolutePath());
      }
      LOG.trace("Added {} JARs from {}", jars.length, dir);
    }
  }
}

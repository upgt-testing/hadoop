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
package org.apache.hadoop.hdfs.server.process.launcher;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.datanode.DataNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Entry point for launching a DataNode in a separate JVM process.
 *
 * <p>This launcher is invoked by {@link org.apache.hadoop.hdfs.server.process.DataNodeProcessManager}
 * with appropriate configuration and classpath. It:
 * <ul>
 *   <li>Parses command-line arguments</li>
 *   <li>Loads configuration from the specified directory</li>
 *   <li>Starts a DataNode instance</li>
 *   <li>Runs until shutdown signal is received</li>
 * </ul>
 *
 * <p>Command-line usage:
 * <pre>
 * java ... DataNodeProcessLauncher --config-dir /path/to/conf [--node-index 0]
 * </pre>
 */
public class DataNodeProcessLauncher {
  private static final Logger LOG =
      LoggerFactory.getLogger(DataNodeProcessLauncher.class);

  private static volatile boolean shouldRun = true;
  private static DataNode dataNode;

  /**
   * Main entry point.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    try {
      LOG.info("DataNodeProcessLauncher starting with args: {}",
          Arrays.toString(args));

      // Parse arguments
      LauncherConfig config = parseArguments(args);

      // Set up shutdown hook
      setupShutdownHook();

      // Load configuration
      Configuration conf = loadConfiguration(config.configDir);

      // Log configuration summary
      logConfigurationSummary(conf, config);

      // Start DataNode
      LOG.info("Starting DataNode with configuration from {}",
          config.configDir);

      // DataNode.instantiateDataNode() is the standard way to start a DataNode
      String[] dnArgs = {}; // Empty args for regular startup
      dataNode = DataNode.instantiateDataNode(dnArgs, conf);

      if (dataNode == null) {
        LOG.error("Failed to create DataNode");
        System.exit(1);
      }

      // Make DataNode active
      dataNode.runDatanodeDaemon();

      LOG.info("DataNode started successfully (node index: {})",
          config.nodeIndex);

      // Wait for shutdown signal
      waitForShutdown();

      LOG.info("DataNode process exiting normally");
      System.exit(0);

    } catch (Throwable t) {
      LOG.error("Fatal error in DataNode process", t);
      System.exit(1);
    }
  }

  /**
   * Parse command-line arguments.
   *
   * @param args command-line arguments
   * @return parsed configuration
   */
  private static LauncherConfig parseArguments(String[] args) {
    LauncherConfig config = new LauncherConfig();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--config-dir":
          if (i + 1 < args.length) {
            config.configDir = args[++i];
          } else {
            throw new IllegalArgumentException(
                "--config-dir requires a directory path");
          }
          break;

        case "--node-index":
          if (i + 1 < args.length) {
            config.nodeIndex = Integer.parseInt(args[++i]);
          } else {
            throw new IllegalArgumentException(
                "--node-index requires an integer");
          }
          break;

        case "--help":
        case "-h":
          printUsage();
          System.exit(0);
          break;

        default:
          throw new IllegalArgumentException("Unknown argument: " + args[i]);
      }
    }

    // Validate required arguments
    if (config.configDir == null) {
      throw new IllegalArgumentException(
          "Missing required argument: --config-dir");
    }

    return config;
  }

  /**
   * Load configuration from the specified directory.
   *
   * @param configDir configuration directory path
   * @return loaded configuration
   */
  private static Configuration loadConfiguration(String configDir) {
    File confDir = new File(configDir);
    if (!confDir.exists() || !confDir.isDirectory()) {
      throw new IllegalArgumentException(
          "Configuration directory does not exist: " + configDir);
    }

    // Use HdfsConfiguration to automatically load hdfs-default.xml, hdfs-site.xml
    // and other default resources (including test resources if on classpath)
    Configuration conf = new HdfsConfiguration();

    // Load core-site.xml
    File coreSite = new File(confDir, "core-site.xml");
    if (coreSite.exists()) {
      LOG.info("Loading configuration from {}", coreSite);
      conf.addResource(coreSite.toURI().toString());
    }

    // Load hdfs-site.xml
    File hdfsSite = new File(confDir, "hdfs-site.xml");
    if (hdfsSite.exists()) {
      LOG.info("Loading configuration from {}", hdfsSite);
      conf.addResource(hdfsSite.toURI().toString());
    }

    // Load any other XML files in conf directory (e.g., hadoop-policy.xml)
    File[] xmlFiles = confDir.listFiles((dir, name) ->
        name.endsWith(".xml") &&
        !name.equals("core-site.xml") &&
        !name.equals("hdfs-site.xml") &&
        !name.equals("configuration.xsl"));

    if (xmlFiles != null) {
      for (File xmlFile : xmlFiles) {
        LOG.info("Loading configuration from {}", xmlFile);
        conf.addResource(xmlFile.toURI().toString());
      }
    }

    return conf;
  }

  /**
   * Log a summary of the configuration.
   *
   * @param conf configuration
   * @param config launcher config
   */
  private static void logConfigurationSummary(Configuration conf,
                                               LauncherConfig config) {
    LOG.info("Configuration summary:");
    LOG.info("  Config directory: {}", config.configDir);
    LOG.info("  Node index: {}", config.nodeIndex);
    LOG.info("  DataNode data address: {}",
        conf.get("dfs.datanode.address", "not set"));
    LOG.info("  DataNode IPC address: {}",
        conf.get("dfs.datanode.ipc.address", "not set"));
    LOG.info("  DataNode HTTP address: {}",
        conf.get("dfs.datanode.http.address", "not set"));
    LOG.info("  DataNode data dir: {}",
        conf.get("dfs.datanode.data.dir", "not set"));
    LOG.info("  NameNode address: {}",
        conf.get("dfs.namenode.rpc-address", "not set"));
  }

  /**
   * Set up shutdown hook to gracefully stop DataNode.
   */
  private static void setupShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("Shutdown hook triggered, stopping DataNode");
      shouldRun = false;

      if (dataNode != null) {
        try {
          dataNode.shutdown();
          LOG.info("DataNode stopped gracefully");
        } catch (Exception e) {
          LOG.error("Error stopping DataNode", e);
        }
      }
    }, "DataNode-shutdown-hook"));
  }

  /**
   * Wait for shutdown signal.
   * This method blocks until shouldRun becomes false.
   */
  private static void waitForShutdown() {
    LOG.info("DataNode running, waiting for shutdown signal");

    while (shouldRun) {
      try {
        Thread.sleep(1000);

        // Check if DataNode is still alive
        if (dataNode != null && !dataNode.isDatanodeUp()) {
          LOG.warn("DataNode died unexpectedly");
          break;
        }

      } catch (InterruptedException e) {
        LOG.info("Interrupted, shutting down");
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /**
   * Print usage information.
   */
  private static void printUsage() {
    System.out.println("Usage: DataNodeProcessLauncher [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --config-dir <dir>    Configuration directory (required)");
    System.out.println("  --node-index <num>    Node index number (optional, default: 0)");
    System.out.println("  --help, -h            Show this help message");
    System.out.println();
    System.out.println("Example:");
    System.out.println("  java ... DataNodeProcessLauncher \\");
    System.out.println("    --config-dir /tmp/minicluster/dn0/conf \\");
    System.out.println("    --node-index 0");
  }

  /**
   * Configuration parsed from command-line arguments.
   */
  private static class LauncherConfig {
    String configDir;
    int nodeIndex = 0;
  }
}

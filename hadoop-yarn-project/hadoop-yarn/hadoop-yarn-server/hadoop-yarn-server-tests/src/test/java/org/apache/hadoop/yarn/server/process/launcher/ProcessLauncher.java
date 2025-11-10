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

package org.apache.hadoop.yarn.server.process.launcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for YARN process launchers. This class provides common
 * functionality for parsing command-line arguments, loading configuration,
 * and setting up the environment for YARN node processes.
 *
 * <p>Subclasses must implement {@link #launchNode(YarnConfiguration, Map)}
 * to actually start the YARN node (ResourceManager or NodeManager).</p>
 *
 * <p>The launcher expects the following command-line arguments:</p>
 * <ul>
 *   <li>--config-dir: Directory containing yarn-site.xml and core-site.xml</li>
 *   <li>Node-specific arguments (defined by subclasses)</li>
 * </ul>
 *
 * @see ResourceManagerProcessLauncher
 * @see NodeManagerProcessLauncher
 */
public abstract class ProcessLauncher {

  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessLauncher.class);

  /** Key for config directory argument */
  protected static final String ARG_CONFIG_DIR = "--config-dir";

  /**
   * Parses command-line arguments into a map.
   *
   * <p>Arguments are expected in the format: --key value --key2 value2</p>
   * <p>Boolean flags can be specified without a value: --flag</p>
   *
   * @param args Command-line arguments
   * @return Map of argument keys to values
   */
  protected static Map<String, String> parseArguments(String[] args) {
    Map<String, String> parsedArgs = new HashMap<>();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.startsWith("--")) {
        String key = arg;
        String value = "true"; // Default for boolean flags

        // Check if there's a value following this argument
        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
          value = args[i + 1];
          i++; // Skip the next argument since we've consumed it
        }

        parsedArgs.put(key, value);
      } else {
        LOG.warn("Ignoring unexpected argument: {}", arg);
      }
    }

    return parsedArgs;
  }

  /**
   * Loads YARN configuration from the specified directory.
   *
   * <p>This method looks for yarn-site.xml and core-site.xml in the
   * config directory and adds them to the configuration.</p>
   *
   * @param configDir Directory containing configuration files
   * @return Loaded YarnConfiguration
   * @throws IOException if configuration files cannot be loaded
   */
  protected static YarnConfiguration loadConfiguration(File configDir)
      throws IOException {
    if (!configDir.exists() || !configDir.isDirectory()) {
      throw new IOException(
          "Configuration directory does not exist: " + configDir);
    }

    YarnConfiguration conf = new YarnConfiguration();

    // Load core-site.xml
    File coreSite = new File(configDir, "core-site.xml");
    if (coreSite.exists()) {
      LOG.info("Loading configuration from: {}", coreSite);
      conf.addResource(coreSite.toURI().toURL());
    } else {
      LOG.warn("core-site.xml not found in: {}", configDir);
    }

    // Load yarn-site.xml
    File yarnSite = new File(configDir, "yarn-site.xml");
    if (yarnSite.exists()) {
      LOG.info("Loading configuration from: {}", yarnSite);
      conf.addResource(yarnSite.toURI().toURL());
    } else {
      LOG.warn("yarn-site.xml not found in: {}", configDir);
    }

    return conf;
  }

  /**
   * Sets up signal handlers for graceful shutdown.
   *
   * <p>This registers a shutdown hook that will be called when the JVM
   * receives a SIGTERM or SIGINT signal.</p>
   *
   * @param shutdownCallback Callback to execute on shutdown
   */
  protected static void setupSignalHandlers(Runnable shutdownCallback) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("Received shutdown signal, stopping node...");
      try {
        shutdownCallback.run();
      } catch (Exception e) {
        LOG.error("Error during shutdown", e);
      }
    }, "ShutdownHook"));
  }

  /**
   * Validates that required arguments are present.
   *
   * @param args Parsed arguments
   * @param requiredArgs Required argument keys
   * @throws IllegalArgumentException if any required argument is missing
   */
  protected static void validateArguments(Map<String, String> args,
      String... requiredArgs) {
    for (String requiredArg : requiredArgs) {
      if (!args.containsKey(requiredArg)) {
        throw new IllegalArgumentException(
            "Missing required argument: " + requiredArg);
      }
    }
  }

  /**
   * Gets the value of an argument, or a default value if not present.
   *
   * @param args Parsed arguments
   * @param key Argument key
   * @param defaultValue Default value if key is not present
   * @return The argument value or default
   */
  protected static String getArgument(Map<String, String> args, String key,
      String defaultValue) {
    return args.getOrDefault(key, defaultValue);
  }

  /**
   * Gets the value of a boolean argument.
   *
   * @param args Parsed arguments
   * @param key Argument key
   * @return true if the argument is present and equals "true", false otherwise
   */
  protected static boolean getBooleanArgument(Map<String, String> args,
      String key) {
    return "true".equalsIgnoreCase(args.get(key));
  }

  /**
   * Gets the value of an integer argument.
   *
   * @param args Parsed arguments
   * @param key Argument key
   * @param defaultValue Default value if key is not present or invalid
   * @return The argument value as an integer or default
   */
  protected static int getIntArgument(Map<String, String> args, String key,
      int defaultValue) {
    String value = args.get(key);
    if (value == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      LOG.warn("Invalid integer value for {}: {}, using default: {}",
          key, value, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Launches the YARN node (ResourceManager or NodeManager).
   *
   * <p>This method should create and start the node, then block until
   * the node is shutdown.</p>
   *
   * @param conf Configuration for the node
   * @param args Parsed command-line arguments
   * @throws Exception if the node fails to start or run
   */
  protected abstract void launchNode(YarnConfiguration conf,
      Map<String, String> args) throws Exception;

  /**
   * Main entry point for the launcher. This method:
   * <ol>
   *   <li>Parses command-line arguments</li>
   *   <li>Loads configuration</li>
   *   <li>Launches the YARN node</li>
   *   <li>Waits for shutdown</li>
   * </ol>
   *
   * @param args Command-line arguments
   */
  protected void run(String[] args) {
    try {
      // Parse arguments
      LOG.info("Parsing command-line arguments");
      Map<String, String> parsedArgs = parseArguments(args);

      // Validate required arguments
      validateArguments(parsedArgs, ARG_CONFIG_DIR);

      // Load configuration
      String configDirPath = parsedArgs.get(ARG_CONFIG_DIR);
      File configDir = new File(configDirPath);
      LOG.info("Loading configuration from: {}", configDir);
      YarnConfiguration conf = loadConfiguration(configDir);

      // Launch the node
      LOG.info("Launching YARN node");
      launchNode(conf, parsedArgs);

      LOG.info("YARN node launcher exiting normally");
    } catch (Exception e) {
      LOG.error("Fatal error in YARN node launcher", e);
      System.exit(1);
    }
  }
}

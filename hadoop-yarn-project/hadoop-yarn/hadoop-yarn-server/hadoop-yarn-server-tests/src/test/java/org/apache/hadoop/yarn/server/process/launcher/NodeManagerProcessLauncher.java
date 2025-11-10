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

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process launcher for NodeManager. This class is the main entry point
 * for NodeManager processes started by ProcessBasedMiniYARNCluster.
 *
 * <p>Usage:</p>
 * <pre>
 * java -cp ... org.apache.hadoop.yarn.server.process.launcher.NodeManagerProcessLauncher \
 *   --config-dir /tmp/nm0/conf \
 *   --nm-index 0
 * </pre>
 *
 * <p>Command-line arguments:</p>
 * <ul>
 *   <li>--config-dir: Directory containing yarn-site.xml and core-site.xml</li>
 *   <li>--nm-index: Index of this NM (0, 1, 2, ...)</li>
 * </ul>
 *
 * <p>The launcher:</p>
 * <ol>
 *   <li>Parses command-line arguments</li>
 *   <li>Loads configuration from the config directory</li>
 *   <li>Creates and starts a NodeManager instance</li>
 *   <li>Waits indefinitely until shutdown signal</li>
 *   <li>Stops the NodeManager gracefully on shutdown</li>
 * </ol>
 *
 * @see ProcessLauncher
 * @see ResourceManagerProcessLauncher
 */
public class NodeManagerProcessLauncher extends ProcessLauncher {

  private static final Logger LOG =
      LoggerFactory.getLogger(NodeManagerProcessLauncher.class);

  /** Argument key for NM index */
  private static final String ARG_NM_INDEX = "--nm-index";

  /** The NodeManager instance */
  private NodeManager nodeManager;

  /** Latch to keep the process running until shutdown */
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  @Override
  protected void launchNode(YarnConfiguration conf, Map<String, String> args)
      throws Exception {
    // Validate arguments
    validateArguments(args, ARG_NM_INDEX);

    // Parse arguments
    int nmIndex = getIntArgument(args, ARG_NM_INDEX, 0);

    LOG.info("Starting NodeManager: index={}", nmIndex);

    // Create NodeManager instance
    nodeManager = new NodeManager();

    // Setup shutdown hook
    setupSignalHandlers(() -> {
      try {
        LOG.info("Shutting down NodeManager {}", nmIndex);
        shutdownLatch.countDown();
        if (nodeManager != null) {
          nodeManager.stop();
          LOG.info("NodeManager {} stopped", nmIndex);
        }
      } catch (Exception e) {
        LOG.error("Error stopping NodeManager {}", nmIndex, e);
      }
    });

    // Initialize and start NodeManager
    try {
      LOG.info("Initializing NodeManager {}", nmIndex);
      nodeManager.init(conf);

      LOG.info("Starting NodeManager {}", nmIndex);
      nodeManager.start();

      LOG.info("NodeManager {} started successfully", nmIndex);

      // Wait for shutdown signal
      LOG.info("NodeManager {} is running, waiting for shutdown signal",
          nmIndex);
      shutdownLatch.await();

      LOG.info("NodeManager {} shutdown complete", nmIndex);
    } catch (Exception e) {
      LOG.error("Failed to start or run NodeManager {}", nmIndex, e);
      throw e;
    }
  }

  /**
   * Main entry point for NodeManager process.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    LOG.info("NodeManagerProcessLauncher starting with args: {}",
        String.join(" ", args));

    NodeManagerProcessLauncher launcher = new NodeManagerProcessLauncher();
    launcher.run(args);
  }
}

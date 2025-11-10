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
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process launcher for ResourceManager. This class is the main entry point
 * for ResourceManager processes started by ProcessBasedMiniYARNCluster.
 *
 * <p>Usage:</p>
 * <pre>
 * java -cp ... org.apache.hadoop.yarn.server.process.launcher.ResourceManagerProcessLauncher \
 *   --config-dir /tmp/rm0/conf \
 *   --rm-index 0 \
 *   [--ha-enabled] \
 *   [--rm-id rm1]
 * </pre>
 *
 * <p>Command-line arguments:</p>
 * <ul>
 *   <li>--config-dir: Directory containing yarn-site.xml and core-site.xml</li>
 *   <li>--rm-index: Index of this RM (0, 1, 2, ...)</li>
 *   <li>--ha-enabled: (Optional) Whether HA is enabled</li>
 *   <li>--rm-id: (Optional) RM ID in HA mode (e.g., "rm1", "rm2")</li>
 * </ul>
 *
 * <p>The launcher:</p>
 * <ol>
 *   <li>Parses command-line arguments</li>
 *   <li>Loads configuration from the config directory</li>
 *   <li>Creates and starts a ResourceManager instance</li>
 *   <li>Waits indefinitely until shutdown signal</li>
 *   <li>Stops the ResourceManager gracefully on shutdown</li>
 * </ol>
 *
 * @see ProcessLauncher
 * @see NodeManagerProcessLauncher
 */
public class ResourceManagerProcessLauncher extends ProcessLauncher {

  private static final Logger LOG =
      LoggerFactory.getLogger(ResourceManagerProcessLauncher.class);

  /** Argument key for RM index */
  private static final String ARG_RM_INDEX = "--rm-index";

  /** Argument key for HA enabled flag */
  private static final String ARG_HA_ENABLED = "--ha-enabled";

  /** Argument key for RM ID in HA mode */
  private static final String ARG_RM_ID = "--rm-id";

  /** The ResourceManager instance */
  private ResourceManager resourceManager;

  /** Latch to keep the process running until shutdown */
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  @Override
  protected void launchNode(YarnConfiguration conf, Map<String, String> args)
      throws Exception {
    // Validate arguments
    validateArguments(args, ARG_RM_INDEX);

    // Parse arguments
    int rmIndex = getIntArgument(args, ARG_RM_INDEX, 0);
    boolean haEnabled = getBooleanArgument(args, ARG_HA_ENABLED);
    String rmId = getArgument(args, ARG_RM_ID, null);

    LOG.info("Starting ResourceManager: index={}, haEnabled={}, rmId={}",
        rmIndex, haEnabled, rmId);

    // Set RM ID in configuration if HA is enabled
    if (haEnabled && rmId != null) {
      conf.set(YarnConfiguration.RM_HA_ID, rmId);
      LOG.info("Set RM HA ID to: {}", rmId);
    }

    // Create ResourceManager instance
    resourceManager = new ResourceManager();

    // Setup shutdown hook
    setupSignalHandlers(() -> {
      try {
        LOG.info("Shutting down ResourceManager {}", rmIndex);
        shutdownLatch.countDown();
        if (resourceManager != null) {
          resourceManager.stop();
          LOG.info("ResourceManager {} stopped", rmIndex);
        }
      } catch (Exception e) {
        LOG.error("Error stopping ResourceManager {}", rmIndex, e);
      }
    });

    // Initialize and start ResourceManager
    try {
      LOG.info("Initializing ResourceManager {}", rmIndex);
      resourceManager.init(conf);

      LOG.info("Starting ResourceManager {}", rmIndex);
      resourceManager.start();

      LOG.info("ResourceManager {} started successfully", rmIndex);

      // Wait for shutdown signal
      LOG.info("ResourceManager {} is running, waiting for shutdown signal",
          rmIndex);
      shutdownLatch.await();

      LOG.info("ResourceManager {} shutdown complete", rmIndex);
    } catch (Exception e) {
      LOG.error("Failed to start or run ResourceManager {}", rmIndex, e);
      throw e;
    }
  }

  /**
   * Main entry point for ResourceManager process.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    LOG.info("ResourceManagerProcessLauncher starting with args: {}",
        String.join(" ", args));

    ResourceManagerProcessLauncher launcher =
        new ResourceManagerProcessLauncher();
    launcher.run(args);
  }
}

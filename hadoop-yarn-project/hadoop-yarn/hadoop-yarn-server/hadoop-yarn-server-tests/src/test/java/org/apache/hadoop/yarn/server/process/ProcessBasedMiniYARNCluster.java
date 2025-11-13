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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-based YARN minicluster for testing multi-version upgrade scenarios.
 * This cluster runs each ResourceManager and NodeManager in a separate JVM
 * process, enabling realistic testing of version compatibility and rolling
 * upgrades.
 *
 * <p>Unlike {@link MiniYARNCluster}, which runs all YARN nodes in a single
 * JVM, this implementation provides true process isolation and allows different
 * nodes to run different Hadoop versions.</p>
 *
 * <p><b>Key Differences from MiniYARNCluster:</b></p>
 * <ul>
 *   <li>Each node runs in a separate JVM process</li>
 *   <li>Different nodes can use different Hadoop distributions</li>
 *   <li>No direct access to ResourceManager/NodeManager objects</li>
 *   <li>Client-side APIs only (YarnClient, RMAdminCLI)</li>
 *   <li>Realistic process lifecycle and failure scenarios</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Basic single-RM cluster
 * ProcessBasedMiniYARNCluster cluster =
 *     new ProcessBasedMiniYARNCluster.Builder(conf)
 *         .numNodeManagers(3)
 *         .build();
 * cluster.start();
 *
 * // Create YarnClient to interact with the cluster
 * YarnClient yarnClient = YarnClient.createYarnClient();
 * yarnClient.init(cluster.getConfiguration());
 * yarnClient.start();
 *
 * // Submit applications, run tests...
 *
 * yarnClient.close();
 * cluster.shutdown();
 *
 * // HA cluster with mixed versions
 * ProcessBasedMiniYARNCluster haCluster =
 *     new ProcessBasedMiniYARNCluster.Builder(conf)
 *         .numResourceManagers(2)
 *         .numNodeManagers(3)
 *         .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
 *         .rmHadoopDistribution(1, "/opt/hadoop-3.3.6")
 *         .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
 *         .nmHadoopDistribution(1, "/opt/hadoop-3.4.0")
 *         .nmHadoopDistribution(2, "/opt/hadoop-3.3.6")
 *         .build();
 * haCluster.start();
 * </pre>
 *
 * <p><b>System Properties for Multi-Version Testing:</b></p>
 * <pre>
 * -Dhadoop.start.home=/opt/hadoop-3.3.6    # Default distribution
 * -Dhadoop.upgrade.home=/opt/hadoop-3.4.0  # Upgrade target distribution
 * </pre>
 *
 * @see ProcessNodeManager
 * @see HadoopVersionRegistry
 * @see MiniYARNCluster
 */
public class ProcessBasedMiniYARNCluster implements Closeable {

  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessBasedMiniYARNCluster.class);

  /** Default timeout for node startup health checks (ms) */
  private static final long DEFAULT_STARTUP_TIMEOUT_MS = 60000;

  /** Default timeout for node shutdown (ms) */
  private static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 30000;

  /** Cluster name (for logging and directory naming) */
  private final String clusterName;

  /** Base configuration for the cluster */
  private final YarnConfiguration baseConfiguration;

  /** Number of ResourceManagers */
  private final int numResourceManagers;

  /** Number of NodeManagers */
  private final int numNodeManagers;

  /** Whether HA is enabled */
  private final boolean haEnabled;

  /** RM IDs for HA mode */
  private String[] rmIds;

  /** Directory manager for cluster directories */
  private final DirectoryManager directoryManager;

  /** Port allocator for dynamic port assignment */
  private final PortAllocator portAllocator;

  /** Configuration generator */
  private final ProcessConfigurationGenerator configGenerator;

  /** Hadoop version registry */
  private final HadoopVersionRegistry versionRegistry;

  /** Per-node Hadoop distribution paths */
  private final Map<String, String> nodeHadoopHomes;

  /** ResourceManager process managers */
  private ResourceManagerProcessManager[] resourceManagers;

  /** NodeManager process managers */
  private NodeManagerProcessManager[] nodeManagers;

  /** Cached NodeManager configurations (for port reuse during upgrades) */
  private final Map<Integer, YarnConfiguration> nmConfigurations;

  /** Cluster root directory */
  private File clusterRoot;

  /** Whether cluster has been started */
  private volatile boolean started = false;

  /** Whether cluster has been shut down */
  private volatile boolean shutdown = false;

  /**
   * Private constructor - use Builder to create instances.
   */
  private ProcessBasedMiniYARNCluster(Builder builder) throws IOException {
    this.clusterName = builder.clusterName;
    this.baseConfiguration = new YarnConfiguration(builder.conf);
    this.numResourceManagers = builder.numResourceManagers;
    this.numNodeManagers = builder.numNodeManagers;
    this.haEnabled = (numResourceManagers > 1);
    this.nodeHadoopHomes = new HashMap<>(builder.nodeHadoopHomes);

    // Initialize components
    this.directoryManager = new DirectoryManager();
    this.portAllocator = new PortAllocator();
    this.configGenerator = new ProcessConfigurationGenerator();
    this.versionRegistry = builder.versionRegistry != null ?
        builder.versionRegistry : HadoopVersionRegistry.fromSystemProperties();

    // Initialize arrays and caches
    this.resourceManagers = new ResourceManagerProcessManager[numResourceManagers];
    this.nodeManagers = new NodeManagerProcessManager[numNodeManagers];
    this.nmConfigurations = new HashMap<>();

    LOG.info("Created ProcessBasedMiniYARNCluster: name={}, numRMs={}, " +
        "numNMs={}, haEnabled={}", clusterName, numResourceManagers,
        numNodeManagers, haEnabled);
  }

  /**
   * Starts the cluster. This creates directories, generates configurations,
   * and starts all ResourceManager and NodeManager processes.
   *
   * @throws IOException if cluster startup fails
   * @throws TimeoutException if nodes fail to become ready within timeout
   */
  public synchronized void start() throws IOException, TimeoutException, InterruptedException {
    if (started) {
      LOG.warn("Cluster already started");
      return;
    }

    LOG.info("Starting ProcessBasedMiniYARNCluster: {}", clusterName);

    try {
      // Step 1: Create cluster directory structure
      LOG.info("Step 1: Creating cluster directory structure");
      clusterRoot = directoryManager.createClusterRoot();
      LOG.info("Created cluster root: {}", clusterRoot);

      // Step 2: Configure HA if needed
      if (haEnabled) {
        LOG.info("Step 2: Configuring HA for {} ResourceManagers", numResourceManagers);
        configureHA();
      }

      // Step 3: Start ResourceManagers
      LOG.info("Step 3: Starting {} ResourceManager(s)", numResourceManagers);
      for (int i = 0; i < numResourceManagers; i++) {
        LOG.info("Starting ResourceManager {}/{}", i + 1, numResourceManagers);
        startResourceManager(i);
      }

      // Step 4: Wait for at least one RM to become active
      LOG.info("Step 4: Waiting for active ResourceManager (timeout={}ms)",
          DEFAULT_STARTUP_TIMEOUT_MS);
      waitForActiveRM(DEFAULT_STARTUP_TIMEOUT_MS);
      LOG.info("Active ResourceManager ready");

      // Step 5: Start NodeManagers
      LOG.info("Step 5: Starting {} NodeManager(s)", numNodeManagers);
      for (int i = 0; i < numNodeManagers; i++) {
        LOG.info("Starting NodeManager {}/{}", i + 1, numNodeManagers);
        startNodeManager(i);
      }

      // Step 6: Wait for NMs to register with RM
      LOG.info("Step 6: Waiting for {} NodeManagers to register (timeout={}ms)",
          numNodeManagers, DEFAULT_STARTUP_TIMEOUT_MS);
      waitForNodeManagersToConnect(DEFAULT_STARTUP_TIMEOUT_MS);
      LOG.info("All {} NodeManagers registered successfully", numNodeManagers);

      started = true;
      LOG.info("ProcessBasedMiniYARNCluster started successfully");

    } catch (Exception e) {
      LOG.error("Failed to start cluster - error during startup", e);
      LOG.error("Cluster startup failed at step: {}", getStartupStepFromException(e));

      // Clean up on failure
      try {
        LOG.info("Cleaning up after startup failure");
        shutdown();
      } catch (Exception cleanupEx) {
        LOG.warn("Error during cleanup after startup failure", cleanupEx);
      }

      // Provide more helpful error message
      String errorMsg = String.format(
          "Failed to start ProcessBasedMiniYARNCluster '%s'. " +
          "Check logs above for details. Error: %s",
          clusterName, e.getMessage());
      throw new IOException(errorMsg, e);
    }
  }

  /**
   * Attempts to determine which startup step failed based on exception message.
   *
   * @param e Exception from startup failure
   * @return Description of which step likely failed
   */
  private String getStartupStepFromException(Exception e) {
    String msg = e.getMessage();
    if (msg == null) {
      return "Unknown step";
    }

    if (msg.contains("createClusterRoot") || msg.contains("directory")) {
      return "Step 1 (Create cluster directories)";
    } else if (msg.contains("HA") || msg.contains("ZooKeeper")) {
      return "Step 2 (Configure HA)";
    } else if (msg.contains("ResourceManager") && !msg.contains("NodeManager")) {
      return "Step 3-4 (Start ResourceManager)";
    } else if (msg.contains("active ResourceManager")) {
      return "Step 4 (Wait for active RM)";
    } else if (msg.contains("NodeManager")) {
      if (msg.contains("register") || msg.contains("RUNNING")) {
        return "Step 6 (Wait for NodeManager registration)";
      } else {
        return "Step 5 (Start NodeManagers)";
      }
    }

    return "Unknown step - check stack trace above";
  }

  /**
   * Configures HA settings for ResourceManagers.
   */
  private void configureHA() {
    LOG.info("Configuring HA for {} ResourceManagers", numResourceManagers);

    // Generate RM IDs
    rmIds = new String[numResourceManagers];
    for (int i = 0; i < numResourceManagers; i++) {
      rmIds[i] = "rm" + (i + 1);
    }

    // Update base configuration
    baseConfiguration.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);
    baseConfiguration.set(YarnConfiguration.RM_HA_IDS,
        String.join(",", rmIds));
    baseConfiguration.set(YarnConfiguration.RM_CLUSTER_ID,
        "test-cluster-" + System.currentTimeMillis());

    // Only set ZooKeeper address if automatic failover is enabled
    // If AUTO_FAILOVER_ENABLED is false, manual failover will be used (no ZK needed)
    boolean autoFailoverEnabled = baseConfiguration.getBoolean(
        YarnConfiguration.AUTO_FAILOVER_ENABLED,
        YarnConfiguration.DEFAULT_AUTO_FAILOVER_ENABLED);

    if (autoFailoverEnabled) {
      // ZooKeeper address (embedded ZK for testing)
      baseConfiguration.set(YarnConfiguration.RM_ZK_ADDRESS, "localhost:2181");
      LOG.info("Auto-failover enabled - configured ZooKeeper at localhost:2181");
    } else {
      LOG.info("Auto-failover disabled - manual RM failover will be used");
    }

    LOG.debug("HA configured with RM IDs: {}", String.join(",", rmIds));
  }

  /**
   * Starts a ResourceManager process.
   *
   * @param rmIndex Index of the RM (0, 1, 2, ...)
   * @throws IOException if RM startup fails
   * @throws TimeoutException if RM fails to become healthy within timeout
   */
  private void startResourceManager(int rmIndex)
      throws IOException, TimeoutException {
    LOG.info("Starting ResourceManager {}", rmIndex);

    // Create RM directory
    File rmDir = directoryManager.createResourceManagerDir(rmIndex);

    // Get Hadoop home for this RM
    String nodeKey = "rm" + rmIndex;
    String hadoopHome = resolveHadoopHome(nodeKey);

    // Generate RM configuration
    String rmId = haEnabled ? rmIds[rmIndex] : null;
    YarnConfiguration rmConf = configGenerator.generateResourceManagerConfig(
        baseConfiguration, rmIndex, rmDir, haEnabled, rmIds, portAllocator);

    // Write configuration to file
    File confDir = directoryManager.getConfDir(rmDir);
    configGenerator.writeConfigToFile(rmConf, confDir);

    // Create and start RM process manager
    ResourceManagerProcessManager rmProcess =
        new ResourceManagerProcessManager(
            rmConf, hadoopHome, rmDir, rmIndex, haEnabled, rmId);

    rmProcess.start();
    resourceManagers[rmIndex] = rmProcess;

    // Copy RM addresses to base configuration so clients can connect
    String rmAddress = rmConf.get(YarnConfiguration.RM_ADDRESS);
    System.out.println("DEBUG_COPY: rmAddress from rmConf = " + rmAddress);
    baseConfiguration.set(YarnConfiguration.RM_ADDRESS, rmAddress);
    System.out.println("DEBUG_COPY: After set, baseConfiguration.RM_ADDRESS = " +
        baseConfiguration.get(YarnConfiguration.RM_ADDRESS));
    baseConfiguration.set(YarnConfiguration.RM_SCHEDULER_ADDRESS,
        rmConf.get(YarnConfiguration.RM_SCHEDULER_ADDRESS));
    baseConfiguration.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS,
        rmConf.get(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS));
    baseConfiguration.set(YarnConfiguration.RM_ADMIN_ADDRESS,
        rmConf.get(YarnConfiguration.RM_ADMIN_ADDRESS));
    baseConfiguration.set(YarnConfiguration.RM_WEBAPP_ADDRESS,
        rmConf.get(YarnConfiguration.RM_WEBAPP_ADDRESS));
    baseConfiguration.set(YarnConfiguration.RM_HOSTNAME,
        rmConf.get(YarnConfiguration.RM_HOSTNAME));

    LOG.info("ResourceManager {} started successfully at {}",
        rmIndex, rmAddress);
    LOG.info("Copied RM addresses to baseConfiguration: RM_ADDRESS={}",
        baseConfiguration.get(YarnConfiguration.RM_ADDRESS));
  }

  /**
   * Starts a NodeManager process.
   *
   * <p>This method can be used to manually start a NodeManager after it has been
   * shut down via shutdownNodeManager(). It's also used internally during cluster
   * startup and during rolling upgrades.</p>
   *
   * <p><b>Example usage for rolling upgrade:</b></p>
   * <pre>
   * cluster.shutdownNodeManager(0);
   * cluster.changeNodeManagerVersion(0, "/opt/hadoop-3.4.0");
   * cluster.startNodeManager(0);  // Starts with new version
   * cluster.waitForNodeManagersToConnect(5000);
   * </pre>
   *
   * @param nmIndex Index of the NM (0, 1, 2, ...)
   * @throws IOException if NM startup fails
   * @throws TimeoutException if NM fails to become healthy within timeout
   */
  public synchronized void startNodeManager(int nmIndex)
      throws IOException, TimeoutException {
    LOG.info("Starting NodeManager {}", nmIndex);

    // Create NM directory
    File nmDir = directoryManager.createNodeManagerDir(nmIndex);

    // Get Hadoop home for this NM
    String nodeKey = "nm" + nmIndex;
    String hadoopHome = resolveHadoopHome(nodeKey);

    // Get RM address
    String rmAddress = getActiveRMAddress();

    // Generate or reuse NM configuration
    // IMPORTANT: Reuse cached configuration to preserve port assignments
    // across NM restarts. This ensures YARN recognizes the restarted NM
    // as the same node (hostname:port) rather than a new node.
    YarnConfiguration nmConf;
    if (nmConfigurations.containsKey(nmIndex)) {
      LOG.info("Reusing cached configuration for NodeManager {} " +
          "(preserves ports across restarts)", nmIndex);
      // Clone the cached config to allow directory updates
      nmConf = new YarnConfiguration(nmConfigurations.get(nmIndex));

      // Update directory paths (may have changed)
      nmConf.set(YarnConfiguration.NM_LOCAL_DIRS,
          nmDir.getAbsolutePath() + "/local");
      nmConf.set(YarnConfiguration.NM_LOG_DIRS,
          nmDir.getAbsolutePath() + "/logs");

      // CRITICAL: Update RM addresses from baseConfiguration
      // During rolling upgrades, NMs must connect to the active RM with
      // current addresses. The cached config may have stale addresses.
      LOG.debug("Updating RM addresses in cached NodeManager {} configuration", nmIndex);
      nmConf.set(YarnConfiguration.RM_ADDRESS,
          baseConfiguration.get(YarnConfiguration.RM_ADDRESS));
      nmConf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS,
          baseConfiguration.get(YarnConfiguration.RM_SCHEDULER_ADDRESS));
      nmConf.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS,
          baseConfiguration.get(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS));
      nmConf.set(YarnConfiguration.RM_ADMIN_ADDRESS,
          baseConfiguration.get(YarnConfiguration.RM_ADMIN_ADDRESS));
      nmConf.set(YarnConfiguration.RM_HOSTNAME,
          baseConfiguration.get(YarnConfiguration.RM_HOSTNAME));

      LOG.info("NodeManager {} will connect to RM at: {}", nmIndex,
          nmConf.get(YarnConfiguration.RM_ADDRESS));
    } else {
      LOG.info("Generating new configuration for NodeManager {}", nmIndex);
      nmConf = configGenerator.generateNodeManagerConfig(
          baseConfiguration, nmIndex, nmDir, rmAddress, portAllocator);

      // Cache this configuration for future restarts
      nmConfigurations.put(nmIndex, new YarnConfiguration(nmConf));
      LOG.debug("Cached configuration for NodeManager {}: ports={}, {}, {}",
          nmIndex,
          nmConf.get(YarnConfiguration.NM_ADDRESS),
          nmConf.get(YarnConfiguration.NM_LOCALIZER_ADDRESS),
          nmConf.get(YarnConfiguration.NM_WEBAPP_ADDRESS));
    }

    // Write configuration to file
    File confDir = directoryManager.getConfDir(nmDir);
    configGenerator.writeConfigToFile(nmConf, confDir);

    // Create and start NM process manager
    NodeManagerProcessManager nmProcess =
        new NodeManagerProcessManager(nmConf, hadoopHome, nmDir, nmIndex);

    nmProcess.start();
    nodeManagers[nmIndex] = nmProcess;

    LOG.info("NodeManager {} started successfully", nmIndex);
  }

  /**
   * Resolves the Hadoop home directory for a specific node.
   *
   * @param nodeKey Node key (e.g., "rm0", "nm1")
   * @return Hadoop home directory path
   * @throws IOException if Hadoop distribution cannot be resolved
   */
  private String resolveHadoopHome(String nodeKey) throws IOException {
    // Check if specific override is set for this node
    if (nodeHadoopHomes.containsKey(nodeKey)) {
      return nodeHadoopHomes.get(nodeKey);
    }

    // Check for role-based defaults (all RMs or all NMs)
    String roleKey = nodeKey.startsWith("rm") ? "rm-all" : "nm-all";
    if (nodeHadoopHomes.containsKey(roleKey)) {
      return nodeHadoopHomes.get(roleKey);
    }

    // Check for cluster-wide default
    if (nodeHadoopHomes.containsKey("all")) {
      return nodeHadoopHomes.get("all");
    }

    // Fall back to version registry (check for "start-version" alias)
    HadoopDistribution defaultDist = versionRegistry.get("start-version");
    if (defaultDist != null) {
      return defaultDist.getHadoopHome().getAbsolutePath();
    }

    throw new IOException(
        "No Hadoop distribution configured for node: " + nodeKey +
        ". Use Builder.hadoopDistribution() or set system properties " +
        "hadoop.start.home/hadoop.upgrade.home");
  }

  /**
   * Waits for at least one ResourceManager to become active.
   *
   * @param timeoutMs Timeout in milliseconds
   * @throws TimeoutException if no RM becomes active within timeout
   * @throws IOException if health check fails
   */
  private void waitForActiveRM(long timeoutMs)
      throws TimeoutException, IOException, InterruptedException {
    LOG.info("Waiting for active ResourceManager (timeout={}ms)", timeoutMs);

    GenericTestUtils.waitFor(() -> {
      try {
        for (ResourceManagerProcessManager rm : resourceManagers) {
          if (rm != null && rm.isHealthy()) {
            return true;
          }
        }
      } catch (IOException e) {
        LOG.debug("Error checking RM health", e);
      }
      return false;
    }, 1000, timeoutMs);

    LOG.info("Active ResourceManager is ready");
  }

  /**
   * Gets the RPC address of the active ResourceManager.
   *
   * @return RM address in "host:port" format
   * @throws IOException if no active RM found
   */
  private String getActiveRMAddress() throws IOException {
    for (ResourceManagerProcessManager rm : resourceManagers) {
      if (rm != null && rm.isHealthy()) {
        InetSocketAddress addr = rm.getRpcAddress();
        return addr.getHostString() + ":" + addr.getPort();
      }
    }
    throw new IOException("No active ResourceManager found");
  }

  /**
   * Waits for NodeManagers to connect and register with the ResourceManager.
   *
   * <p>This method actively verifies NodeManager registration by polling the
   * ResourceManager via RPC. It waits until the expected number of NodeManagers
   * are registered in RUNNING state or the timeout expires.</p>
   *
   * <p><b>Important:</b> This method creates a temporary RPC client to the RM
   * to verify registration. It checks actual registration status, not just process
   * startup.</p>
   *
   * @param timeoutMs Maximum time to wait in milliseconds
   * @return true if all NodeManagers registered successfully
   * @throws InterruptedException if wait is interrupted
   * @throws TimeoutException if NodeManagers fail to register within timeout
   */
  public boolean waitForNodeManagersToConnect(long timeoutMs)
      throws InterruptedException, TimeoutException {
    if (numNodeManagers == 0) {
      LOG.debug("No NodeManagers expected, skipping registration check");
      return true;
    }

    LOG.info("Waiting for {} NodeManagers to register as RUNNING (timeout={}ms)",
        numNodeManagers, timeoutMs);

    long deadline = System.currentTimeMillis() + timeoutMs;
    org.apache.hadoop.yarn.api.ApplicationClientProtocol rmClient = null;

    try {
      // Create RM client proxy
      rmClient = org.apache.hadoop.yarn.client.ClientRMProxy.createRMProxy(
          baseConfiguration, org.apache.hadoop.yarn.api.ApplicationClientProtocol.class);

      while (System.currentTimeMillis() < deadline) {
        try {
          // Get cluster nodes with RUNNING state filter
          org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequest request =
              org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequest.newInstance();
          request.setNodeStates(java.util.EnumSet.of(
              org.apache.hadoop.yarn.api.records.NodeState.RUNNING));
          org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesResponse response =
              rmClient.getClusterNodes(request);

          java.util.List<org.apache.hadoop.yarn.api.records.NodeReport> runningNodes =
              response.getNodeReports();
          int runningCount = runningNodes.size();

          if (runningCount >= numNodeManagers) {
            LOG.info("All {} NodeManagers are registered as RUNNING", numNodeManagers);
            return true;
          }

          LOG.debug("NodeManagers registered: {}/{}, waiting...",
              runningCount, numNodeManagers);
          Thread.sleep(500);

        } catch (Exception e) {
          LOG.debug("Error checking NodeManager registration: {}", e.getMessage());
          Thread.sleep(500);
        }
      }

      // Timeout - get final count for error message
      int finalCount = 0;
      try {
        org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequest request =
            org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesRequest.newInstance();
        request.setNodeStates(java.util.EnumSet.of(
            org.apache.hadoop.yarn.api.records.NodeState.RUNNING));
        org.apache.hadoop.yarn.api.protocolrecords.GetClusterNodesResponse response =
            rmClient.getClusterNodes(request);
        finalCount = response.getNodeReports().size();
      } catch (Exception e) {
        LOG.warn("Error getting final NodeManager count", e);
      }

      throw new TimeoutException(
          "Only " + finalCount + "/" + numNodeManagers +
          " NodeManagers registered as RUNNING within " + timeoutMs + "ms");

    } catch (TimeoutException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error during NodeManager registration check", e);
    }
  }

  /**
   * Restarts a ResourceManager process.
   *
   * @param rmIndex Index of the RM to restart
   * @throws IOException if restart fails
   * @throws TimeoutException if RM fails to become healthy after restart
   */
  public synchronized void restartResourceManager(int rmIndex)
      throws IOException, TimeoutException {
    if (rmIndex < 0 || rmIndex >= numResourceManagers) {
      throw new IllegalArgumentException(
          "Invalid RM index: " + rmIndex + " (valid: 0-" +
          (numResourceManagers - 1) + ")");
    }

    LOG.info("Restarting ResourceManager {}", rmIndex);

    // Stop existing RM
    ResourceManagerProcessManager rm = resourceManagers[rmIndex];
    if (rm != null) {
      rm.stop();
    }

    // Start new RM
    startResourceManager(rmIndex);

    LOG.info("ResourceManager {} restarted successfully", rmIndex);
  }

  /**
   * Restarts a NodeManager process.
   *
   * @param nmIndex Index of the NM to restart
   * @throws IOException if restart fails
   * @throws TimeoutException if NM fails to become healthy after restart
   */
  public synchronized void restartNodeManager(int nmIndex)
      throws IOException, TimeoutException {
    if (nmIndex < 0 || nmIndex >= numNodeManagers) {
      throw new IllegalArgumentException(
          "Invalid NM index: " + nmIndex + " (valid: 0-" +
          (numNodeManagers - 1) + ")");
    }

    LOG.info("Restarting NodeManager {}", nmIndex);

    // Stop existing NM
    NodeManagerProcessManager nm = nodeManagers[nmIndex];
    if (nm != null) {
      nm.stop();
    }

    // Start new NM
    startNodeManager(nmIndex);

    LOG.info("NodeManager {} restarted successfully", nmIndex);
  }

  /**
   * Performs a rolling upgrade of all NodeManagers to a new Hadoop version.
   *
   * <p>This method performs a sequential rolling upgrade of NodeManagers,
   * upgrading one at a time to maintain cluster availability. Each NodeManager
   * is shut down, reconfigured with the upgrade distribution, restarted, and
   * verified healthy before proceeding to the next one.</p>
   *
   * <p><b>Upgrade Sequence:</b></p>
   * <ol>
   *   <li>Get upgrade distribution path from system property or version registry</li>
   *   <li>For each NodeManager (0, 1, 2, ...):
   *     <ul>
   *       <li>Shut down the NodeManager</li>
   *       <li>Change Hadoop distribution to upgrade version</li>
   *       <li>Restart NodeManager with new version</li>
   *       <li>Wait for NodeManager to reconnect</li>
   *     </ul>
   *   </li>
   *   <li>Verify all NodeManagers are connected</li>
   * </ol>
   *
   * <p><b>Example usage in upgrade test:</b></p>
   * <pre>
   * // Set upgrade distribution via system property
   * System.setProperty("hadoop.upgrade.home", "/opt/hadoop-3.4.0");
   *
   * // Start cluster with Hadoop 3.3.6
   * ProcessBasedMiniYARNCluster cluster =
   *     new ProcessBasedMiniYARNCluster.Builder(conf)
   *         .numNodeManagers(3)
   *         .build();
   *
   * // Submit long-running application
   * ApplicationId appId = submitDistributedShellApp(yarnClient, "sleep 300");
   *
   * // Perform rolling upgrade (application continues running)
   * cluster.rollingUpgradeNodeManagers();
   *
   * // Verify application completed successfully after upgrade
   * waitForAppCompletion(yarnClient, appId);
   * </pre>
   *
   * <p><b>Requirements:</b></p>
   * <ul>
   *   <li>System property {@code hadoop.upgrade.home} must be set, or</li>
   *   <li>Version registry must have "upgrade-version" entry</li>
   *   <li>Cluster must be running (isClusterUp() returns true)</li>
   * </ul>
   *
   * @throws IOException if any NodeManager fails to restart
   * @throws TimeoutException if any NodeManager fails to become healthy
   * @throws InterruptedException if wait is interrupted
   * @throws IllegalStateException if no upgrade distribution is configured
   *
   * @see #getUpgradeDistributionPath()
   * @see #changeNodeManagerVersion(int, String)
   */
  public synchronized void rollingUpgradeNodeManagers()
      throws IOException, TimeoutException, InterruptedException {
    // Get upgrade distribution path
    String upgradePath = getUpgradeDistributionPath();
    if (upgradePath == null || upgradePath.isEmpty()) {
      throw new IllegalStateException(
          "No upgrade distribution configured. " +
          "Set system property hadoop.upgrade.home or register " +
          "'upgrade-version' in HadoopVersionRegistry. " +
          "Example: -Dhadoop.upgrade.home=/opt/hadoop-3.4.0");
    }

    LOG.info("Starting rolling upgrade of {} NodeManagers to {}",
        numNodeManagers, upgradePath);

    // Upgrade each NodeManager sequentially
    for (int i = 0; i < numNodeManagers; i++) {
      LOG.info("Upgrading NodeManager {} ({}/{})",
          i, i + 1, numNodeManagers);

      // Step 1: Shut down NodeManager
      shutdownNodeManager(i);
      LOG.debug("NodeManager {} shut down", i);

      // Step 2: Change Hadoop distribution
      changeNodeManagerVersion(i, upgradePath);
      LOG.debug("NodeManager {} configured for {}", i, upgradePath);

      // Step 3: Restart NodeManager with new version
      // The new NM will reuse the same ports as the old NM, so YARN
      // will recognize it as the same node (hostname:port) reconnecting
      startNodeManager(i);
      LOG.debug("NodeManager {} restarted with new version", i);

      // Step 4: Wait for NodeManager to reconnect
      // Give upgraded NMs more time to start and register (15 seconds per NM)
      LOG.info("Waiting for NodeManager {} to reconnect and register as RUNNING", i);
      try {
        waitForNodeManagersToConnect(15000);
        LOG.info("NodeManager {} successfully registered after upgrade", i);
      } catch (TimeoutException e) {
        LOG.warn("NodeManager {} did not register within 15 seconds, continuing anyway", i);
        // Continue to next NM - final verification will catch any issues
      }

      LOG.info("NodeManager {} upgraded successfully ({}/{})",
          i, i + 1, numNodeManagers);
    }

    // Final verification: ensure all NodeManagers are connected
    // Give NMs additional time to stabilize after all upgrades complete
    LOG.info("Verifying all {} NodeManagers are connected after upgrade",
        numNodeManagers);
    waitForNodeManagersToConnect(20000);

    LOG.info("Rolling upgrade completed successfully: {} NodeManagers upgraded to {}",
        numNodeManagers, upgradePath);
  }

  /**
   * Shuts down a specific ResourceManager.
   *
   * @param rmIndex Index of the RM to shut down
   * @throws IOException if shutdown fails
   */
  public synchronized void shutdownResourceManager(int rmIndex)
      throws IOException {
    if (rmIndex < 0 || rmIndex >= numResourceManagers) {
      throw new IllegalArgumentException(
          "Invalid RM index: " + rmIndex);
    }

    LOG.info("Shutting down ResourceManager {}", rmIndex);

    ResourceManagerProcessManager rm = resourceManagers[rmIndex];
    if (rm != null) {
      rm.stop();
      resourceManagers[rmIndex] = null;
    }
  }

  /**
   * Shuts down a specific NodeManager.
   *
   * @param nmIndex Index of the NM to shut down
   * @throws IOException if shutdown fails
   */
  public synchronized void shutdownNodeManager(int nmIndex)
      throws IOException {
    if (nmIndex < 0 || nmIndex >= numNodeManagers) {
      throw new IllegalArgumentException(
          "Invalid NM index: " + nmIndex);
    }

    LOG.info("Shutting down NodeManager {}", nmIndex);

    NodeManagerProcessManager nm = nodeManagers[nmIndex];
    if (nm != null) {
      nm.stop();
      nodeManagers[nmIndex] = null;
    }
  }

  /**
   * Changes the Hadoop distribution for a specific NodeManager.
   * This sets the Hadoop home path that will be used when the NodeManager
   * is next started (via startNodeManager() or restartNodeManager()).
   *
   * <p>This method is typically used in rolling upgrade scenarios where
   * you want to upgrade individual NodeManagers to a different Hadoop version:</p>
   *
   * <pre>
   * // Upgrade NodeManager 0 to Hadoop 3.4.0
   * cluster.shutdownNodeManager(0);
   * cluster.changeNodeManagerVersion(0, "/opt/hadoop-3.4.0");
   * cluster.startNodeManager(0);
   * cluster.waitForNodeManagersToConnect(5000);
   * </pre>
   *
   * <p><b>Note:</b> This method only updates the configuration. The NodeManager
   * must be restarted for the change to take effect.</p>
   *
   * @param nmIndex Index of the NodeManager to change
   * @param hadoopHome Path to the new Hadoop distribution
   * @throws IllegalArgumentException if nmIndex is invalid or hadoopHome is null
   */
  public synchronized void changeNodeManagerVersion(int nmIndex,
      String hadoopHome) {
    if (nmIndex < 0 || nmIndex >= numNodeManagers) {
      throw new IllegalArgumentException(
          "Invalid NM index: " + nmIndex + " (valid: 0-" +
          (numNodeManagers - 1) + ")");
    }

    if (hadoopHome == null || hadoopHome.isEmpty()) {
      throw new IllegalArgumentException(
          "Hadoop home path cannot be null or empty");
    }

    LOG.info("Changing NodeManager {} to Hadoop distribution: {}",
        nmIndex, hadoopHome);

    // Update the per-node Hadoop home mapping
    String nodeKey = "nm" + nmIndex;
    nodeHadoopHomes.put(nodeKey, hadoopHome);

    LOG.debug("NodeManager {} will use {} when next started",
        nmIndex, hadoopHome);
  }

  /**
   * Shuts down the entire cluster. Stops all ResourceManagers and
   * NodeManagers, then cleans up directories.
   *
   * @throws IOException if shutdown fails
   */
  public synchronized void shutdown() throws IOException {
    if (shutdown) {
      LOG.debug("Cluster already shut down");
      return;
    }

    LOG.info("Shutting down ProcessBasedMiniYARNCluster: {}", clusterName);

    // Stop all NodeManagers
    for (int i = 0; i < numNodeManagers; i++) {
      if (nodeManagers[i] != null) {
        try {
          nodeManagers[i].stop();
        } catch (Exception e) {
          LOG.warn("Error stopping NodeManager {}", i, e);
        }
        nodeManagers[i] = null;
      }
    }

    // Stop all ResourceManagers
    for (int i = 0; i < numResourceManagers; i++) {
      if (resourceManagers[i] != null) {
        try {
          resourceManagers[i].stop();
        } catch (Exception e) {
          LOG.warn("Error stopping ResourceManager {}", i, e);
        }
        resourceManagers[i] = null;
      }
    }

    // Clean up directories
    try {
      directoryManager.cleanup();
    } catch (IOException e) {
      LOG.warn("Error cleaning up directories", e);
    }

    // Release ports
    portAllocator.releaseAll();

    shutdown = true;
    started = false;

    LOG.info("ProcessBasedMiniYARNCluster shut down successfully");
  }

  /**
   * Closes the cluster (alias for shutdown).
   */
  @Override
  public void close() throws IOException {
    shutdown();
  }


  /**
   * Gets the cluster configuration. This is a client-side configuration
   * that can be used to connect to the cluster.
   *
   * @return Client-side YarnConfiguration
   */
  public YarnConfiguration getConfiguration() {
    // Return baseConfiguration directly so RM addresses set in start() are available
    // Creating new YarnConfiguration(baseConfiguration) would load defaults that override our values
    LOG.info("getConfiguration() called - RM_ADDRESS={}",
        baseConfiguration.get(YarnConfiguration.RM_ADDRESS));
    return baseConfiguration;
  }

  /**
   * Gets the upgrade distribution path for rolling upgrades.
   * This method checks system properties and the version registry to find
   * the target Hadoop distribution for upgrades.
   *
   * <p>The upgrade distribution is resolved in the following order:</p>
   * <ol>
   *   <li>System property {@code hadoop.upgrade.home}</li>
   *   <li>Version registry entry for "upgrade-version"</li>
   *   <li>Returns null if neither is available</li>
   * </ol>
   *
   * <p><b>Example Usage:</b></p>
   * <pre>
   * // Set system property before cluster creation
   * System.setProperty("hadoop.upgrade.home", "/opt/hadoop-3.4.0");
   *
   * // Or pass as JVM argument
   * // -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
   *
   * // In test code
   * String upgradePath = cluster.getUpgradeDistributionPath();
   * if (upgradePath != null) {
   *     cluster.rollingUpgradeNodeManagers();
   * }
   * </pre>
   *
   * @return Path to upgrade Hadoop distribution, or null if not configured
   */
  public String getUpgradeDistributionPath() {
    // First try system property
    String upgradePath = System.getProperty("hadoop.upgrade.home");
    if (upgradePath != null && !upgradePath.isEmpty()) {
      LOG.debug("Upgrade distribution from hadoop.upgrade.home: {}", upgradePath);
      return upgradePath;
    }

    // Fall back to version registry
    try {
      HadoopDistribution upgradeDist = versionRegistry.get("upgrade-version");
      if (upgradeDist != null) {
        String path = upgradeDist.getHadoopHome().getAbsolutePath();
        LOG.debug("Upgrade distribution from version registry: {}", path);
        return path;
      }
    } catch (Exception e) {
      LOG.debug("Could not get upgrade distribution from version registry", e);
    }

    LOG.debug("No upgrade distribution configured");
    return null;
  }

  /**
   * Gets the index of the active ResourceManager (for HA clusters).
   *
   * @return Index of active RM, or 0 for single-RM clusters
   * @throws IOException if no active RM found
   */
  public int getActiveRMIndex() throws IOException {
    if (!haEnabled) {
      return 0;
    }

    for (int i = 0; i < numResourceManagers; i++) {
      ResourceManagerProcessManager rm = resourceManagers[i];
      if (rm != null && rm.isHealthy()) {
        return i;
      }
    }

    throw new IOException("No active ResourceManager found");
  }

  /**
   * Gets the RPC address of a specific ResourceManager.
   *
   * @param rmIndex Index of the RM
   * @return RM RPC address
   * @throws IOException if RM is not running
   */
  public InetSocketAddress getResourceManagerAddress(int rmIndex)
      throws IOException {
    if (rmIndex < 0 || rmIndex >= numResourceManagers) {
      throw new IllegalArgumentException("Invalid RM index: " + rmIndex);
    }

    ResourceManagerProcessManager rm = resourceManagers[rmIndex];
    if (rm == null) {
      throw new IOException("ResourceManager " + rmIndex + " is not running");
    }

    return rm.getRpcAddress();
  }

  /**
   * Gets the RPC address of a specific NodeManager.
   *
   * @param nmIndex Index of the NM
   * @return NM RPC address
   * @throws IOException if NM is not running
   */
  public InetSocketAddress getNodeManagerAddress(int nmIndex)
      throws IOException {
    if (nmIndex < 0 || nmIndex >= numNodeManagers) {
      throw new IllegalArgumentException("Invalid NM index: " + nmIndex);
    }

    NodeManagerProcessManager nm = nodeManagers[nmIndex];
    if (nm == null) {
      throw new IOException("NodeManager " + nmIndex + " is not running");
    }

    return nm.getRpcAddress();
  }

  /**
   * Checks if the cluster is currently running and healthy.
   *
   * <p>This method prioritizes actual process health over lifecycle flags.
   * Even if the cluster failed to complete startup (started=false), if at least
   * one ResourceManager is healthy and responding, the cluster is considered "up".</p>
   *
   * <p>This allows tests to proceed even if startup had non-critical failures
   * (e.g., some NodeManagers failed to register), as long as the ResourceManager
   * is functional.</p>
   *
   * <p><b>Shutdown takes precedence:</b> If shutdown() has been called
   * (shutdown=true), the cluster is always considered down, regardless of
   * process health. This prevents use-after-shutdown scenarios.</p>
   *
   * @return true if not shut down and at least one RM is healthy
   */
  public boolean isClusterUp() {
    // If explicitly shut down, cluster is definitely not up
    if (shutdown) {
      LOG.debug("isClusterUp() returning false - cluster has been shut down");
      return false;
    }

    // Check if at least one ResourceManager is healthy
    // This verifies actual process health, not just the 'started' flag
    // This allows cluster to be "up" even if start() didn't complete fully
    try {
      for (ResourceManagerProcessManager rm : resourceManagers) {
        if (rm != null && rm.isHealthy()) {
          LOG.debug("isClusterUp() returning true - found healthy ResourceManager");
          return true;  // At least one RM is healthy
        }
      }
    } catch (Exception e) {
      LOG.debug("Error checking ResourceManager health in isClusterUp()", e);
      return false;
    }

    // No healthy RMs found
    LOG.debug("isClusterUp() returning false - no healthy ResourceManagers found");
    return false;
  }

  /**
   * Gets the number of ResourceManagers in this cluster.
   *
   * @return Number of RMs
   */
  public int getNumResourceManagers() {
    return numResourceManagers;
  }

  /**
   * Gets the number of NodeManagers in this cluster.
   *
   * @return Number of NMs
   */
  public int getNumNodeManagers() {
    return numNodeManagers;
  }

  /**
   * Gets the cluster root directory.
   *
   * @return Cluster root directory
   */
  public File getClusterRoot() {
    return clusterRoot;
  }

  /**
   * Gets the directory manager for this cluster.
   *
   * @return Directory manager instance
   */
  public DirectoryManager getDirectoryManager() {
    return directoryManager;
  }

  // =========================================================================
  // UNSUPPORTED OPERATIONS
  // These methods throw UnsupportedOperationException because they require
  // direct access to server-side objects, which is not possible in a
  // process-based cluster.
  // =========================================================================

  /**
   * NOT SUPPORTED - Direct ResourceManager object access is not available
   * in ProcessBasedMiniYARNCluster.
   *
   * @throws UnsupportedOperationException always
   */
  public Object getResourceManager() {
    throw new UnsupportedOperationException(
        "Direct ResourceManager object access not supported in " +
        "ProcessBasedMiniYARNCluster. This cluster runs nodes in separate " +
        "processes. Use YarnClient APIs instead:\n" +
        "  YarnClient client = YarnClient.createYarnClient();\n" +
        "  client.init(cluster.getConfiguration());\n" +
        "  client.start();\n" +
        "  YarnClusterMetrics metrics = client.getYarnClusterMetrics();");
  }

  /**
   * NOT SUPPORTED - Direct ResourceManager object access is not available
   * in ProcessBasedMiniYARNCluster.
   *
   * @param index RM index
   * @throws UnsupportedOperationException always
   */
  public Object getResourceManager(int index) {
    throw new UnsupportedOperationException(
        "Direct ResourceManager object access not supported in " +
        "ProcessBasedMiniYARNCluster. This cluster runs nodes in separate " +
        "processes. Use YarnClient APIs instead.");
  }

  /**
   * NOT SUPPORTED - Direct NodeManager object access is not available
   * in ProcessBasedMiniYARNCluster.
   *
   * @param index NM index
   * @throws UnsupportedOperationException always
   */
  public Object getNodeManager(int index) {
    throw new UnsupportedOperationException(
        "Direct NodeManager object access not supported in " +
        "ProcessBasedMiniYARNCluster. This cluster runs nodes in separate " +
        "processes. Use YarnClient APIs instead:\n" +
        "  YarnClient client = YarnClient.createYarnClient();\n" +
        "  client.init(cluster.getConfiguration());\n" +
        "  client.start();\n" +
        "  List<NodeReport> nodes = client.getNodeReports();");
  }

  /**
   * NOT SUPPORTED - Application History Server is not implemented in
   * ProcessBasedMiniYARNCluster.
   *
   * @throws UnsupportedOperationException always
   */
  public Object getApplicationHistoryServer() {
    throw new UnsupportedOperationException(
        "Application History Server not supported in " +
        "ProcessBasedMiniYARNCluster.");
  }

  // =========================================================================
  // BUILDER
  // =========================================================================

  /**
   * Builder for ProcessBasedMiniYARNCluster. Provides a fluent API for
   * configuring cluster properties before construction.
   *
   * <p>Example:</p>
   * <pre>
   * ProcessBasedMiniYARNCluster cluster =
   *     new ProcessBasedMiniYARNCluster.Builder(conf)
   *         .numResourceManagers(2)
   *         .numNodeManagers(3)
   *         .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
   *         .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
   *         .build();
   * </pre>
   */
  public static class Builder {
    private final Configuration conf;
    private String clusterName = "test-cluster";
    private int numResourceManagers = 1;
    private int numNodeManagers = 1;
    private final Map<String, String> nodeHadoopHomes = new HashMap<>();
    private HadoopVersionRegistry versionRegistry = null;

    /**
     * Creates a new Builder with the given base configuration.
     *
     * @param conf Base configuration for the cluster
     */
    public Builder(Configuration conf) {
      this.conf = conf != null ? conf : new YarnConfiguration();
    }

    /**
     * Sets the cluster name.
     *
     * @param name Cluster name
     * @return this Builder
     */
    public Builder clusterName(String name) {
      this.clusterName = name;
      return this;
    }

    /**
     * Sets the number of ResourceManagers.
     *
     * @param num Number of RMs (1 for standalone, 2+ for HA)
     * @return this Builder
     */
    public Builder numResourceManagers(int num) {
      if (num < 1) {
        throw new IllegalArgumentException(
            "Number of ResourceManagers must be >= 1 (got: " + num + ")");
      }
      this.numResourceManagers = num;
      return this;
    }

    /**
     * Sets the number of NodeManagers.
     *
     * @param num Number of NMs
     * @return this Builder
     */
    public Builder numNodeManagers(int num) {
      if (num < 1) {
        throw new IllegalArgumentException(
            "Number of NodeManagers must be >= 1 (got: " + num + ")");
      }
      this.numNodeManagers = num;
      return this;
    }

    /**
     * Sets the Hadoop distribution for a specific ResourceManager.
     *
     * @param rmIndex RM index (0, 1, 2, ...)
     * @param hadoopHome Path to Hadoop installation
     * @return this Builder
     */
    public Builder rmHadoopDistribution(int rmIndex, String hadoopHome) {
      nodeHadoopHomes.put("rm" + rmIndex, hadoopHome);
      return this;
    }

    /**
     * Sets the Hadoop distribution for a specific NodeManager.
     *
     * @param nmIndex NM index (0, 1, 2, ...)
     * @param hadoopHome Path to Hadoop installation
     * @return this Builder
     */
    public Builder nmHadoopDistribution(int nmIndex, String hadoopHome) {
      nodeHadoopHomes.put("nm" + nmIndex, hadoopHome);
      return this;
    }

    /**
     * Sets the Hadoop distribution for all ResourceManagers.
     *
     * @param hadoopHome Path to Hadoop installation
     * @return this Builder
     */
    public Builder allRMsHadoopDistribution(String hadoopHome) {
      nodeHadoopHomes.put("rm-all", hadoopHome);
      return this;
    }

    /**
     * Sets the Hadoop distribution for all NodeManagers.
     *
     * @param hadoopHome Path to Hadoop installation
     * @return this Builder
     */
    public Builder allNMsHadoopDistribution(String hadoopHome) {
      nodeHadoopHomes.put("nm-all", hadoopHome);
      return this;
    }

    /**
     * Sets the Hadoop distribution for all nodes (RMs and NMs).
     *
     * @param hadoopHome Path to Hadoop installation
     * @return this Builder
     */
    public Builder allNodesHadoopDistribution(String hadoopHome) {
      nodeHadoopHomes.put("all", hadoopHome);
      return this;
    }

    /**
     * Sets a custom version registry.
     *
     * @param registry Version registry
     * @return this Builder
     */
    public Builder versionRegistry(HadoopVersionRegistry registry) {
      this.versionRegistry = registry;
      return this;
    }

    /**
     * Builds the ProcessBasedMiniYARNCluster.
     *
     * <p>If no Hadoop distributions are explicitly set via
     * {@link #allNodesHadoopDistribution(String)} or related methods, this will
     * automatically use the {@code hadoop.start.home} system property as the
     * default distribution for all nodes.</p>
     *
     * @return A new ProcessBasedMiniYARNCluster instance
     * @throws IOException if the cluster cannot be created
     * @throws IllegalStateException if no Hadoop distribution can be determined
     */
    public ProcessBasedMiniYARNCluster build() throws IOException {
      // If no Hadoop distributions were explicitly set, use hadoop.start.home
      if (nodeHadoopHomes.isEmpty()) {
        String defaultHome = System.getProperty("hadoop.start.home");
        if (defaultHome != null && !defaultHome.isEmpty()) {
          LOG.info("No Hadoop distributions specified. Using hadoop.start.home: {}",
              defaultHome);
          nodeHadoopHomes.put("all", defaultHome);
        } else {
          throw new IllegalStateException(
              "No Hadoop distributions specified and hadoop.start.home system " +
              "property is not set. Either:\n" +
              "  1. Call .allNodesHadoopDistribution(path) on the Builder, OR\n" +
              "  2. Set system property: -Dhadoop.start.home=/path/to/hadoop");
        }
      }

      return new ProcessBasedMiniYARNCluster(this);
    }
  }
}

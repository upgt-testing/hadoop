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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates configuration files for YARN node processes. This class creates
 * yarn-site.xml and core-site.xml files for each ResourceManager and
 * NodeManager process, with appropriate ports and directories.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Per-node configuration generation</li>
 *   <li>Dynamic port assignment integration</li>
 *   <li>HA configuration support</li>
 *   <li>Isolated data directory configuration</li>
 *   <li>Test-friendly settings (small memory, fast timeouts)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * ProcessConfigurationGenerator generator =
 *     new ProcessConfigurationGenerator();
 *
 * PortAllocator portAllocator = new PortAllocator();
 * File rmDir = new File("/tmp/rm0");
 *
 * // Generate RM configuration
 * YarnConfiguration rmConf = generator.generateResourceManagerConfig(
 *     baseConfig, 0, rmDir, false, null, portAllocator);
 *
 * // Write to file
 * File confDir = new File(rmDir, "conf");
 * generator.writeConfigToFile(rmConf, confDir);
 * </pre>
 *
 * @see PortAllocator
 * @see DirectoryManager
 */
public class ProcessConfigurationGenerator {

  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessConfigurationGenerator.class);

  /** Configuration file names */
  private static final String YARN_SITE_XML = "yarn-site.xml";
  private static final String CORE_SITE_XML = "core-site.xml";

  /**
   * Generates a ResourceManager configuration.
   *
   * @param baseConfig Base configuration to extend
   * @param rmIndex Index of this RM (0, 1, 2, ...)
   * @param workDir Working directory for this RM
   * @param haEnabled Whether HA is enabled
   * @param rmIds Array of RM IDs for HA mode (e.g., ["rm1", "rm2"])
   * @param portAllocator Port allocator for dynamic port assignment
   * @return Generated YarnConfiguration
   * @throws IOException if port allocation fails
   */
  public YarnConfiguration generateResourceManagerConfig(
      Configuration baseConfig,
      int rmIndex,
      File workDir,
      boolean haEnabled,
      String[] rmIds,
      PortAllocator portAllocator) throws IOException {

    // Create configuration - defaults will be loaded, but we'll filter when writing
    YarnConfiguration conf = new YarnConfiguration();

    String rmId = haEnabled && rmIds != null && rmIndex < rmIds.length ?
        rmIds[rmIndex] : null;

    LOG.info("Generating RM config: index={}, haEnabled={}, rmId={}",
        rmIndex, haEnabled, rmId);

    // Allocate ports for this RM
    int rpcPort = portAllocator.allocatePort();
    int schedulerPort = portAllocator.allocatePort();
    int resourceTrackerPort = portAllocator.allocatePort();
    int adminPort = portAllocator.allocatePort();
    int webappPort = portAllocator.allocatePort();

    // Basic RM configuration
    conf.set(YarnConfiguration.RM_HOSTNAME, "localhost");

    if (haEnabled && rmId != null) {
      // HA mode configuration
      conf.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);
      conf.set(YarnConfiguration.RM_HA_ID, rmId);

      // Set HA RM IDs
      if (rmIds != null) {
        conf.set(YarnConfiguration.RM_HA_IDS, String.join(",", rmIds));
      }

      // RM addresses with HA suffix
      String addressKey = YarnConfiguration.RM_ADDRESS + "." + rmId;
      conf.set(addressKey, "localhost:" + rpcPort);

      String schedulerKey = YarnConfiguration.RM_SCHEDULER_ADDRESS + "." + rmId;
      conf.set(schedulerKey, "localhost:" + schedulerPort);

      String trackerKey = YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS + "." + rmId;
      conf.set(trackerKey, "localhost:" + resourceTrackerPort);

      String adminKey = YarnConfiguration.RM_ADMIN_ADDRESS + "." + rmId;
      conf.set(adminKey, "localhost:" + adminPort);

      String webappKey = YarnConfiguration.RM_WEBAPP_ADDRESS + "." + rmId;
      conf.set(webappKey, "localhost:" + webappPort);

      // ZooKeeper for HA coordination (using embedded ZK for testing)
      conf.set(YarnConfiguration.RM_ZK_ADDRESS, "localhost:2181");
      conf.set(YarnConfiguration.RM_CLUSTER_ID, "test-cluster-" + System.currentTimeMillis());

    } else {
      // Standalone mode configuration
      conf.set(YarnConfiguration.RM_ADDRESS, "localhost:" + rpcPort);
      conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, "localhost:" + schedulerPort);
      conf.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS, "localhost:" + resourceTrackerPort);
      conf.set(YarnConfiguration.RM_ADMIN_ADDRESS, "localhost:" + adminPort);
      conf.set(YarnConfiguration.RM_WEBAPP_ADDRESS, "localhost:" + webappPort);
    }

    // State store configuration
    File stateStoreDir = new File(workDir, "data/rm-state-store");
    conf.set(YarnConfiguration.RM_STORE, "org.apache.hadoop.yarn.server.resourcemanager.recovery.FileSystemRMStateStore");
    conf.set(YarnConfiguration.FS_RM_STATE_STORE_URI, "file://" + stateStoreDir.getAbsolutePath());

    // Scheduler configuration
    conf.set(YarnConfiguration.RM_SCHEDULER, "org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler");
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
    conf.setInt(YarnConfiguration.RM_SCHEDULER_MAXIMUM_ALLOCATION_MB, 2048);

    // Test-friendly settings
    conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, true);
    conf.setBoolean(YarnConfiguration.YARN_MINICLUSTER_USE_RPC, true);
    conf.setInt(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 10000);
    conf.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 1000);

    LOG.debug("Generated RM config with ports: rpc={}, scheduler={}, " +
        "tracker={}, admin={}, webapp={}",
        rpcPort, schedulerPort, resourceTrackerPort, adminPort, webappPort);

    return conf;
  }

  /**
   * Generates a NodeManager configuration.
   *
   * @param baseConfig Base configuration to extend
   * @param nmIndex Index of this NM (0, 1, 2, ...)
   * @param workDir Working directory for this NM
   * @param rmAddress RM address to register with
   * @param portAllocator Port allocator for dynamic port assignment
   * @return Generated YarnConfiguration
   * @throws IOException if port allocation fails
   */
  public YarnConfiguration generateNodeManagerConfig(
      Configuration baseConfig,
      int nmIndex,
      File workDir,
      String rmAddress,
      PortAllocator portAllocator) throws IOException {

    // Create configuration - defaults will be loaded, but we'll filter when writing
    YarnConfiguration conf = new YarnConfiguration();

    LOG.info("Generating NM config: index={}, rmAddress={}",
        nmIndex, rmAddress);

    // Allocate ports for this NM (each service needs its own port)
    int nmPort = portAllocator.allocatePort();          // Container Management
    int localizerPort = portAllocator.allocatePort();   // Resource Localizer
    int webappPort = portAllocator.allocatePort();      // Web UI

    // Basic NM configuration
    conf.set(YarnConfiguration.NM_ADDRESS, "localhost:" + nmPort);
    conf.set(YarnConfiguration.NM_LOCALIZER_ADDRESS, "localhost:" + localizerPort);
    conf.set(YarnConfiguration.NM_WEBAPP_ADDRESS, "localhost:" + webappPort);

    // RM addresses - NMs need all RM service addresses to connect
    if (rmAddress != null) {
      conf.set(YarnConfiguration.RM_ADDRESS, rmAddress);

      // Derive other RM service addresses from the main RM address
      // In standalone mode, all RM services run on the same host with different ports
      String[] parts = rmAddress.split(":");
      if (parts.length == 2) {
        String host = parts[0];
        int basePort = Integer.parseInt(parts[1]);

        // Set all RM service addresses (using sequential ports from the base)
        conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, host + ":" + (basePort + 1));
        conf.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS, host + ":" + (basePort + 2));
        conf.set(YarnConfiguration.RM_ADMIN_ADDRESS, host + ":" + (basePort + 3));
        conf.set(YarnConfiguration.RM_WEBAPP_ADDRESS, host + ":" + (basePort + 4));
        conf.set(YarnConfiguration.RM_HOSTNAME, host);

        LOG.debug("Set RM service addresses for NM: address={}, scheduler={}, " +
            "tracker={}, admin={}, webapp={}",
            rmAddress, (basePort + 1), (basePort + 2), (basePort + 3), (basePort + 4));
      }
    }

    // Local directories
    File localDirs = new File(workDir, "local-dirs");
    File logDirs = new File(workDir, "log-dirs");

    conf.set(YarnConfiguration.NM_LOCAL_DIRS, localDirs.getAbsolutePath());
    conf.set(YarnConfiguration.NM_LOG_DIRS, logDirs.getAbsolutePath());

    // Resource configuration
    conf.setInt(YarnConfiguration.NM_PMEM_MB, 4096);
    conf.setInt(YarnConfiguration.NM_VCORES, 4);

    // Container configuration
    conf.setBoolean(YarnConfiguration.NM_PMEM_CHECK_ENABLED, false);
    conf.setBoolean(YarnConfiguration.NM_VMEM_CHECK_ENABLED, false);

    // Test-friendly settings
    conf.setInt(YarnConfiguration.NM_SLEEP_DELAY_BEFORE_SIGKILL_MS, 1000);
    conf.setInt(YarnConfiguration.NM_PROCESS_KILL_WAIT_MS, 1000);
    conf.setInt(YarnConfiguration.NM_CONTAINER_EXECUTOR_SCHED_PRIORITY, 0);

    LOG.debug("Generated NM config with ports: localizer={}, webapp={}",
        localizerPort, webappPort);

    return conf;
  }

  /**
   * Writes a configuration to files in a directory.
   * Creates yarn-site.xml and core-site.xml files.
   *
   * <p>IMPORTANT: This method extracts ONLY the properties we explicitly set
   * (excluding those from default resource files like yarn-default.xml).</p>
   *
   * @param conf Configuration to write (may contain defaults)
   * @param confDir Directory to write files to
   * @return The configuration directory
   * @throws IOException if file writing fails
   */
  public File writeConfigToFile(Configuration conf, File confDir)
      throws IOException {

    if (!confDir.exists()) {
      if (!confDir.mkdirs()) {
        throw new IOException(
            "Failed to create config directory: " + confDir);
      }
    }

    // Create a clean config with ONLY explicitly set properties (not from defaults)
    Configuration cleanConf = new Configuration(false);
    int count = 0;

    for (java.util.Map.Entry<String, String> entry : conf) {
      String key = entry.getKey();
      String value = entry.getValue();

      // Check if this property was set from a default resource file
      String[] sources = conf.getPropertySources(key);
      if (sources != null && sources.length > 0) {
        // Skip properties from default resource files
        boolean isFromDefaults = false;
        for (String source : sources) {
          if (source != null && (source.contains("-default.xml") || source.equals("Unknown"))) {
            isFromDefaults = true;
            break;
          }
        }

        // Only include properties NOT from default resources
        if (!isFromDefaults) {
          cleanConf.set(key, value);
          count++;
        }
      }
    }

    // Write yarn-site.xml with ONLY explicit properties
    File yarnSiteFile = new File(confDir, YARN_SITE_XML);
    try (FileOutputStream out = new FileOutputStream(yarnSiteFile)) {
      cleanConf.writeXml(out);
      out.flush();
    }
    LOG.info("Wrote config to: {} ({} explicit properties, {} total in source)",
        yarnSiteFile, count, conf.size());

    // Write core-site.xml (for any Hadoop-wide settings)
    File coreSiteFile = new File(confDir, CORE_SITE_XML);
    try (FileOutputStream out = new FileOutputStream(coreSiteFile)) {
      // Write a minimal core-site.xml with hadoop.* and fs.* properties from cleanConf
      Configuration coreConf = new Configuration(false);
      for (java.util.Map.Entry<String, String> entry : cleanConf) {
        if (entry.getKey().startsWith("hadoop.") ||
            entry.getKey().startsWith("fs.")) {
          coreConf.set(entry.getKey(), entry.getValue());
        }
      }
      coreConf.writeXml(out);
      out.flush();
    }
    LOG.debug("Wrote core-site.xml to: {}", coreSiteFile);

    return confDir;
  }

  /**
   * Creates a base configuration with test-friendly defaults.
   *
   * <p>Note: This creates a configuration that loads defaults normally,
   * but the test-specific properties are marked as programmatically set
   * and will be included when generating node configs.</p>
   *
   * @return Base YarnConfiguration with test-friendly properties
   */
  public YarnConfiguration createBaseConfiguration() {
    // Create with defaults for normal operation
    YarnConfiguration conf = new YarnConfiguration();

    // Disable security for testing (these will be marked as "programmatically" set)
    conf.set("hadoop.security.authentication", "simple");
    conf.setBoolean("hadoop.security.authorization", false);

    // Fast timeouts for testing
    conf.setInt(YarnConfiguration.CLIENT_FAILOVER_SLEEPTIME_BASE_MS, 100);
    conf.setInt(YarnConfiguration.CLIENT_FAILOVER_SLEEPTIME_MAX_MS, 1000);
    conf.setInt(YarnConfiguration.RESOURCEMANAGER_CONNECT_RETRY_INTERVAL_MS, 1000);

    // Enable recovery for HA testing
    conf.setBoolean(YarnConfiguration.RECOVERY_ENABLED, true);

    // Disable unnecessary services for testing
    conf.setBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, false);

    LOG.debug("Created base configuration with test-friendly properties");
    return conf;
  }

  /**
   * Generates configuration for multiple ResourceManagers (HA mode).
   *
   * @param baseConfig Base configuration
   * @param numRMs Number of RMs
   * @param workDirs Work directories for each RM
   * @param portAllocator Port allocator
   * @return Array of configurations, one per RM
   * @throws IOException if configuration generation fails
   */
  public YarnConfiguration[] generateHAResourceManagerConfigs(
      Configuration baseConfig,
      int numRMs,
      File[] workDirs,
      PortAllocator portAllocator) throws IOException {

    if (numRMs < 2) {
      throw new IllegalArgumentException(
          "HA mode requires at least 2 RMs (got: " + numRMs + ")");
    }

    if (workDirs.length != numRMs) {
      throw new IllegalArgumentException(
          "Work dirs array length must match numRMs");
    }

    // Generate RM IDs
    String[] rmIds = new String[numRMs];
    for (int i = 0; i < numRMs; i++) {
      rmIds[i] = "rm" + (i + 1);
    }

    // Generate configuration for each RM
    YarnConfiguration[] configs = new YarnConfiguration[numRMs];
    for (int i = 0; i < numRMs; i++) {
      configs[i] = generateResourceManagerConfig(
          baseConfig, i, workDirs[i], true, rmIds, portAllocator);
    }

    LOG.info("Generated HA configurations for {} RMs", numRMs);
    return configs;
  }
}

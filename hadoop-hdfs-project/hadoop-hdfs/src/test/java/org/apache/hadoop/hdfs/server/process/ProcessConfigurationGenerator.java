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
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates configuration files for ProcessBasedMiniDFSCluster nodes.
 *
 * <p>This class is responsible for creating node-specific Hadoop configuration
 * files (core-site.xml, hdfs-site.xml) that enable nodes to run in isolated
 * processes with unique ports and data directories.
 *
 * <p>Configuration generation includes:
 * <ul>
 *   <li>Port assignment for RPC, HTTP, and data transfer</li>
 *   <li>Data directory paths specific to each node</li>
 *   <li>Cluster-wide settings (replication factor, block size, etc.)</li>
 *   <li>Version-specific configuration adjustments</li>
 * </ul>
 *
 * <p>Generated configurations are written as XML files to the node's
 * configuration directory, ready to be loaded by the node process.
 */
public class ProcessConfigurationGenerator {
  private static final Logger LOG =
      LoggerFactory.getLogger(ProcessConfigurationGenerator.class);

  // Configuration file names
  private static final String CORE_SITE_XML = "core-site.xml";
  private static final String HDFS_SITE_XML = "hdfs-site.xml";

  // Default values
  private static final int DEFAULT_REPLICATION = 1;
  private static final long DEFAULT_BLOCK_SIZE = 1048576; // 1MB for testing
  private static final String DEFAULT_FILESYSTEM = "hdfs://localhost:9000";

  private final Configuration baseConfig;
  private final PortAllocator portAllocator;

  /**
   * Creates a new ProcessConfigurationGenerator.
   *
   * @param baseConfig base configuration to inherit settings from
   * @param portAllocator port allocator for assigning network ports
   */
  public ProcessConfigurationGenerator(Configuration baseConfig,
                                        PortAllocator portAllocator) {
    this.baseConfig = baseConfig != null ? baseConfig : new Configuration(false);
    this.portAllocator = portAllocator;
  }

  /**
   * Generates configuration for a NameNode.
   *
   * @param nnIndex the NameNode index (0 for single NN, 0-N for HA)
   * @param nodeDir the node directory structure
   * @return NodeConfiguration containing the generated config and allocated ports
   * @throws IOException if port allocation or config generation fails
   */
  public NodeConfiguration generateNameNodeConfig(
      int nnIndex, DirectoryManager.NodeDirectory nodeDir) throws IOException {

    Configuration config = new Configuration(baseConfig);

    // Allocate ports for NameNode
    int rpcPort = portAllocator.allocatePort();
    int httpPort = portAllocator.allocatePort();
    int serviceRpcPort = portAllocator.allocatePort();

    Map<String, Integer> allocatedPorts = new HashMap<>();
    allocatedPorts.put("rpc", rpcPort);
    allocatedPorts.put("http", httpPort);
    allocatedPorts.put("serviceRpc", serviceRpcPort);

    // Set NameNode-specific properties
    // Use 127.0.0.1 instead of "localhost" to avoid IPv4/IPv6 binding issues on macOS
    String nnHost = "127.0.0.1";
    String nnRpcAddress = nnHost + ":" + rpcPort;

    // Set the default filesystem URI (critical for HDFS operations)
    config.set("fs.defaultFS", "hdfs://" + nnRpcAddress);

    config.set(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY, nnRpcAddress);
    config.set(DFSConfigKeys.DFS_NAMENODE_HTTP_ADDRESS_KEY,
        nnHost + ":" + httpPort);

    // Explicit bind hosts to avoid IPv4/IPv6 dual-stack issues
    config.set(DFSConfigKeys.DFS_NAMENODE_RPC_BIND_HOST_KEY, "127.0.0.1");
    config.set(DFSConfigKeys.DFS_NAMENODE_HTTP_BIND_HOST_KEY, "127.0.0.1");

    // Note: Service RPC removed for simplicity - DNs will use regular RPC
    // config.set(DFSConfigKeys.DFS_NAMENODE_SERVICE_RPC_ADDRESS_KEY,
    //     nnHost + ":" + serviceRpcPort);

    // Set data directory
    config.set(DFSConfigKeys.DFS_NAMENODE_NAME_DIR_KEY,
        new File(nodeDir.getDataDir(), "name").toURI().toString());

    // Set checkpoint directory
    config.set(DFSConfigKeys.DFS_NAMENODE_CHECKPOINT_DIR_KEY,
        new File(nodeDir.getDataDir(), "namesecondary").toURI().toString());

    // Disable image/edit log streaming for simplicity in testing
    config.setBoolean(DFSConfigKeys.DFS_IMAGE_TRANSFER_RATE_KEY + ".test", true);

    // Set common HDFS properties for mini cluster
    setCommonHdfsProperties(config);

    // Write configuration files
    writeConfigurationFiles(config, nodeDir.getConfDir());

    LOG.info("Generated NameNode configuration for nn{}: rpc={}, http={}, serviceRpc={}",
        nnIndex, rpcPort, httpPort, serviceRpcPort);

    return new NodeConfiguration(config, allocatedPorts);
  }

  /**
   * Generates configuration for a DataNode (backward compatibility).
   *
   * @param dnIndex the DataNode index (0 to numDataNodes-1)
   * @param nodeDir the node directory structure
   * @param nameNodeAddresses list of NameNode RPC addresses to connect to
   * @return NodeConfiguration containing the generated config and allocated ports
   * @throws IOException if port allocation or config generation fails
   */
  public NodeConfiguration generateDataNodeConfig(
      int dnIndex,
      DirectoryManager.NodeDirectory nodeDir,
      List<InetSocketAddress> nameNodeAddresses) throws IOException {
    return generateDataNodeConfig(dnIndex, nodeDir, nameNodeAddresses, null);
  }

  /**
   * Generates configuration for a DataNode with storage type configuration.
   *
   * @param dnIndex the DataNode index (0 to numDataNodes-1)
   * @param nodeDir the node directory structure
   * @param nameNodeAddresses list of NameNode RPC addresses to connect to
   * @param storageTypes array of storage types for each storage location
   * @return NodeConfiguration containing the generated config and allocated ports
   * @throws IOException if port allocation or config generation fails
   */
  public NodeConfiguration generateDataNodeConfig(
      int dnIndex,
      DirectoryManager.NodeDirectory nodeDir,
      List<InetSocketAddress> nameNodeAddresses,
      StorageType[] storageTypes) throws IOException {

    if (nameNodeAddresses == null || nameNodeAddresses.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one NameNode address is required for DataNode configuration");
    }

    Configuration config = new Configuration(baseConfig);

    // Allocate ports for DataNode
    int dataPort = portAllocator.allocatePort();
    int ipcPort = portAllocator.allocatePort();
    int httpPort = portAllocator.allocatePort();

    Map<String, Integer> allocatedPorts = new HashMap<>();
    allocatedPorts.put("data", dataPort);
    allocatedPorts.put("ipc", ipcPort);
    allocatedPorts.put("http", httpPort);

    // Set DataNode-specific properties
    // Use 127.0.0.1 instead of "localhost" to avoid IPv4/IPv6 binding issues on macOS
    String dnHost = "127.0.0.1";
    config.set(DFSConfigKeys.DFS_DATANODE_ADDRESS_KEY,
        dnHost + ":" + dataPort);
    config.set(DFSConfigKeys.DFS_DATANODE_IPC_ADDRESS_KEY,
        dnHost + ":" + ipcPort);
    config.set(DFSConfigKeys.DFS_DATANODE_HTTP_ADDRESS_KEY,
        dnHost + ":" + httpPort);

    // Set hostname explicitly to ensure DN reports correct address to NN
    config.set(DFSConfigKeys.DFS_DATANODE_HOST_NAME_KEY, "127.0.0.1");

    // Explicit bind hosts to avoid IPv4/IPv6 dual-stack issues
    config.set("dfs.datanode.http.bind-host", "127.0.0.1");

    // Build comma-separated list of data directories with storage type prefixes
    StringBuilder dataDirBuilder = new StringBuilder();
    List<File> storageDirs = nodeDir.getDataStorageDirs();
    StorageType[] types = nodeDir.getStorageTypes();

    for (int i = 0; i < storageDirs.size(); i++) {
      if (i > 0) {
        dataDirBuilder.append(",");
      }

      StorageType type = (types != null && i < types.length)
          ? types[i]
          : StorageType.DEFAULT;

      // Format: [DISK]file:///path,[SSD]file:///path2
      dataDirBuilder.append("[").append(type.toString()).append("]")
          .append(storageDirs.get(i).toURI().toString());
    }

    config.set(DFSConfigKeys.DFS_DATANODE_DATA_DIR_KEY, dataDirBuilder.toString());

    // Set NameNode address(es)
    if (nameNodeAddresses.size() == 1) {
      // Single NameNode
      InetSocketAddress nnAddr = nameNodeAddresses.get(0);
      // Always use 127.0.0.1 to match NN's bind address and avoid IPv4/IPv6 issues
      String nnRpcAddress = "127.0.0.1:" + nnAddr.getPort();
      config.set("fs.defaultFS", "hdfs://" + nnRpcAddress);
      config.set(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY, nnRpcAddress);
    } else {
      // Multiple NameNodes (HA setup)
      // For now, just use the first one; full HA support will be added later
      InetSocketAddress nnAddr = nameNodeAddresses.get(0);
      // Always use 127.0.0.1 to match NN's bind address and avoid IPv4/IPv6 issues
      String nnRpcAddress = "127.0.0.1:" + nnAddr.getPort();
      config.set("fs.defaultFS", "hdfs://" + nnRpcAddress);
      config.set(DFSConfigKeys.DFS_NAMENODE_RPC_ADDRESS_KEY, nnRpcAddress);
      LOG.warn("Multiple NameNodes specified, but HA configuration not yet implemented. " +
          "Using first NameNode: {}", nnAddr);
    }

    // Set common HDFS properties for mini cluster
    setCommonHdfsProperties(config);

    // Write configuration files
    writeConfigurationFiles(config, nodeDir.getConfDir());

    LOG.info("Generated DataNode configuration for dn{} with {} storage locations: data={}, ipc={}, http={}",
        dnIndex, storageDirs.size(), dataPort, ipcPort, httpPort);

    return new NodeConfiguration(config, allocatedPorts);
  }

  /**
   * Generates configuration for a DataNode with storage type and rack configuration.
   *
   * @param dnIndex the DataNode index (0 to numDataNodes-1)
   * @param nodeDir the node directory structure
   * @param nameNodeAddresses list of NameNode RPC addresses to connect to
   * @param storageTypes array of storage types for each storage location
   * @param rack the rack assignment for this DataNode (can be null)
   * @param allRacks all rack assignments for rack topology mapping (can be null)
   * @return NodeConfiguration containing the generated config and allocated ports
   * @throws IOException if port allocation or config generation fails
   */
  public NodeConfiguration generateDataNodeConfig(
      int dnIndex,
      DirectoryManager.NodeDirectory nodeDir,
      List<InetSocketAddress> nameNodeAddresses,
      StorageType[] storageTypes,
      String rack,
      String[] allRacks) throws IOException {

    // First generate the basic config
    NodeConfiguration baseConfig = generateDataNodeConfig(dnIndex, nodeDir, nameNodeAddresses, storageTypes);
    Configuration config = baseConfig.getConfig();

    // Configure rack topology if racks are specified
    if (rack != null && allRacks != null) {
      // Use StaticMapping for rack awareness
      config.setClass("net.topology.node.switch.mapping.impl",
          org.apache.hadoop.net.StaticMapping.class,
          org.apache.hadoop.net.DNSToSwitchMapping.class);

      // Build the node-to-rack mapping string
      // Format: hostname1=rack1,hostname2=rack2,...
      StringBuilder mappingBuilder = new StringBuilder();
      for (int i = 0; i < allRacks.length; i++) {
        if (i > 0) {
          mappingBuilder.append(",");
        }
        // Use localhost:port as the hostname identifier
        String nodeId = "127.0.0.1:" + (50000 + i);  // Simple node identifier
        mappingBuilder.append(nodeId).append("=").append(allRacks[i]);
      }

      config.set("hadoop.configured.node.mapping", mappingBuilder.toString());

      // Also configure the hostname for this specific DataNode
      String dnHost = "127.0.0.1";
      config.set(DFSConfigKeys.DFS_DATANODE_HOST_NAME_KEY, dnHost);

      LOG.info("Configured DataNode dn{} with rack: {}", dnIndex, rack);
    }

    // Re-write configuration files with rack settings
    if (rack != null) {
      writeConfigurationFiles(config, nodeDir.getConfDir());
    }

    return new NodeConfiguration(config, baseConfig.getAllocatedPorts());
  }

  /**
   * Sets common HDFS properties suitable for a mini cluster.
   *
   * @param config the configuration to update
   */
  private void setCommonHdfsProperties(Configuration config) {
    // Set low replication for mini cluster
    if (!config.get(DFSConfigKeys.DFS_REPLICATION_KEY, "").isEmpty()) {
      // Keep user-specified value
    } else {
      config.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, DEFAULT_REPLICATION);
    }

    // Set small block size for faster testing
    if (config.getLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, -1) == -1) {
      config.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, DEFAULT_BLOCK_SIZE);
    }

    // Disable permissions checking for easier testing
    config.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, false);

    // Reduce heartbeat interval for faster cluster startup
    config.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
    config.setLong(DFSConfigKeys.DFS_NAMENODE_HEARTBEAT_RECHECK_INTERVAL_KEY, 1000);

    // Disable block scanner for DataNode (not needed in tests)
    config.setLong(DFSConfigKeys.DFS_DATANODE_SCAN_PERIOD_HOURS_KEY, 0);

    // Enable DataNode to bind to any available address
    config.setBoolean("dfs.datanode.address.allow-ephemeral", true);

    // Reduce safemode threshold for faster startup
    config.setFloat(DFSConfigKeys.DFS_NAMENODE_SAFEMODE_THRESHOLD_PCT_KEY, 0.0f);

    // Set namenode startup options
    config.set(DFSConfigKeys.DFS_NAMENODE_STARTUP_KEY, "regular");
  }

  /**
   * Writes configuration to XML files in the specified directory.
   *
   * @param config the configuration to write
   * @param confDir the directory to write configuration files to
   * @throws IOException if writing fails
   */
  private void writeConfigurationFiles(Configuration config, File confDir)
      throws IOException {

    if (!confDir.exists() && !confDir.mkdirs()) {
      throw new IOException("Failed to create configuration directory: " + confDir);
    }

    // Copy essential test resources first (these will be loaded before generated configs)
    // TEMPORARILY DISABLED - may be causing process failures
    // copyTestResources(confDir);

    // Write core-site.xml
    File coreSiteFile = new File(confDir, CORE_SITE_XML);
    writeConfigFile(config, coreSiteFile);

    // Write hdfs-site.xml (same config for simplicity)
    File hdfsSiteFile = new File(confDir, HDFS_SITE_XML);
    writeConfigFile(config, hdfsSiteFile);

    // Write log4j.properties for logging configuration
    File log4jFile = new File(confDir, "log4j.properties");
    writeLog4jProperties(log4jFile);

    LOG.debug("Wrote configuration files to: {}", confDir);
  }

  /**
   * Copy essential test resource configuration files to the node's conf directory.
   * This ensures spawned processes have the same test resources as in-process tests.
   *
   * @param confDir the directory to copy test resources to
   * @throws IOException if copying fails
   */
  private void copyTestResources(File confDir) throws IOException {
    // Essential test resources to copy (if they exist on classpath)
    String[] testResources = {
        "hadoop-policy.xml",  // ACL configurations for all protocols
        "fi-site.xml"         // Fault injection configuration
    };

    for (String resourceName : testResources) {
      URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
      if (resourceUrl != null) {
        File destFile = new File(confDir, resourceName);
        try (InputStream in = resourceUrl.openStream();
             OutputStream out = new FileOutputStream(destFile)) {
          IOUtils.copyBytes(in, out, 4096, false);
          LOG.info("Copied test resource {} to {}", resourceName, destFile);
        } catch (IOException e) {
          LOG.warn("Failed to copy test resource {}: {}", resourceName, e.getMessage());
          // Don't fail - just log warning and continue
        }
      } else {
        LOG.debug("Test resource {} not found on classpath, skipping", resourceName);
      }
    }
  }

  /**
   * Writes a Configuration object to an XML file.
   * Only writes properties that were explicitly set (not defaults).
   * This makes the configuration files much smaller and easier to debug.
   *
   * @param config the configuration to write
   * @param file the file to write to
   * @throws IOException if writing fails
   */
  private void writeConfigFile(Configuration config, File file) throws IOException {
    // Create a new Configuration with only explicitly set properties
    Configuration configToWrite = new Configuration(false); // false = don't load defaults

    // Copy only properties that were explicitly set (not from default resources)
    for (java.util.Map.Entry<String, String> entry : config) {
      String key = entry.getKey();
      String value = entry.getValue();

      // Check if this property was set programmatically or from a non-default resource
      String[] sources = config.getPropertySources(key);
      if (sources != null && sources.length > 0) {
        // Skip properties from default resource files
        boolean isFromDefaults = false;
        for (String source : sources) {
          if (source != null && (source.contains("-default.xml") || source.equals("Unknown"))) {
            isFromDefaults = true;
            break;
          }
        }

        if (!isFromDefaults) {
          configToWrite.set(key, value);
        }
      }
    }

    try (OutputStream out = new FileOutputStream(file)) {
      configToWrite.writeXml(out);
      out.flush();
    }
    LOG.debug("Wrote {} configuration properties to: {}", configToWrite.size(), file);
  }

  /**
   * Writes a basic log4j.properties file for Hadoop logging.
   *
   * @param file the file to write to
   * @throws IOException if writing fails
   */
  private void writeLog4jProperties(File file) throws IOException {
    String log4jConfig =
        "# Log4j configuration for ProcessBasedMiniDFSCluster\n" +
        "log4j.rootLogger=INFO,console\n" +
        "\n" +
        "# Console appender\n" +
        "log4j.appender.console=org.apache.log4j.ConsoleAppender\n" +
        "log4j.appender.console.target=System.err\n" +
        "log4j.appender.console.layout=org.apache.log4j.PatternLayout\n" +
        "log4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n" +
        "\n" +
        "# Reduce verbosity of some noisy loggers\n" +
        "log4j.logger.org.apache.hadoop.util.NativeCodeLoader=ERROR\n" +
        "log4j.logger.org.apache.hadoop.security.ShellBasedUnixGroupsMapping=ERROR\n" +
        "log4j.logger.org.apache.hadoop.metrics2=ERROR\n" +
        "log4j.logger.org.eclipse.jetty=WARN\n" +
        "log4j.logger.org.apache.hadoop.ipc.Server=WARN\n";

    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(log4jConfig.getBytes("UTF-8"));
      out.flush();
    }
    LOG.debug("Wrote log4j.properties to: {}", file);
  }

  /**
   * Container class for generated node configuration and allocated ports.
   */
  public static class NodeConfiguration {
    private final Configuration config;
    private final Map<String, Integer> allocatedPorts;

    public NodeConfiguration(Configuration config, Map<String, Integer> allocatedPorts) {
      this.config = config;
      this.allocatedPorts = new HashMap<>(allocatedPorts);
    }

    /**
     * Gets the generated configuration.
     *
     * @return the Hadoop configuration
     */
    public Configuration getConfig() {
      return config;
    }

    /**
     * Gets the map of allocated ports by name.
     *
     * <p>For NameNode: "rpc", "http", "serviceRpc"
     * <p>For DataNode: "data", "ipc", "http"
     *
     * @return map of port name to port number
     */
    public Map<String, Integer> getAllocatedPorts() {
      return new HashMap<>(allocatedPorts);
    }

    /**
     * Gets a specific allocated port by name.
     *
     * @param portName the name of the port (e.g., "rpc", "http")
     * @return the port number, or null if not found
     */
    public Integer getPort(String portName) {
      return allocatedPorts.get(portName);
    }

    /**
     * Gets the RPC port (applicable to both NameNode and DataNode).
     *
     * @return the RPC port, or -1 if not applicable
     */
    public int getRpcPort() {
      Integer port = allocatedPorts.get("rpc");
      if (port != null) {
        return port;
      }
      port = allocatedPorts.get("ipc");
      return port != null ? port : -1;
    }

    /**
     * Gets the HTTP port.
     *
     * @return the HTTP port, or -1 if not allocated
     */
    public int getHttpPort() {
      Integer port = allocatedPorts.get("http");
      return port != null ? port : -1;
    }

    @Override
    public String toString() {
      return "NodeConfiguration{" +
          "allocatedPorts=" + allocatedPorts +
          '}';
    }
  }
}

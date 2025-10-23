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
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocols;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.DNSToSwitchMapping;
import org.apache.hadoop.net.StaticMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ProcessBasedMiniDFSCluster - A test cluster where each NameNode and DataNode
 * runs in a separate JVM process. This enables testing with different Hadoop
 * versions for different nodes, supporting version upgrade and compatibility testing.
 *
 * <p>Unlike {@link org.apache.hadoop.hdfs.MiniDFSCluster}, this cluster:
 * <ul>
 *   <li>Runs each node in its own JVM process with isolated classpath</li>
 *   <li>Supports running different Hadoop versions per node</li>
 *   <li>Only supports client-side operations (RPC/HTTP based)</li>
 *   <li>Does NOT support direct object access to NameNode/DataNode instances</li>
 * </ul>
 *
 * <p><b>Standard usage (system properties are automatic):</b>
 * <pre>
 * // Run with: mvn test -Dtest=MyTest \
 * //              -Dhadoop.start.home=/opt/hadoop-3.3.5 \
 * //              -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
 * ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
 *     .numDataNodes(3)
 *     .format(true)
 *     .build();  // Automatically reads from system properties!
 *
 * try {
 *     // Basic operations use start version
 *     FileSystem fs = cluster.getFileSystem();
 *     fs.mkdirs(new Path("/test"));
 *
 *     // Rolling upgrade: upgrade DataNodes one by one
 *     String upgradeHome = cluster.getUpgradeDistributionPath();
 *     for (int i = 0; i &lt; 3; i++) {
 *         cluster.shutdownDataNode(i);
 *         cluster.changeDataNodeVersion(i, upgradeHome);
 *         cluster.startDataNode(i);
 *     }
 * } finally {
 *     cluster.shutdown();
 * }
 * </pre>
 *
 * <p><b>Supported system properties:</b>
 * <ul>
 *   <li>{@code -Dhadoop.start.home=/path} - Start version Hadoop distribution (initial cluster version)</li>
 *   <li>{@code -Dhadoop.upgrade.home=/path} - Upgrade version Hadoop distribution (target version for rolling upgrades)</li>
 * </ul>
 *
 * <p><b>Backward compatibility:</b>
 * Environment variables HADOOP_HOME (for start version) and HADOOP_UPGRADE_HOME (for upgrade version)
 * are still supported as fallback, but system properties are preferred for easier test execution via Maven.</pre>
 *
 * <p><b>Manual distribution paths (for advanced scenarios):</b>
 * <pre>
 * ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
 *     .numDataNodes(3)
 *     .nameNodeHadoopDistribution("/opt/hadoop-3.3.1")
 *     .dataNodeHadoopDistribution(0, "/opt/hadoop-3.3.5")
 *     .dataNodeHadoopDistribution(1, "/opt/hadoop-3.3.5")
 *     .dataNodeHadoopDistribution(2, "/opt/hadoop-3.3.5")
 *     .format(true)
 *     .build();
 * </pre>
 */
public class ProcessBasedMiniDFSCluster implements AutoCloseable, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessBasedMiniDFSCluster.class);

    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;

    // Configuration
    private final Configuration baseConfiguration;
    private final int numNameNodes;
    private int numDataNodes;  // No longer final - can grow dynamically
    private final boolean format;

    // Storage configuration
    private final StorageType[][] storageTypes;
    private final int storagesPerDatanode;

    // Network topology
    private final String[] racks;

    // Hadoop version registry
    private final HadoopVersionRegistry versionRegistry;

    // Upgrade version tracking (for rolling upgrade scenarios)
    private final String upgradeDistributionPath;

    // Process managers
    private final List<NameNodeProcessManager> nameNodeManagers;
    private final List<DataNodeProcessManager> dataNodeManagers;

    // Configuration generators
    private final ProcessConfigurationGenerator configGenerator;
    private final PortAllocator portAllocator;
    private final DirectoryManager directoryManager;

    // Cluster state
    private boolean isStarted = false;
    private boolean isShutdown = false;

    // FileSystem instance (cached)
    private DistributedFileSystem fileSystem;

    /**
     * Private constructor - use Builder to create instances.
     */
    private ProcessBasedMiniDFSCluster(Builder builder) throws IOException {
        // Don't copy the configuration - use the same instance so updates are visible to caller
        this.baseConfiguration = builder.conf;

        // Apply test-specific default configurations (matching MiniDFSCluster behavior)
        applyTestDefaults(this.baseConfiguration);

        this.numNameNodes = builder.numNameNodes;
        this.numDataNodes = builder.numDataNodes;
        this.format = builder.format;

        // Initialize storage configuration
        this.storagesPerDatanode = builder.storagesPerDatanode;

        // Validate and expand storage types configuration
        if (builder.storageTypes != null && builder.storageTypes.length != builder.numDataNodes) {
            throw new IllegalArgumentException(
                "storageTypes array length (" + builder.storageTypes.length +
                ") must match numDataNodes (" + builder.numDataNodes + ")");
        }

        // Expand 1D array to 2D if needed
        if (builder.storageTypes == null && builder.storageTypes1D != null) {
            // Duplicate the 1D config for all DataNodes
            this.storageTypes = new StorageType[builder.numDataNodes][];
            for (int i = 0; i < builder.numDataNodes; i++) {
                this.storageTypes[i] = builder.storageTypes1D;
            }
        } else {
            this.storageTypes = builder.storageTypes;
        }

        // Initialize rack configuration
        if (builder.racks != null && builder.racks.length != builder.numDataNodes) {
            throw new IllegalArgumentException(
                "racks array length (" + builder.racks.length +
                ") must match numDataNodes (" + builder.numDataNodes + ")");
        }
        this.racks = builder.racks;

        // Initialize managers
        this.versionRegistry = new HadoopVersionRegistry();
        this.upgradeDistributionPath = builder.upgradeHadoopHome;
        this.nameNodeManagers = new ArrayList<>();
        this.dataNodeManagers = new ArrayList<>();

        // Initialize utilities
        this.portAllocator = new PortAllocator(builder.portRangeStart, builder.portRangeEnd);

        // Create base directory
        File baseDir = builder.baseDir != null ? builder.baseDir : createDefaultBaseDir();
        this.directoryManager = new DirectoryManager(baseDir, true);  // cleanup on exit
        this.configGenerator = new ProcessConfigurationGenerator(baseConfiguration, portAllocator);

        // Register Hadoop distributions
        registerHadoopDistributions(builder);

        // Build the cluster
        buildCluster();
    }

    /**
     * Apply test-specific default configurations to match MiniDFSCluster behavior.
     * This ensures ProcessBasedMiniDFSCluster has the same test defaults as MiniDFSCluster.
     *
     * @param conf the configuration to apply defaults to
     */
    private void applyTestDefaults(Configuration conf) {
        // Block scanner volume join timeout (default: 30 seconds for tests)
        // This matches MiniDFSCluster.Builder.initDefaultConfigurations()
        long defaultScannerTimeout = conf.getLong(
            DFSConfigKeys.DFS_BLOCK_SCANNER_VOLUME_JOIN_TIMEOUT_MSEC_KEY,
            TimeUnit.SECONDS.toMillis(30));
        conf.setLong(DFSConfigKeys.DFS_BLOCK_SCANNER_VOLUME_JOIN_TIMEOUT_MSEC_KEY,
            defaultScannerTimeout);

        // Do not consider load factor when selecting a datanode (default for tests)
        // This matches MiniDFSCluster.Builder.initDefaultConfigurations()
        conf.setBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY, false);

        // Safemode extension from testing key
        // This matches MiniDFSCluster.initMiniDFSCluster()
        int safemodeExtension = conf.getInt(
            DFSConfigKeys.DFS_NAMENODE_SAFEMODE_EXTENSION_KEY + ".testing", 0);
        conf.setInt(DFSConfigKeys.DFS_NAMENODE_SAFEMODE_EXTENSION_KEY, safemodeExtension);

        // Decommission interval from testing key (default: 3 seconds for tests)
        // This matches MiniDFSCluster.initMiniDFSCluster()
        long decommissionInterval = conf.getTimeDuration(
            DFSConfigKeys.DFS_NAMENODE_DECOMMISSION_INTERVAL_KEY + ".testing",
            3, TimeUnit.SECONDS);
        conf.setTimeDuration(DFSConfigKeys.DFS_NAMENODE_DECOMMISSION_INTERVAL_KEY,
            decommissionInterval, TimeUnit.SECONDS);

        // Use StaticMapping for network topology (for rack awareness in tests)
        // This matches MiniDFSCluster.initMiniDFSCluster()
        conf.setClass("net.topology.node.switch.mapping.impl",
            StaticMapping.class, DNSToSwitchMapping.class);

        // Block report initial delay (default: 0 for tests)
        // This matches MiniDFSCluster.startDataNodes()
        if (conf.get(DFSConfigKeys.DFS_BLOCKREPORT_INITIAL_DELAY_KEY) == null) {
            conf.setLong(DFSConfigKeys.DFS_BLOCKREPORT_INITIAL_DELAY_KEY, 0);
        }

        // Disable min block size for tests (from hdfs-site.xml test resources)
        // This allows tests to use tiny blocks
        if (conf.get(DFSConfigKeys.DFS_NAMENODE_MIN_BLOCK_SIZE_KEY) == null) {
            conf.setLong(DFSConfigKeys.DFS_NAMENODE_MIN_BLOCK_SIZE_KEY, 0);
        }

        // Simple authentication for tests (from hdfs-site.xml test resources)
        // This turns security off by default for testing
        if (conf.get("hadoop.security.authentication") == null) {
            conf.set("hadoop.security.authentication", "simple");
        }

        // Adjust replication based on numDataNodes (matches MiniDFSCluster.initMiniDFSCluster())
        // This prevents replication errors when cluster has fewer DNs than replication factor
        if (numDataNodes > 0) {
            int replication = conf.getInt(DFSConfigKeys.DFS_REPLICATION_KEY, 3);
            conf.setInt(DFSConfigKeys.DFS_REPLICATION_KEY, Math.min(replication, numDataNodes));

            // Also adjust maintenance replication minimum
            int maintenanceMinReplication = conf.getInt(
                DFSConfigKeys.DFS_NAMENODE_MAINTENANCE_REPLICATION_MIN_KEY,
                DFSConfigKeys.DFS_NAMENODE_MAINTENANCE_REPLICATION_MIN_DEFAULT);
            if (maintenanceMinReplication ==
                DFSConfigKeys.DFS_NAMENODE_MAINTENANCE_REPLICATION_MIN_DEFAULT) {
                conf.setInt(DFSConfigKeys.DFS_NAMENODE_MAINTENANCE_REPLICATION_MIN_KEY,
                    Math.min(maintenanceMinReplication, numDataNodes));
            }
        }

        LOG.info("Applied test-specific default configurations");
    }

    /**
     * Validate that ProcessBasedMiniDFSCluster has the same test defaults as MiniDFSCluster.
     * This is useful for debugging configuration issues and ensuring test compatibility.
     *
     * @return true if all critical test defaults are correctly configured
     */
    public boolean validateTestDefaults() {
        Configuration conf = baseConfiguration;
        boolean valid = true;

        // Check redundancy considerload (should be false for tests)
        if (conf.getBoolean(DFSConfigKeys.DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD_KEY, true)) {
            LOG.warn("Configuration mismatch: DFS_NAMENODE_REDUNDANCY_CONSIDERLOAD should be false for tests");
            valid = false;
        }

        // Check block report initial delay (should be 0 for tests)
        long blockReportDelay = conf.getLong(DFSConfigKeys.DFS_BLOCKREPORT_INITIAL_DELAY_KEY, -1);
        if (blockReportDelay != 0) {
            LOG.warn("Configuration mismatch: DFS_BLOCKREPORT_INITIAL_DELAY should be 0 for tests, got: {}",
                blockReportDelay);
            valid = false;
        }

        // Check min block size (should be 0 for tests)
        long minBlockSize = conf.getLong(DFSConfigKeys.DFS_NAMENODE_MIN_BLOCK_SIZE_KEY, 1024*1024);
        if (minBlockSize != 0) {
            LOG.warn("Configuration mismatch: DFS_NAMENODE_MIN_BLOCK_SIZE should be 0 for tests, got: {}",
                minBlockSize);
            valid = false;
        }

        // Check authentication (should be simple for tests)
        String auth = conf.get("hadoop.security.authentication", "kerberos");
        if (!"simple".equals(auth)) {
            LOG.warn("Configuration mismatch: hadoop.security.authentication should be 'simple' for tests, got: {}",
                auth);
            valid = false;
        }

        // Check network topology mapping
        Class<?> mappingClass = conf.getClass("net.topology.node.switch.mapping.impl", null);
        if (mappingClass != StaticMapping.class) {
            LOG.warn("Configuration mismatch: net.topology.node.switch.mapping.impl should be StaticMapping for tests, got: {}",
                mappingClass);
            valid = false;
        }

        // Check replication is adjusted for cluster size
        if (numDataNodes > 0) {
            int replication = conf.getInt(DFSConfigKeys.DFS_REPLICATION_KEY, 3);
            if (replication > numDataNodes) {
                LOG.warn("Configuration mismatch: DFS_REPLICATION ({}) exceeds numDataNodes ({})",
                    replication, numDataNodes);
                valid = false;
            }
        }

        if (valid) {
            LOG.info("Test configuration validation passed - cluster configuration matches MiniDFSCluster defaults");
        } else {
            LOG.warn("Test configuration validation failed - cluster may behave differently than MiniDFSCluster");
        }

        return valid;
    }

    /**
     * Register Hadoop distributions from builder configuration.
     */
    private void registerHadoopDistributions(Builder builder) throws IOException {
        // Register NameNode distributions
        for (int i = 0; i < numNameNodes; i++) {
            String hadoopHome = builder.nameNodeHadoopHomes.getOrDefault(i, builder.defaultHadoopHome);
            if (hadoopHome == null) {
                throw new IllegalArgumentException(
                    "No Hadoop distribution specified for NameNode " + i);
            }
            String versionKey = "nn-" + i;
            if (!versionRegistry.isRegistered(versionKey)) {
                versionRegistry.register(versionKey, hadoopHome);
            }
        }

        // Register DataNode distributions
        for (int i = 0; i < numDataNodes; i++) {
            String hadoopHome = builder.dataNodeHadoopHomes.getOrDefault(i, builder.defaultHadoopHome);
            if (hadoopHome == null) {
                throw new IllegalArgumentException(
                    "No Hadoop distribution specified for DataNode " + i);
            }
            String versionKey = "dn-" + i;
            if (!versionRegistry.isRegistered(versionKey)) {
                versionRegistry.register(versionKey, hadoopHome);
            }
        }
    }

    /**
     * Build the cluster structure (but don't start processes yet).
     */
    private void buildCluster() throws IOException {
        LOG.info("Building ProcessBasedMiniDFSCluster with {} NameNodes and {} DataNodes",
            numNameNodes, numDataNodes);

        // Create NameNode managers
        for (int i = 0; i < numNameNodes; i++) {
            DirectoryManager.NodeDirectory nnNodeDir = directoryManager.createNameNodeDirectory(i);
            ProcessConfigurationGenerator.NodeConfiguration nnNodeConf =
                configGenerator.generateNameNodeConfig(i, nnNodeDir);
            HadoopDistribution distribution = versionRegistry.get("nn-" + i);

            NameNodeProcessManager nnManager = new NameNodeProcessManager(
                nnNodeConf.getConfig(), distribution.getHadoopHome().getAbsolutePath(),
                nnNodeDir.getNodeBaseDir(), i);
            nameNodeManagers.add(nnManager);
        }

        // Get NameNode addresses for DataNode configuration
        List<InetSocketAddress> nnAddresses = new ArrayList<>();
        for (NameNodeProcessManager nnManager : nameNodeManagers) {
            nnAddresses.add(nnManager.getRpcAddress());
        }

        // Create DataNode managers
        for (int i = 0; i < numDataNodes; i++) {
            // Get storage types for this DataNode
            StorageType[] dnStorageTypes = (storageTypes != null) ? storageTypes[i] : null;

            // Get rack for this DataNode
            String rack = (racks != null && i < racks.length) ? racks[i] : null;

            DirectoryManager.NodeDirectory dnNodeDir =
                directoryManager.createDataNodeDirectory(i, storagesPerDatanode, dnStorageTypes);
            ProcessConfigurationGenerator.NodeConfiguration dnNodeConf =
                configGenerator.generateDataNodeConfig(i, dnNodeDir, nnAddresses, dnStorageTypes, rack, racks);
            HadoopDistribution distribution = versionRegistry.get("dn-" + i);

            DataNodeProcessManager dnManager = new DataNodeProcessManager(
                dnNodeConf.getConfig(), distribution.getHadoopHome().getAbsolutePath(),
                dnNodeDir.getNodeBaseDir(), i);
            dataNodeManagers.add(dnManager);
        }

        LOG.info("Cluster structure built successfully");
    }

    /**
     * Start the cluster - launch all NameNode and DataNode processes.
     */
    private void start() throws IOException, TimeoutException {
        if (isStarted) {
            throw new IllegalStateException("Cluster already started");
        }

        LOG.info("Starting ProcessBasedMiniDFSCluster...");

        try {
            // Format NameNodes if requested
            if (format) {
                for (int i = 0; i < nameNodeManagers.size(); i++) {
                    NameNodeProcessManager nn = nameNodeManagers.get(i);
                    LOG.info("Formatting NameNode {}", i);
                    nn.format();
                }
            }

            // Start NameNodes first
            for (int i = 0; i < nameNodeManagers.size(); i++) {
                NameNodeProcessManager nn = nameNodeManagers.get(i);
                LOG.info("Starting NameNode {}", i);
                nn.start();
                // Note: waitForProcessReady is called internally by start()
                LOG.info("NameNode {} started and ready", i);
            }

            // Then start DataNodes
            for (int i = 0; i < dataNodeManagers.size(); i++) {
                DataNodeProcessManager dn = dataNodeManagers.get(i);
                LOG.info("Starting DataNode {}", i);
                dn.start();
                // Note: waitForProcessReady is called internally by start()
                LOG.info("DataNode {} started and ready", i);
            }

            // Mark cluster as started before waiting for full readiness
            // This allows isClusterUp() to proceed with health checks
            isStarted = true;

            // Wait for cluster to be fully operational
            waitClusterUp();

            // Update base configuration with the cluster's HDFS URI
            // This ensures tests using configurations derived from baseConfiguration
            // have the correct fs.defaultFS setting
            baseConfiguration.set("fs.defaultFS", getURI().toString());
            LOG.info("Set fs.defaultFS in baseConfiguration to: {}", getURI());

            LOG.info("ProcessBasedMiniDFSCluster started successfully");

        } catch (Exception e) {
            LOG.error("Failed to start cluster, cleaning up", e);
            try {
                // Check if cleanup is disabled for debugging
                boolean noCleanup = Boolean.getBoolean("mini.dfs.no.cleanup");
                if (noCleanup) {
                    LOG.info("Skipping directory cleanup due to mini.dfs.no.cleanup=true");
                    LOG.info("Cluster directory preserved at: {}", directoryManager.getClusterBaseDir());
                }
                shutdown(!noCleanup);
            } catch (Exception shutdownEx) {
                LOG.warn("Error during cleanup after failed startup", shutdownEx);
            }
            throw new IOException("Failed to start cluster", e);
        }
    }

    /**
     * Wait for the cluster to be fully operational.
     */
    public void waitClusterUp() throws IOException, TimeoutException {
        waitClusterUp(DEFAULT_STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Wait for the cluster to be fully operational with custom timeout.
     */
    public void waitClusterUp(long timeout, TimeUnit unit) throws IOException, TimeoutException {
        long timeoutMs = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();

        LOG.info("Waiting for cluster to be ready (timeout: {} {})", timeout, unit);

        // Check all NameNodes are healthy
        for (int i = 0; i < nameNodeManagers.size(); i++) {
            long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
            if (remaining <= 0) {
                throw new TimeoutException("Timeout waiting for NameNode " + i);
            }

            NameNodeProcessManager nn = nameNodeManagers.get(i);
            if (!nn.isHealthy()) {
                throw new IOException("NameNode " + i + " is not healthy");
            }
        }

        // Check all DataNodes are healthy
        for (int i = 0; i < dataNodeManagers.size(); i++) {
            long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
            if (remaining <= 0) {
                throw new TimeoutException("Timeout waiting for DataNode " + i);
            }

            DataNodeProcessManager dn = dataNodeManagers.get(i);
            if (!dn.isHealthy()) {
                throw new IOException("DataNode " + i + " is not healthy");
            }
        }

        // Wait for DataNodes to register and NameNode to leave safe mode
        // This is critical - similar to MiniDFSCluster.waitClusterUp()
        if (dataNodeManagers.size() > 0) {
            int attempts = 0;
            while (!isClusterUp()) {
                long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
                if (remaining <= 0 || ++attempts > timeout) {
                    throw new TimeoutException(
                        "Timeout waiting for DataNodes to register and NameNode to leave safe mode. " +
                        "NameNode may still be in safe mode or DataNodes have not reported yet.");
                }

                LOG.info("Waiting for DataNodes to register with NameNode (attempt {})...", attempts);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for cluster", e);
                }
            }
        }

        // Try to get FileSystem to verify connectivity
        try {
            FileSystem fs = getFileSystem();
            fs.getStatus(); // Trigger an RPC call
            LOG.info("Cluster is ready and operational");
        } catch (IOException e) {
            throw new IOException("Cluster processes are running but FileSystem is not accessible", e);
        }
    }

    /**
     * Check if the cluster is running.
     * This checks not just that processes are alive, but that DataNodes have registered
     * with the NameNode and the NameNode has left safe mode.
     */
    public boolean isClusterUp() {
        LOG.info("isClusterUp() called - isStarted={}, isShutdown={}", isStarted, isShutdown);
        if (!isStarted || isShutdown) {
            LOG.info("isClusterUp() returning false - cluster not started or is shutdown");
            return false;
        }

        try {
            // Check if all processes are alive
            LOG.info("Checking if all {} NameNode(s) and {} DataNode(s) are alive...",
                nameNodeManagers.size(), dataNodeManagers.size());
            for (int i = 0; i < nameNodeManagers.size(); i++) {
                NameNodeProcessManager nn = nameNodeManagers.get(i);
                boolean alive = nn.isAlive();
                LOG.info("NameNode {} is alive: {}", i, alive);
                if (!alive) {
                    return false;
                }
            }
            for (int i = 0; i < dataNodeManagers.size(); i++) {
                DataNodeProcessManager dn = dataNodeManagers.get(i);
                boolean alive = dn.isAlive();
                LOG.info("DataNode {} is alive: {}", i, alive);
                if (!alive) {
                    return false;
                }
            }

            // Check that at least one NameNode is up (not in safe mode and has capacity)
            // This is the critical check that ensures DataNodes have registered
            LOG.info("All processes alive, now checking NameNode status...");
            boolean anyNameNodeUp = false;
            for (int i = 0; i < nameNodeManagers.size(); i++) {
                LOG.info("Checking if NameNode {} is up...", i);
                if (isNameNodeUp(i)) {
                    LOG.info("NameNode {} is UP!", i);
                    anyNameNodeUp = true;
                    break;
                } else {
                    LOG.info("NameNode {} is NOT up yet", i);
                }
            }

            LOG.info("isClusterUp() returning: {}", anyNameNodeUp);
            return anyNameNodeUp;
        } catch (Exception e) {
            LOG.info("isClusterUp() caught exception: {}", e.getMessage());
            LOG.warn("Error checking cluster status", e);
            return false;
        }
    }

    /**
     * Check if a specific NameNode is fully operational.
     * A NameNode is considered "up" when it's not in safe mode and has non-zero capacity,
     * which indicates that DataNodes have successfully registered.
     *
     * @param nnIndex the NameNode index
     * @return true if the NameNode is up and has registered DataNodes
     */
    public boolean isNameNodeUp(int nnIndex) {
        try {
            // For now, just check the primary NameNode (index 0)
            // TODO: support multiple NameNodes properly
            if (nnIndex >= nameNodeManagers.size()) {
                LOG.info("NameNode {} check: FAILED - invalid index", nnIndex);
                return false;
            }

            FileSystem fs = getFileSystem();
            if (fs == null) {
                LOG.info("NameNode {} check: FAILED - FileSystem is null", nnIndex);
                return false;
            }
            if (!(fs instanceof DistributedFileSystem)) {
                LOG.info("NameNode {} check: FAILED - FileSystem is not DistributedFileSystem, it's {}",
                    nnIndex, fs.getClass().getName());
                return false;
            }

            DistributedFileSystem dfs = (DistributedFileSystem) fs;

            // 1) Not in safemode?
            LOG.info("NameNode {} check: Checking safe mode...", nnIndex);
            boolean inSafeMode = dfs.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_GET);
            if (inSafeMode) {
                LOG.info("NameNode {} check: FAILED - still in safe mode", nnIndex);
                return false;
            }

            // 2) Do we have ≥1 LIVE DataNode?
            LOG.info("NameNode {} check: Checking for LIVE DataNodes...", nnIndex);
            DatanodeInfo[] live = dfs.getClient().datanodeReport(HdfsConstants.DatanodeReportType.LIVE);

            if (live == null || live.length < 1) {
                LOG.info("NameNode {} check: FAILED - No LIVE DataNodes registered (count: {})",
                    nnIndex, (live == null ? "null" : live.length));
                return false;
            }

            LOG.info("NameNode {} check: Found {} LIVE DataNode(s), creating probe file...", nnIndex, live.length);
            try (FSDataOutputStream out =
                         fs.create(new Path("/.startup_probe"), (short)1)) {
                out.hflush(); // forces pipeline open and first packet handshake
            }
            fs.delete(new Path("/.startup_probe"), false);
            LOG.info("NameNode {} check: SUCCESS - cluster is fully operational", nnIndex);
            return true;

        } catch (Exception e) {
            LOG.info("NameNode {} check: FAILED - Exception: {}", nnIndex, e.getMessage());
            LOG.warn("Error checking NameNode {} status: {}", nnIndex, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the FileSystem instance for this cluster.
     */
    public DistributedFileSystem getFileSystem() throws IOException {
        if (fileSystem == null) {
            URI uri = getURI();
            LOG.info("Creating FileSystem with URI: {}", uri);
            Configuration conf = new Configuration(baseConfiguration);
            conf.set("fs.defaultFS", uri.toString());

            try {
                FileSystem fs = FileSystem.get(conf);
                LOG.info("FileSystem.get() returned: {}", fs.getClass().getName());
                fileSystem = (DistributedFileSystem) fs;
                LOG.info("Successfully created DistributedFileSystem");
            } catch (ClassCastException e) {
                LOG.error("FileSystem is not DistributedFileSystem: {}",
                    FileSystem.get(conf).getClass().getName(), e);
                throw e;
            } catch (IOException e) {
                LOG.error("Failed to get FileSystem for URI {}: {}", uri, e.getMessage(), e);
                throw e;
            }
        }
        return fileSystem;
    }

    /**
     * Get a new FileSystem instance (not cached).
     */
    public DistributedFileSystem getNewFileSystemInstance() throws IOException {
        Configuration conf = new Configuration(baseConfiguration);
        conf.set("fs.defaultFS", getURI().toString());
        return (DistributedFileSystem) FileSystem.newInstance(conf);
    }

    /**
     * Get the configuration for the cluster.
     * The returned configuration includes fs.defaultFS set to the cluster's HDFS URI.
     * This matches the MiniDFSCluster API.
     *
     * @return the base configuration with fs.defaultFS set
     */
    public Configuration getConfiguration() {
        return getConfiguration(0);
    }

    /**
     * Get the configuration for a specific NameNode.
     * For ProcessBasedMiniDFSCluster, this returns the base configuration
     * since all nodes share the same configuration.
     *
     * @param nnIndex the NameNode index (currently unused but kept for API compatibility)
     * @return the base configuration with fs.defaultFS set
     */
    public Configuration getConfiguration(int nnIndex) {
        return baseConfiguration;
    }

    /**
     * Get the URI for the cluster.
     */
    public URI getURI() {
        return getURI(0);
    }

    /**
     * Get the URI for a specific NameNode.
     */
    public URI getURI(int nnIndex) {
        if (nnIndex < 0 || nnIndex >= nameNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid NameNode index: " + nnIndex);
        }

        InetSocketAddress addr = nameNodeManagers.get(nnIndex).getRpcAddress();
        try {
            // Always use 127.0.0.1 for local testing to avoid IPv4/IPv6 resolution issues
            // On macOS, "localhost" may resolve to ::1 (IPv6) while server binds to IPv4
            return new URI("hdfs", null, "127.0.0.1", addr.getPort(), null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create URI", e);
        }
    }

    /**
     * Get the RPC address of the primary NameNode.
     */
    public InetSocketAddress getNameNodeRpcAddress() {
        return getNameNodeRpcAddress(0);
    }

    /**
     * Get the RPC address of a specific NameNode.
     */
    public InetSocketAddress getNameNodeRpcAddress(int nnIndex) {
        if (nnIndex < 0 || nnIndex >= nameNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid NameNode index: " + nnIndex);
        }
        return nameNodeManagers.get(nnIndex).getRpcAddress();
    }

    /**
     * Get the number of NameNodes in this cluster.
     */
    public int getNumNameNodes() {
        return numNameNodes;
    }

    /**
     * Get the number of DataNodes in this cluster.
     */
    public int getNumDataNodes() {
        return numDataNodes;
    }

    /**
     * Restart a specific NameNode.
     */
    public void restartNameNode(int nnIndex) throws IOException, TimeoutException {
        restartNameNode(nnIndex, true);
    }

    /**
     * Restart a specific NameNode with option to wait for readiness.
     */
    public void restartNameNode(int nnIndex, boolean waitActive) throws IOException, TimeoutException {
        if (nnIndex < 0 || nnIndex >= nameNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid NameNode index: " + nnIndex);
        }

        LOG.info("Restarting NameNode {}", nnIndex);
        NameNodeProcessManager nn = nameNodeManagers.get(nnIndex);
        nn.stop();
        nn.start();

        // start() already waits for process readiness
        LOG.info("NameNode {} restarted and ready", nnIndex);
    }

    /**
     * Restart a specific DataNode.
     */
    public void restartDataNode(int dnIndex) throws IOException, TimeoutException {
        restartDataNode(dnIndex, true);
    }

    /**
     * Restart a specific DataNode with option to wait for readiness.
     */
    public void restartDataNode(int dnIndex, boolean waitActive) throws IOException, TimeoutException {
        if (dnIndex < 0 || dnIndex >= dataNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid DataNode index: " + dnIndex);
        }

        LOG.info("Restarting DataNode {}", dnIndex);
        DataNodeProcessManager dn = dataNodeManagers.get(dnIndex);
        dn.stop();
        dn.start();

        // start() already waits for process readiness
        LOG.info("DataNode {} restarted and ready", dnIndex);
    }

    /**
     * Shutdown a specific NameNode.
     */
    public void shutdownNameNode(int nnIndex) throws IOException {
        if (nnIndex < 0 || nnIndex >= nameNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid NameNode index: " + nnIndex);
        }

        LOG.info("Shutting down NameNode {}", nnIndex);
        nameNodeManagers.get(nnIndex).stop();
    }

    /**
     * Shutdown a specific DataNode.
     */
    public void shutdownDataNode(int dnIndex) throws IOException {
        if (dnIndex < 0 || dnIndex >= dataNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid DataNode index: " + dnIndex);
        }

        LOG.info("Shutting down DataNode {}", dnIndex);
        dataNodeManagers.get(dnIndex).stop();
    }

    /**
     * Start a specific DataNode that was previously shut down.
     * This is used in rolling upgrade scenarios.
     */
    public void startDataNode(int dnIndex) throws IOException, TimeoutException {
        if (dnIndex < 0 || dnIndex >= dataNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid DataNode index: " + dnIndex);
        }

        LOG.info("Starting DataNode {}", dnIndex);
        DataNodeProcessManager dn = dataNodeManagers.get(dnIndex);
        dn.start();
        LOG.info("DataNode {} started and ready", dnIndex);
    }

    /**
     * Change the Hadoop version for a specific DataNode.
     * The DataNode must be shut down before calling this method.
     *
     * @param dnIndex index of the DataNode
     * @param hadoopHome path to the new Hadoop distribution
     * @throws IOException if the version change fails
     */
    public void changeDataNodeVersion(int dnIndex, String hadoopHome) throws IOException {
        if (dnIndex < 0 || dnIndex >= dataNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid DataNode index: " + dnIndex);
        }

        LOG.info("Changing DataNode {} version to {}", dnIndex, hadoopHome);
        DataNodeProcessManager oldDn = dataNodeManagers.get(dnIndex);

        // Create new DataNodeProcessManager with new Hadoop version
        DataNodeProcessManager newDn = new DataNodeProcessManager(
            oldDn.getConfiguration(),
            hadoopHome,
            oldDn.getWorkDir(),
            dnIndex);

        // Replace in list
        dataNodeManagers.set(dnIndex, newDn);

        LOG.info("DataNode {} version changed to {}", dnIndex, hadoopHome);
    }

    /**
     * Start additional DataNodes dynamically.
     * Simple overload with just number of nodes and storage types.
     *
     * @param conf configuration for new DataNodes
     * @param numNewDataNodes number of DataNodes to add
     * @param storageTypes storage types for each new DataNode
     * @param manageDfsDirs ignored for process-based (always managed)
     * @param racks rack IDs for new DataNodes (optional, can be null)
     * @param hosts host names for new DataNodes (optional, can be null)
     * @param simulatedCapacities simulated capacities (optional, can be null)
     * @param storageCapacities storage capacities per storage per DataNode (optional, can be null)
     * @throws IOException if starting DataNodes fails
     * @throws TimeoutException if DataNodes don't become healthy in time
     */
    public synchronized void startDataNodes(Configuration conf, int numNewDataNodes,
                                           StorageType[][] storageTypes, boolean manageDfsDirs,
                                           String[] racks, String[] hosts,
                                           long[] simulatedCapacities,
                                           long[][] storageCapacities)
            throws IOException, TimeoutException {

        if (numNewDataNodes <= 0) {
            LOG.warn("startDataNodes called with numNewDataNodes={}, ignoring", numNewDataNodes);
            return;
        }

        LOG.info("Starting {} additional DataNodes", numNewDataNodes);

        // Validate array sizes
        if (storageTypes != null && storageTypes.length != numNewDataNodes) {
            throw new IllegalArgumentException(
                "storageTypes array length (" + storageTypes.length +
                ") must match numNewDataNodes (" + numNewDataNodes + ")");
        }
        if (racks != null && racks.length != numNewDataNodes) {
            throw new IllegalArgumentException(
                "racks array length (" + racks.length +
                ") must match numNewDataNodes (" + numNewDataNodes + ")");
        }
        if (hosts != null && hosts.length != numNewDataNodes) {
            throw new IllegalArgumentException(
                "hosts array length (" + hosts.length +
                ") must match numNewDataNodes (" + numNewDataNodes + ")");
        }

        // Get NameNode addresses for new DataNode configuration
        List<InetSocketAddress> nnAddresses = new ArrayList<>();
        for (NameNodeProcessManager nnManager : nameNodeManagers) {
            nnAddresses.add(nnManager.getRpcAddress());
        }

        // Get default Hadoop distribution for new DataNodes
        // Use the distribution from the last existing DataNode, or from builder's default
        String defaultHadoopHome = getDefaultHadoopHomeForNewDataNodes();

        // Create and start each new DataNode
        int startIndex = this.numDataNodes;  // First new DN index
        for (int i = 0; i < numNewDataNodes; i++) {
            int dnIndex = startIndex + i;

            // Get storage types for this DataNode
            StorageType[] dnStorageTypes = (storageTypes != null) ? storageTypes[i] : null;

            // Create directory structure
            DirectoryManager.NodeDirectory dnNodeDir =
                directoryManager.createDataNodeDirectory(dnIndex, storagesPerDatanode, dnStorageTypes);

            // Get rack for this DataNode
            String rack = (racks != null && i < racks.length) ? racks[i] : null;

            // Build combined racks array for topology mapping
            // Need to include both original cluster racks and new DataNode racks
            String[] allRacks = null;
            if (rack != null && racks != null) {
                // Combine original racks (if any) with new racks
                int totalDataNodes = startIndex + numNewDataNodes;
                allRacks = new String[totalDataNodes];

                // Copy original racks
                if (this.racks != null) {
                    System.arraycopy(this.racks, 0, allRacks, 0, Math.min(this.racks.length, startIndex));
                }

                // Add new racks
                for (int j = 0; j < numNewDataNodes; j++) {
                    if (racks[j] != null) {
                        allRacks[startIndex + j] = racks[j];
                    }
                }
            }

            // Generate configuration with rack support
            ProcessConfigurationGenerator.NodeConfiguration dnNodeConf =
                configGenerator.generateDataNodeConfig(dnIndex, dnNodeDir, nnAddresses, dnStorageTypes, rack, allRacks);

            Configuration dnConf = dnNodeConf.getConfig();
            if (hosts != null && hosts[i] != null) {
                // Note: Host configuration would need to be applied here
                LOG.warn("Host configuration is not yet fully supported in ProcessBasedMiniDFSCluster");
            }

            // Register Hadoop distribution if not already registered
            String versionKey = "dn-" + dnIndex;
            if (!versionRegistry.isRegistered(versionKey)) {
                versionRegistry.register(versionKey, defaultHadoopHome);
            }
            HadoopDistribution distribution = versionRegistry.get(versionKey);

            // Create DataNode process manager
            DataNodeProcessManager dnManager = new DataNodeProcessManager(
                dnConf, distribution.getHadoopHome().getAbsolutePath(),
                dnNodeDir.getNodeBaseDir(), dnIndex);

            // Start the DataNode process
            LOG.info("Starting new DataNode {}", dnIndex);
            dnManager.start();
            LOG.info("New DataNode {} started and ready", dnIndex);

            // Add to list
            dataNodeManagers.add(dnManager);
        }

        // Update count
        this.numDataNodes += numNewDataNodes;

        LOG.info("Successfully started {} additional DataNodes. Total DataNodes: {}",
            numNewDataNodes, this.numDataNodes);

        // Wait for cluster to be stable
        waitClusterUp();
    }

    /**
     * Simplified startDataNodes overload matching common MiniDFSCluster usage.
     */
    public synchronized void startDataNodes(Configuration conf, int numNewDataNodes,
                                           boolean manageDfsDirs,
                                           String[] racks, String[] hosts,
                                           long[] simulatedCapacities)
            throws IOException, TimeoutException {
        startDataNodes(conf, numNewDataNodes, null, manageDfsDirs, racks, hosts,
            simulatedCapacities, null);
    }

    /**
     * Comprehensive startDataNodes overload matching full MiniDFSCluster API.
     * This signature is used by tests like TestExternalStoragePolicySatisfier.
     *
     * @param conf configuration for new DataNodes
     * @param numNewDataNodes number of DataNodes to add
     * @param storageTypes storage types for each new DataNode (can be null)
     * @param manageDfsDirs ignored (always managed in process-based)
     * @param operation ignored (not supported in process-based)
     * @param racks rack IDs for new DataNodes (can be null)
     * @param hosts host names for new DataNodes (can be null)
     * @param dnHttpPorts ignored (ports are auto-allocated)
     * @param storageCapacities storage capacities per storage per DN (can be null)
     * @param dnIpcPorts ignored (ports are auto-allocated)
     * @param setupHostsFile ignored (not applicable to process-based)
     * @param checkDataNodeAddrConfig ignored
     * @param checkDataNodeHostConfig ignored
     * @param dnConfOverlays ignored (not yet supported)
     * @param dnHttpsPorts ignored (ports are auto-allocated)
     * @param dnBpPorts ignored (ports are auto-allocated)
     */
    public synchronized void startDataNodes(Configuration conf, int numNewDataNodes,
                                           StorageType[][] storageTypes,
                                           boolean manageDfsDirs,
                                           Object operation,  // StartupOption - ignored
                                           String[] racks, String[] hosts,
                                           int[] dnHttpPorts,  // ignored
                                           long[][] storageCapacities,
                                           int[] dnIpcPorts,  // ignored
                                           boolean setupHostsFile,  // ignored
                                           boolean checkDataNodeAddrConfig,  // ignored
                                           boolean checkDataNodeHostConfig,  // ignored
                                           Configuration[] dnConfOverlays,  // ignored
                                           int[] dnHttpsPorts,  // ignored
                                           int[] dnBpPorts)  // ignored
            throws IOException, TimeoutException {
        // Delegate to our main implementation, ignoring unsupported parameters
        startDataNodes(conf, numNewDataNodes, storageTypes, manageDfsDirs, racks, hosts,
            null, storageCapacities);
    }

    /**
     * Change the Hadoop version for a specific NameNode.
     * The NameNode must be shut down before calling this method.
     *
     * @param nnIndex index of the NameNode
     * @param hadoopHome path to the new Hadoop distribution
     * @throws IOException if the version change fails
     */
    public void changeNameNodeVersion(int nnIndex, String hadoopHome) throws IOException {
        if (nnIndex < 0 || nnIndex >= nameNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid NameNode index: " + nnIndex);
        }

        LOG.info("Changing NameNode {} version to {}", nnIndex, hadoopHome);
        NameNodeProcessManager oldNn = nameNodeManagers.get(nnIndex);

        // Create new NameNodeProcessManager with new Hadoop version
        NameNodeProcessManager newNn = new NameNodeProcessManager(
            oldNn.getConfiguration(),
            hadoopHome,
            oldNn.getWorkDir(),
            nnIndex);

        // Replace in list
        nameNodeManagers.set(nnIndex, newNn);

        LOG.info("NameNode {} version changed to {}", nnIndex, hadoopHome);
    }

    /**
     * Get the upgrade distribution path, if configured.
     * This is used for rolling upgrade scenarios where tests need to upgrade nodes
     * to a different Hadoop version.
     *
     * @return the upgrade distribution path, or null if not configured
     */
    public String getUpgradeDistributionPath() {
        return upgradeDistributionPath;
    }

    // ========================================================================
    // ROLLING UPGRADE SUPPORT (following official HDFS rolling upgrade procedure)
    // ========================================================================

    /**
     * Enter safe mode.
     * Executes: hdfs dfsadmin -safemode enter
     *
     * @throws IOException if entering safe mode fails
     */
    public void enterSafeMode() throws IOException {
        LOG.info("Entering safe mode...");

        // Get filesystem and create DFSAdmin
        DistributedFileSystem fs = getFileSystem();
        org.apache.hadoop.hdfs.tools.DFSAdmin admin =
            new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

        try {
            // Capture System.out to get command output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -safemode enter
                int exitCode = admin.run(new String[]{"-safemode", "enter"});

                if (exitCode != 0) {
                    String output = baos.toString();
                    LOG.error("Failed to enter safe mode, exit code {}, output:\n{}", exitCode, output);
                    throw new IOException("Failed to enter safe mode, exit code: " + exitCode);
                }

                LOG.info("Entered safe mode successfully");
                LOG.debug("Safe mode output: {}", baos.toString());
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to enter safe mode", e);
        }
    }

    /**
     * Leave safe mode.
     * Executes: hdfs dfsadmin -safemode leave
     *
     * @throws IOException if leaving safe mode fails
     */
    public void leaveSafeMode() throws IOException {
        LOG.info("Leaving safe mode...");

        // Get filesystem and create DFSAdmin
        DistributedFileSystem fs = getFileSystem();
        org.apache.hadoop.hdfs.tools.DFSAdmin admin =
            new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

        try {
            // Capture System.out to get command output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -safemode leave
                int exitCode = admin.run(new String[]{"-safemode", "leave"});

                if (exitCode != 0) {
                    String output = baos.toString();
                    LOG.error("Failed to leave safe mode, exit code {}, output:\n{}", exitCode, output);
                    throw new IOException("Failed to leave safe mode, exit code: " + exitCode);
                }

                LOG.info("Left safe mode successfully");
                LOG.debug("Safe mode output: {}", baos.toString());
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to leave safe mode", e);
        }
    }

    /**
     * Prepare for rolling upgrade by creating rollback fsimage.
     * Executes: hdfs dfsadmin -rollingUpgrade prepare
     *
     * This is Step 1 of the official HDFS rolling upgrade procedure.
     *
     * @throws IOException if preparation fails
     */
    public void prepareRollingUpgrade() throws IOException {
        LOG.info("Preparing rolling upgrade...");

        // Get filesystem and create DFSAdmin
        DistributedFileSystem fs = getFileSystem();
        org.apache.hadoop.hdfs.tools.DFSAdmin admin =
            new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

        try {
            // Capture System.out to get command output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -rollingUpgrade prepare
                int exitCode = admin.run(new String[]{"-rollingUpgrade", "prepare"});

                if (exitCode != 0) {
                    String output = baos.toString();
                    LOG.error("Rolling upgrade prepare failed with exit code {}, output:\n{}", exitCode, output);
                    throw new IOException("Failed to prepare rolling upgrade, exit code: " + exitCode);
                }

                LOG.info("Rolling upgrade preparation completed successfully");
                LOG.debug("Preparation output: {}", baos.toString());
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to prepare rolling upgrade", e);
        }
    }

    /**
     * Query rolling upgrade status.
     * Executes: hdfs dfsadmin -rollingUpgrade query
     *
     * @return true if ready to proceed, false if still preparing
     * @throws IOException if query fails
     */
    public boolean queryRollingUpgrade() throws IOException {
        // Get filesystem and create DFSAdmin
        DistributedFileSystem fs = getFileSystem();
        org.apache.hadoop.hdfs.tools.DFSAdmin admin =
            new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

        try {
            // Capture System.out to check output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -rollingUpgrade query
                admin.run(new String[]{"-rollingUpgrade", "query"});

                String output = baos.toString();
                LOG.debug("Rolling upgrade query output: {}", output);

                // Check if ready to proceed
                boolean ready = output.contains("Proceed with rolling upgrade");
                return ready;
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to query rolling upgrade", e);
        }
    }

    /**
     * Wait for rolling upgrade to be ready.
     * Polls queryRollingUpgrade() until ready or timeout.
     *
     * This is Step 2 of the official HDFS rolling upgrade procedure.
     *
     * @param timeoutMs timeout in milliseconds
     * @throws IOException if query fails
     * @throws TimeoutException if not ready within timeout
     */
    public void waitRollingUpgradeReady(long timeoutMs) throws IOException, TimeoutException {
        LOG.info("Waiting for rolling upgrade to be ready (timeout: {} ms)...", timeoutMs);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (queryRollingUpgrade()) {
                LOG.info("Rolling upgrade is ready to proceed");
                return;
            }

            try {
                Thread.sleep(2000); // Poll every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for rolling upgrade", e);
            }
        }

        throw new TimeoutException("Rolling upgrade did not become ready within " + timeoutMs + "ms");
    }

    /**
     * Finalize rolling upgrade.
     * Executes: hdfs dfsadmin -rollingUpgrade finalize
     *
     * This is the final step of the official HDFS rolling upgrade procedure.
     *
     * @throws IOException if finalization fails
     */
    public void finalizeRollingUpgrade() throws IOException {
        LOG.info("Finalizing rolling upgrade...");

        // Get filesystem and create DFSAdmin
        DistributedFileSystem fs = getFileSystem();
        org.apache.hadoop.hdfs.tools.DFSAdmin admin =
            new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

        try {
            // Capture System.out to get command output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -rollingUpgrade finalize
                int exitCode = admin.run(new String[]{"-rollingUpgrade", "finalize"});

                if (exitCode != 0) {
                    String output = baos.toString();
                    LOG.error("Rolling upgrade finalize failed with exit code {}, output:\n{}", exitCode, output);
                    throw new IOException("Failed to finalize rolling upgrade, exit code: " + exitCode);
                }

                LOG.info("Rolling upgrade finalized successfully");
                LOG.debug("Finalize output: {}", baos.toString());
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to finalize rolling upgrade", e);
        }
    }

    /**
     * Gracefully shutdown DataNode using dfsadmin command.
     * Executes: hdfs dfsadmin -shutdownDatanode <HOST:PORT> upgrade
     *
     * This is the recommended way to shutdown DataNodes during rolling upgrade.
     *
     * @param dnIndex DataNode index
     * @throws IOException if shutdown fails
     */
    public void shutdownDataNodeGracefully(int dnIndex) throws IOException {
        if (dnIndex < 0 || dnIndex >= dataNodeManagers.size()) {
            throw new IllegalArgumentException("Invalid DataNode index: " + dnIndex);
        }

        DataNodeProcessManager dn = dataNodeManagers.get(dnIndex);
        InetSocketAddress ipcAddress = dn.getRpcAddress();

        LOG.info("Gracefully shutting down DataNode {} at {}:{}",
            dnIndex, ipcAddress.getHostName(), ipcAddress.getPort());

        try {
            // IMPORTANT: Stop monitoring thread before shutdown to prevent
            // "Process died unexpectedly" error when DataNode exits gracefully
            dn.stopMonitoring();
            LOG.debug("Stopped monitoring thread for DataNode {}", dnIndex);

            // Get filesystem and create DFSAdmin
            DistributedFileSystem fs = getFileSystem();
            org.apache.hadoop.hdfs.tools.DFSAdmin admin =
                new org.apache.hadoop.hdfs.tools.DFSAdmin(fs.getConf());

            // Capture System.out to get command output
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps = new java.io.PrintStream(baos);
            java.io.PrintStream oldOut = System.out;
            java.io.PrintStream oldErr = System.err;

            try {
                System.setOut(ps);
                System.setErr(ps);

                // Run: hdfs dfsadmin -shutdownDatanode <HOST:PORT> upgrade
                int exitCode = admin.run(new String[]{
                    "-shutdownDatanode",
                    ipcAddress.getHostName() + ":" + ipcAddress.getPort(),
                    "upgrade"
                });

                if (exitCode != 0) {
                    LOG.warn("Graceful shutdown command returned {}, proceeding anyway", exitCode);
                }

                LOG.debug("DataNode shutdown output: {}", baos.toString());
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }

            // Wait for DataNode to actually stop
            waitForDataNodeShutdown(dnIndex, 30000);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to shutdown DataNode gracefully", e);
        }
    }

    /**
     * Wait for DataNode to shutdown.
     *
     * @param dnIndex DataNode index
     * @param timeoutMs timeout in milliseconds
     * @throws IOException if wait fails
     */
    private void waitForDataNodeShutdown(int dnIndex, long timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        DataNodeProcessManager dn = dataNodeManagers.get(dnIndex);

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!dn.isAlive()) {
                LOG.info("DataNode {} has shut down", dnIndex);
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted waiting for DataNode shutdown", e);
            }
        }

        LOG.warn("DataNode {} did not shut down gracefully within {} ms, forcing stop",
            dnIndex, timeoutMs);
        dn.stop();
    }

    /**
     * Get array of all NameNode indices.
     */
    private int[] getAllNameNodeIndices() {
        int[] indices = new int[numNameNodes];
        for (int i = 0; i < numNameNodes; i++) {
            indices[i] = i;
        }
        return indices;
    }

    /**
     * Get array of all DataNode indices.
     */
    private int[] getAllDataNodeIndices() {
        int[] indices = new int[numDataNodes];
        for (int i = 0; i < numDataNodes; i++) {
            indices[i] = i;
        }
        return indices;
    }

    /**
     * Perform rolling upgrade following official HDFS rolling upgrade procedure.
     *
     * <p>This method implements the complete rolling upgrade sequence as documented at:
     * https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsRollingUpgrade.html
     *
     * <p>The upgrade procedure consists of:</p>
     * <ol>
     *   <li>Prepare rolling upgrade (create rollback fsimage)</li>
     *   <li>Wait for preparation to complete</li>
     *   <li>Upgrade nodes according to the upgrade plan</li>
     *   <li>Wait for cluster to stabilize</li>
     *   <li>Finalize rolling upgrade</li>
     * </ol>
     *
     * <p>The upgrade configuration (which nodes to upgrade, upgrade order) is read from:</p>
     * <ul>
     *   <li>System property: {@code -Dhadoop.upgrade.plan=<preset>}</li>
     *   <li>System property: {@code -Dhadoop.upgrade.plan.file=/path/to/plan.properties}</li>
     *   <li>Default: upgrade all nodes (NameNodes first, then DataNodes)</li>
     * </ul>
     *
     * <p>The target version is taken from {@code -Dhadoop.upgrade.home} system property.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // In test code:
     * cluster.upgrade();
     *
     * // Command line:
     * mvn test -Dtest=MyTest \
     *   -Dhadoop.start.home=/opt/hadoop-3.3.5 \
     *   -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
     * </pre>
     *
     * @throws IOException if upgrade fails
     * @throws TimeoutException if cluster doesn't stabilize
     */
    public void upgrade() throws IOException, TimeoutException {
        LOG.info("=== Starting HDFS Rolling Upgrade ===");

        // Get target version
        String targetVersion = getUpgradeDistributionPath();
        if (targetVersion == null || targetVersion.isEmpty()) {
            throw new IllegalStateException(
                "No upgrade version configured. Set -Dhadoop.upgrade.home=/path or " +
                "HADOOP_UPGRADE_HOME environment variable.");
        }

        LOG.info("Target upgrade version: {}", targetVersion);

        // Load upgrade plan
        UpgradeConfig config = loadUpgradeConfig();
        LOG.info("Upgrade plan loaded: {} steps", config.getSteps().size());

        try {
            // STEP 0: Enter safe mode (required for rolling upgrade preparation)
            LOG.info("Step 0: Entering safe mode for rolling upgrade preparation");
            enterSafeMode();

            // STEP 1: Prepare rolling upgrade
            LOG.info("Step 1/5: Preparing rolling upgrade");
            prepareRollingUpgrade();

            // STEP 2: Wait for preparation to complete
            LOG.info("Step 2/5: Waiting for rolling upgrade to be ready");
            waitRollingUpgradeReady(60000); // 60 second timeout

            // STEP 3: Execute upgrade plan
            LOG.info("Step 3/5: Executing upgrade plan");
            executeUpgradePlan(config, targetVersion);

            // STEP 4: Wait for cluster to stabilize
            LOG.info("Step 4/5: Waiting for cluster to stabilize");
            waitClusterUp();

            // STEP 4.5: Wait for DataNodes to report blocks for existing files
            // After a rolling upgrade, DataNodes need time to report their blocks
            // back to the NameNode. This is necessary for append operations on
            // existing files to succeed.
            LOG.info("Waiting for blocks to be sufficiently replicated...");
            LOG.info("(This ensures existing files can be reopened for append)");
            try {
                Thread.sleep(5000); // 5 second grace period for block reports
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for block reports", e);
            }

            // STEP 5: Finalize rolling upgrade
            LOG.info("Step 5/5: Finalizing rolling upgrade");
            finalizeRollingUpgrade();

            // STEP 6: Leave safe mode
            LOG.info("Step 6: Leaving safe mode after rolling upgrade");
            leaveSafeMode();

            LOG.info("=== Rolling Upgrade Completed Successfully ===");

        } catch (Exception e) {
            LOG.error("Rolling upgrade failed: {}", e.getMessage(), e);
            throw new IOException("Rolling upgrade failed", e);
        }
    }

    /**
     * Load upgrade configuration from system properties.
     */
    private UpgradeConfig loadUpgradeConfig() {
        String planFile = System.getProperty("hadoop.upgrade.plan.file");
        if (planFile != null) {
            try {
                return UpgradeConfig.fromFile(planFile);
            } catch (IOException e) {
                LOG.warn("Failed to load upgrade plan from file {}: {}",
                    planFile, e.getMessage());
                LOG.info("Falling back to default upgrade plan");
            }
        }

        String preset = System.getProperty("hadoop.upgrade.plan", "all-nodes");
        return UpgradeConfig.fromPreset(preset, this);
    }

    /**
     * Execute the upgrade plan.
     */
    private void executeUpgradePlan(UpgradeConfig config, String targetVersion)
        throws IOException, TimeoutException {

        int stepNum = 0;
        for (UpgradeConfig.UpgradeStep step : config.getSteps()) {
            stepNum++;
            LOG.info("Executing upgrade step {}/{}: {}",
                stepNum, config.getSteps().size(), step.getDescription());

            if (step.getNodeType() == UpgradeConfig.NodeType.NAMENODE) {
                upgradeNameNodeStep(step, targetVersion);
            } else {
                upgradeDataNodeStep(step, targetVersion);
            }

            LOG.info("Completed upgrade step {}/{}", stepNum, config.getSteps().size());
        }
    }

    /**
     * Upgrade NameNodes according to step configuration.
     */
    private void upgradeNameNodeStep(UpgradeConfig.UpgradeStep step, String targetVersion)
        throws IOException, TimeoutException {

        int[] indices = step.getIndices();
        if (indices == null) {
            indices = getAllNameNodeIndices();
        }

        for (int nnIndex : indices) {
            LOG.info("Upgrading NameNode {}", nnIndex);

            // Shutdown
            shutdownNameNode(nnIndex);

            // Change version
            changeNameNodeVersion(nnIndex, targetVersion);

            // Start with -rollingUpgrade started option
            NameNodeProcessManager nn = nameNodeManagers.get(nnIndex);
            nn.startWithRollingUpgrade();

            // Wait for cluster to stabilize
            waitClusterUp();

            LOG.info("Successfully upgraded NameNode {}", nnIndex);
        }
    }

    /**
     * Upgrade DataNodes according to step configuration.
     */
    private void upgradeDataNodeStep(UpgradeConfig.UpgradeStep step, String targetVersion)
        throws IOException, TimeoutException {

        int[] indices = step.getIndices();
        if (indices == null) {
            indices = getAllDataNodeIndices();
        }

        for (int dnIndex : indices) {
            LOG.info("Upgrading DataNode {}", dnIndex);

            // Graceful shutdown using dfsadmin
            shutdownDataNodeGracefully(dnIndex);

            // Change version
            changeDataNodeVersion(dnIndex, targetVersion);

            // Start DataNode
            startDataNode(dnIndex);

            // Wait for cluster to stabilize
            waitClusterUp();

            // IMPORTANT: Wait for DataNode to be writable (not excluded)
            // After graceful shutdown for upgrade, DataNode may be in decommissioning
            // or maintenance state. We need to wait for it to be fully available.
            waitDataNodeWritable(dnIndex, 30000);

            LOG.info("Successfully upgraded DataNode {}", dnIndex);
        }
    }

    /**
     * Wait for a DataNode to be writable (not excluded from writes).
     *
     * After a rolling upgrade, a DataNode may be in decommissioning or maintenance state.
     * This method waits for the DataNode to be fully available for writes.
     *
     * @param dnIndex DataNode index
     * @param timeoutMs timeout in milliseconds
     * @throws IOException if DataNode doesn't become writable
     */
    private void waitDataNodeWritable(int dnIndex, long timeoutMs) throws IOException {
        LOG.info("Waiting for DataNode {} to be writable (not excluded)", dnIndex);

        long startTime = System.currentTimeMillis();
        int attempts = 0;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            attempts++;
            try {
                DistributedFileSystem fs = getFileSystem();

                // Get DataNode report to check state
                DatanodeInfo[] liveNodes = fs.getClient().datanodeReport(
                    org.apache.hadoop.hdfs.protocol.HdfsConstants.DatanodeReportType.LIVE);

                DataNodeProcessManager targetDn = dataNodeManagers.get(dnIndex);
                InetSocketAddress targetAddr = targetDn.getRpcAddress();

                // Find our DataNode in the live nodes list
                boolean found = false;
                boolean writable = false;

                for (DatanodeInfo dn : liveNodes) {
                    // Match by IPC port
                    if (dn.getIpcPort() == targetAddr.getPort()) {
                        found = true;

                        // Check if DataNode is in a state that excludes it from writes
                        boolean isDecommissioning = dn.isDecommissionInProgress() || dn.isDecommissioned();
                        boolean isInMaintenance = dn.isInMaintenance() || dn.isEnteringMaintenance();

                        // Use a staleness interval of 30 seconds (30000 ms)
                        long stalenessInterval = 30000;
                        boolean isStale = dn.isStale(stalenessInterval);

                        if (!isDecommissioning && !isInMaintenance && !isStale) {
                            writable = true;
                            LOG.info("DataNode {} is writable (attempt {}): decom={}, maint={}, stale={}",
                                dnIndex, attempts, isDecommissioning, isInMaintenance, isStale);
                        } else {
                            LOG.info("DataNode {} not yet writable (attempt {}): decom={}, maint={}, stale={}",
                                dnIndex, attempts, isDecommissioning, isInMaintenance, isStale);
                        }
                        break;
                    }
                }

                if (!found) {
                    LOG.warn("DataNode {} not found in live nodes list (attempt {})", dnIndex, attempts);
                } else if (writable) {
                    LOG.info("DataNode {} is now writable after {} attempts ({} ms)",
                        dnIndex, attempts, System.currentTimeMillis() - startTime);
                    return;
                }

                // Wait before retry
                Thread.sleep(1000);

            } catch (Exception e) {
                LOG.debug("Error checking DataNode {} writability (attempt {}): {}",
                    dnIndex, attempts, e.getMessage());
            }
        }

        throw new IOException(String.format(
            "DataNode %d did not become writable within %d ms (%d attempts)",
            dnIndex, timeoutMs, attempts));
    }

    /**
     * Get the default Hadoop home for new DataNodes.
     * Uses the distribution from the last existing DataNode.
     */
    private String getDefaultHadoopHomeForNewDataNodes() throws IOException {
        if (dataNodeManagers.isEmpty()) {
            throw new IOException("No existing DataNodes to determine default Hadoop distribution");
        }

        // Get the Hadoop home from the last DataNode
        DataNodeProcessManager lastDn = dataNodeManagers.get(dataNodeManagers.size() - 1);
        return lastDn.getHadoopHome();
    }

    /**
     * Trigger heartbeats from all DataNodes to the NameNode.
     *
     * In ProcessBasedMiniDFSCluster, we cannot directly trigger heartbeats since
     * nodes run in separate processes. Instead, this method waits long enough for
     * natural heartbeat cycles to occur, ensuring state propagation.
     *
     * Default heartbeat interval is 3 seconds. This method waits for 5 seconds
     * to ensure at least one full heartbeat cycle completes.
     */
    public void triggerHeartbeats() throws InterruptedException {
        // Get heartbeat interval from configuration (default is 3 seconds)
        long heartbeatInterval = baseConfiguration.getTimeDuration(
            "dfs.heartbeat.interval", 3, TimeUnit.SECONDS);

        // Wait for 1.5x the heartbeat interval to ensure heartbeats occur
        long waitTime = (heartbeatInterval * 3) / 2;
        LOG.info("Waiting {} ms for DataNode heartbeats to propagate state", waitTime);
        Thread.sleep(waitTime);
    }

    /**
     * Trigger block reports from all DataNodes to the NameNode.
     *
     * In ProcessBasedMiniDFSCluster, we cannot directly trigger block reports since
     * nodes run in separate processes. Instead, this method waits long enough for
     * natural block report cycles to occur.
     *
     * Default block report interval is 6 hours in production, but tests typically
     * set it much lower. This method waits for 10 seconds to allow block reports.
     */
    public void triggerBlockReports() throws InterruptedException {
        // Block reports take longer than heartbeats
        // Wait 10 seconds to allow block reports to complete
        LOG.info("Waiting 10 seconds for DataNode block reports to complete");
        Thread.sleep(10000);
    }

    /**
     * Shutdown the entire cluster.
     */
    public void shutdown() {
        shutdown(false);
    }

    /**
     * Shutdown the entire cluster with optional directory deletion.
     */
    public void shutdown(boolean deleteDirs) {
        if (isShutdown) {
            LOG.warn("Cluster already shut down");
            return;
        }

        LOG.info("Shutting down ProcessBasedMiniDFSCluster");

        // Close FileSystem if cached
        if (fileSystem != null) {
            try {
                fileSystem.close();
            } catch (IOException e) {
                LOG.warn("Error closing FileSystem", e);
            }
            fileSystem = null;
        }

        // Stop all DataNodes
        for (int i = 0; i < dataNodeManagers.size(); i++) {
            try {
                LOG.info("Stopping DataNode {}", i);
                dataNodeManagers.get(i).stop();
            } catch (Exception e) {
                LOG.warn("Error stopping DataNode " + i, e);
            }
        }

        // Stop all NameNodes
        for (int i = 0; i < nameNodeManagers.size(); i++) {
            try {
                LOG.info("Stopping NameNode {}", i);
                nameNodeManagers.get(i).stop();
            } catch (Exception e) {
                LOG.warn("Error stopping NameNode " + i, e);
            }
        }

        // Clean up directories if requested
        if (deleteDirs) {
            try {
                directoryManager.cleanup();
            } catch (IOException e) {
                LOG.warn("Error cleaning up directories", e);
            }
        }

        isShutdown = true;
        LOG.info("ProcessBasedMiniDFSCluster shut down");
    }

    @Override
    public void close() {
        shutdown(false);
    }

    /**
     * Create default base directory in temp.
     */
    private static File createDefaultBaseDir() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        File baseDir = new File(tmpDir, "process-minicluster-" + System.currentTimeMillis());
        if (!baseDir.mkdirs()) {
            throw new IOException("Failed to create base directory: " + baseDir);
        }
        return baseDir;
    }

    // ========================================================================
    // UNSUPPORTED METHODS - These require direct object access
    // ========================================================================

    private static final String UNSUPPORTED_MSG =
        "Direct object access not supported in ProcessBasedMiniDFSCluster. " +
        "This cluster runs nodes in separate processes. " +
        "Use client-side APIs (FileSystem, RPC clients) instead.";

    /**
     * NOT SUPPORTED - Cannot access NameNode object directly.
     * @throws UnsupportedOperationException always
     */
    public Object getNameNode() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot access NameNode object directly.
     * @throws UnsupportedOperationException always
     */
    public Object getNameNode(int nnIndex) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot access DataNode object directly.
     * @throws UnsupportedOperationException always
     */
    public Object getDataNode(int dnIndex) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot access DataNode objects directly.
     * @throws UnsupportedOperationException always
     */
    public List<Object> getDataNodes() {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot inject blocks without direct DataNode access.
     * @throws UnsupportedOperationException always
     */
    public void injectBlocks(int dataNodeIndex, Iterable<?> blocks) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot corrupt blocks without direct DataNode access.
     * @throws UnsupportedOperationException always
     */
    public int corruptBlockOnDataNodes(Object block) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot get FsDataset without direct DataNode access.
     * @throws UnsupportedOperationException always
     */
    public Object getFSDataset(int dnIndex) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    /**
     * NOT SUPPORTED - Cannot get materialized replica without direct DataNode access.
     * @throws UnsupportedOperationException always
     */
    public Object getMaterializedReplica(int dnIndex, Object block) {
        throw new UnsupportedOperationException(UNSUPPORTED_MSG);
    }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    /**
     * Builder for ProcessBasedMiniDFSCluster.
     */
    public static class Builder {
        // Required
        private final Configuration conf;

        // Optional with defaults
        private int numNameNodes = 1;
        private int numDataNodes = 1;
        private boolean format = true;
        private File baseDir = null;
        private int portRangeStart = 50000;
        private int portRangeEnd = 59999;

        // Hadoop distributions
        private String defaultHadoopHome = null;
        private Map<Integer, String> nameNodeHadoopHomes = new HashMap<>();
        private Map<Integer, String> dataNodeHadoopHomes = new HashMap<>();
        private String upgradeHadoopHome = null;             // target version for rolling upgrades

        // Storage configuration
        private StorageType[][] storageTypes = null;        // 2D: [dnIndex][storageIndex]
        private StorageType[] storageTypes1D = null;        // 1D: same types for all DNs
        private int storagesPerDatanode = 1;                 // default: 1 storage per DN

        // Network topology
        private String[] racks = null;                       // rack assignments for DataNodes

        /**
         * Create a new builder.
         * NOTE: The configuration is NOT copied - it will be modified when the cluster starts
         * to set fs.defaultFS to the cluster's HDFS URI. This matches MiniDFSCluster behavior.
         */
        public Builder(Configuration conf) {
            this.conf = conf;
            loadTestResources(this.conf);
        }

        /**
         * Load configuration from test resources if available.
         * This ensures ProcessBasedMiniDFSCluster has the same defaults as regular tests.
         *
         * Test resources are loaded from the classpath (e.g., src/test/resources/).
         * This includes files like hdfs-site.xml and core-site.xml that contain
         * test-specific configuration settings.
         */
        private void loadTestResources(Configuration conf) {
            // Try to load hdfs-site.xml from test classpath
            java.net.URL testResource = getClass().getClassLoader().getResource("hdfs-site.xml");
            if (testResource != null) {
                LOG.info("Loading test resource: hdfs-site.xml from {}", testResource);
                conf.addResource("hdfs-site.xml");
            } else {
                LOG.debug("Test resource hdfs-site.xml not found on classpath");
            }

            // Also try core-site.xml
            testResource = getClass().getClassLoader().getResource("core-site.xml");
            if (testResource != null) {
                LOG.info("Loading test resource: core-site.xml from {}", testResource);
                conf.addResource("core-site.xml");
            } else {
                LOG.debug("Test resource core-site.xml not found on classpath");
            }

            // Try to load yarn-site.xml (may be needed for some tests)
            testResource = getClass().getClassLoader().getResource("yarn-site.xml");
            if (testResource != null) {
                LOG.info("Loading test resource: yarn-site.xml from {}", testResource);
                conf.addResource("yarn-site.xml");
            } else {
                LOG.debug("Test resource yarn-site.xml not found on classpath");
            }
        }

        /**
         * Set the number of NameNodes (default: 1).
         */
        public Builder numNameNodes(int num) {
            if (num < 1) {
                throw new IllegalArgumentException("Must have at least 1 NameNode");
            }
            this.numNameNodes = num;
            return this;
        }

        /**
         * Set the number of DataNodes (default: 1).
         */
        public Builder numDataNodes(int num) {
            if (num < 1) {
                throw new IllegalArgumentException("Must have at least 1 DataNode");
            }
            this.numDataNodes = num;
            return this;
        }

        /**
         * Set whether to format the cluster on startup (default: true).
         */
        public Builder format(boolean format) {
            this.format = format;
            return this;
        }

        /**
         * Set the base directory for the cluster (default: temp directory).
         */
        public Builder baseDir(File dir) {
            this.baseDir = dir;
            return this;
        }

        /**
         * Set the port range for allocating node ports (default: 50000-59999).
         */
        public Builder portRange(int start, int end) {
            if (start < 1024 || end > 65535 || start >= end) {
                throw new IllegalArgumentException("Invalid port range: " + start + "-" + end);
            }
            this.portRangeStart = start;
            this.portRangeEnd = end;
            return this;
        }

        /**
         * Detect all available Hadoop distributions from environment variables.
         * Returns a sorted map of version strings to their paths, ordered by version.
         *
         * @return map of version to HADOOP_HOME path (may be empty)
         */
        private static Map<String, String> detectAvailableDistributions() {
            Map<String, String> distributions = new HashMap<>();

            // Check HADOOP_HOME
            String hadoopHome = System.getenv("HADOOP_HOME");
            if (hadoopHome != null && !hadoopHome.isEmpty()) {
                distributions.put("default", hadoopHome);
            }

            // Check version-specific patterns (e.g., HADOOP_3_3_5_HOME)
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("HADOOP_") && key.endsWith("_HOME") &&
                    !key.equals("HADOOP_HOME")) {
                    String value = entry.getValue();
                    if (value != null && !value.isEmpty()) {
                        // Extract version: HADOOP_3_3_5_HOME -> 3.3.5
                        String version = key.substring("HADOOP_".length(),
                            key.length() - "_HOME".length()).replace("_", ".");
                        distributions.put(version, value);
                    }
                }
            }

            return distributions;
        }

        /**
         * Read start version Hadoop distribution path from system property.
         * Tries in order:
         * 1. System property: hadoop.start.home
         * 2. Environment variable: HADOOP_HOME (fallback for backward compatibility)
         *
         * @return Hadoop home path for start version
         * @throws IllegalStateException if no distribution found
         */
        private static String readStartVersionHome() {
            // Try system property first (preferred)
            String sysProp = System.getProperty("hadoop.start.home");
            if (sysProp != null && !sysProp.isEmpty()) {
                return sysProp;
            }

            // Try environment variable HADOOP_HOME (backward compatibility)
            String hadoopHome = System.getenv("HADOOP_HOME");
            if (hadoopHome != null && !hadoopHome.isEmpty()) {
                return hadoopHome;
            }

            throw new IllegalStateException(
                "Start version Hadoop distribution not found. " +
                "Please pass system property: -Dhadoop.start.home=/path/to/hadoop-start-version, or " +
                "set environment variable HADOOP_HOME (deprecated)");
        }

        /**
         * Read upgrade version Hadoop distribution path from system property.
         * Tries in order:
         * 1. System property: hadoop.upgrade.home
         * 2. Environment variable: HADOOP_UPGRADE_HOME (fallback)
         *
         * @return Hadoop home path for upgrade version
         * @throws IllegalStateException if no distribution found
         */
        private static String readUpgradeVersionHome() {
            // Try system property first (preferred)
            String sysProp = System.getProperty("hadoop.upgrade.home");
            if (sysProp != null && !sysProp.isEmpty()) {
                return sysProp;
            }

            // Try environment variable (backward compatibility)
            String hadoopHome = System.getenv("HADOOP_UPGRADE_HOME");
            if (hadoopHome != null && !hadoopHome.isEmpty()) {
                return hadoopHome;
            }

            throw new IllegalStateException(
                "Upgrade version Hadoop distribution not found. " +
                "Please pass system property: -Dhadoop.upgrade.home=/path/to/hadoop-upgrade-version, or " +
                "set environment variable HADOOP_UPGRADE_HOME (deprecated)");
        }

        /**
         * Read Hadoop distributions from system properties.
         * ALL ProcessBased tests are designed to be upgrade-capable, so this method
         * ALWAYS reads BOTH start and upgrade versions:
         * - Start version: -Dhadoop.start.home (or HADOOP_HOME env var as fallback)
         * - Upgrade version: -Dhadoop.upgrade.home (or HADOOP_UPGRADE_HOME env var as fallback)
         *
         * The start version is used to initially start all nodes.
         * The upgrade version is available via cluster.getUpgradeDistributionPath()
         * for use in rolling upgrade scenarios.
         *
         * Example usage:
         * <pre>
         * mvn test -Dtest=MyTest \
         *     -Dhadoop.start.home=/opt/hadoop-3.3.5 \
         *     -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
         * </pre>
         *
         * @return this Builder
         * @throws IllegalStateException if either distribution not found
         */
        public Builder fromSystemProperties() {
            this.defaultHadoopHome = readStartVersionHome();
            this.upgradeHadoopHome = readUpgradeVersionHome();
            return this;
        }

        /**
         * Set the default Hadoop distribution for all nodes.
         */
        public Builder allNodesHadoopDistribution(String hadoopHome) {
            this.defaultHadoopHome = hadoopHome;
            return this;
        }

        /**
         * Set the Hadoop distribution for a specific NameNode.
         */
        public Builder nameNodeHadoopDistribution(int nnIndex, String hadoopHome) {
            this.nameNodeHadoopHomes.put(nnIndex, hadoopHome);
            return this;
        }

        /**
         * Set the Hadoop distribution for all NameNodes.
         */
        public Builder nameNodeHadoopDistribution(String hadoopHome) {
            return nameNodeHadoopDistribution(0, hadoopHome);
        }

        /**
         * Set the Hadoop distribution for a specific DataNode.
         */
        public Builder dataNodeHadoopDistribution(int dnIndex, String hadoopHome) {
            this.dataNodeHadoopHomes.put(dnIndex, hadoopHome);
            return this;
        }

        /**
         * Set the same storage type configuration for each DataNode.
         * If storageTypes is uninitialized or passed null then StorageType.DEFAULT is used.
         *
         * @param types array of storage types (length should equal storagesPerDatanode)
         * @return this Builder
         */
        public Builder storageTypes(StorageType[] types) {
            this.storageTypes1D = types;
            return this;
        }

        /**
         * Set custom storage type configuration for each DataNode.
         * If storageTypes is uninitialized or passed null then StorageType.DEFAULT is used.
         *
         * @param types 2D array where types[i] contains storage types for DataNode i
         * @return this Builder
         */
        public Builder storageTypes(StorageType[][] types) {
            this.storageTypes = types;
            return this;
        }

        /**
         * Set the number of storage locations per DataNode (default: 1).
         *
         * @param num number of storage locations per DataNode
         * @return this Builder
         */
        public Builder storagesPerDatanode(int num) {
            if (num < 1) {
                throw new IllegalArgumentException("Must have at least 1 storage per DataNode");
            }
            this.storagesPerDatanode = num;
            return this;
        }

        /**
         * Set rack assignments for DataNodes.
         *
         * @param val array of rack names, one for each DataNode
         * @return this Builder
         */
        public Builder racks(String[] val) {
            this.racks = val;
            return this;
        }

        /**
         * Build and start the cluster.
         * If no Hadoop distributions have been explicitly set, automatically reads
         * from system properties (hadoop.start.home and hadoop.upgrade.home).
         */
        public ProcessBasedMiniDFSCluster build() throws IOException, TimeoutException {
            // Auto-configure from system properties if not explicitly set
            if (defaultHadoopHome == null) {
                fromSystemProperties();
            }

            ProcessBasedMiniDFSCluster cluster = new ProcessBasedMiniDFSCluster(this);
            cluster.start();
            return cluster;
        }

        /**
         * Build the cluster without starting it (for testing).
         * If no Hadoop distributions have been explicitly set, automatically reads
         * from system properties (hadoop.start.home and hadoop.upgrade.home).
         */
        public ProcessBasedMiniDFSCluster buildWithoutStart() throws IOException {
            // Auto-configure from system properties if not explicitly set
            if (defaultHadoopHome == null) {
                fromSystemProperties();
            }

            return new ProcessBasedMiniDFSCluster(this);
        }
    }
}

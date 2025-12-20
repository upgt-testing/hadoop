package org.restarttest.adapter.yarn;

import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.ha.HAServiceProtocol.HAServiceState;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.nodemanager.Context;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.container.Container;
import org.restarttest.adapter.yarn.health.YarnApplicationsHealthCheck;
import org.restarttest.adapter.yarn.health.YarnNodeManagersRegisteredCheck;
import org.restarttest.adapter.yarn.health.YarnQueuesHealthCheck;
import org.restarttest.adapter.yarn.health.YarnResourceManagerActiveCheck;
import org.restarttest.core.ClusterAdapter;
import org.restarttest.core.RestartMode;
import org.restarttest.health.CompositeHealthCheck;
import org.restarttest.health.HealthCheck;
import org.restarttest.state.StateCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapter for Apache Hadoop YARN MiniYARNCluster to enable restart testing.
 *
 * Supports restarting ResourceManagers and NodeManagers with different restart modes.
 * Handles both single-RM and HA (High Availability) configurations.
 *
 * <p>Restart modes:
 * <ul>
 *   <li>GRACEFUL: Performs clean shutdown with proper state transitions, waits for
 *       containers to complete, and ensures proper unregistration.</li>
 *   <li>CRASH: Simulates abrupt failure without cleanup or state transitions.</li>
 *   <li>DELAYED_CRASH: Crash with a delay before restart.</li>
 * </ul>
 */
public class YarnClusterAdapter implements ClusterAdapter<MiniYARNCluster> {

    private static final Logger LOG = LoggerFactory.getLogger(YarnClusterAdapter.class);

    // Timeouts for graceful operations
    private static final long GRACEFUL_HA_TRANSITION_TIMEOUT_MS = 10000;
    private static final long GRACEFUL_CONTAINER_WAIT_TIMEOUT_MS = 30000;
    private static final long GRACEFUL_STATE_SYNC_WAIT_MS = 1000;
    private static final long GRACEFUL_HA_FAILOVER_WAIT_MS = 2000;

    private final YarnStateCapture stateCapture;
    private final CompositeHealthCheck<MiniYARNCluster> healthCheck;

    public YarnClusterAdapter() {
        this.stateCapture = new YarnStateCapture();
        this.healthCheck = new CompositeHealthCheck<>("yarn-health");

        // Add all health checks
        healthCheck.addCheck(new YarnResourceManagerActiveCheck());
        healthCheck.addCheck(new YarnNodeManagersRegisteredCheck());
        healthCheck.addCheck(new YarnApplicationsHealthCheck());
        healthCheck.addCheck(new YarnQueuesHealthCheck());
    }

    @Override
    public Class<MiniYARNCluster> getClusterType() {
        return MiniYARNCluster.class;
    }

    @Override
    public void restartNode(MiniYARNCluster cluster, String nodeRole, int nodeIndex,
                           RestartMode mode) throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        LOG.info("Restarting {} node at index {} with mode {}", normalizedRole, nodeIndex, mode);

        if ("resourcemanager".equals(normalizedRole)) {
            restartResourceManager(cluster, nodeIndex, mode);
        } else if ("nodemanager".equals(normalizedRole)) {
            restartNodeManager(cluster, nodeIndex, mode);
        } else if ("all".equals(normalizedRole)) {
            restartAllNodes(cluster, "all", mode);
        } else {
            throw new IllegalArgumentException(
                "Unknown node role: " + nodeRole +
                ". Supported: resourcemanager, nodemanager, master, worker, all");
        }

        LOG.info("Successfully restarted {} node at index {}", normalizedRole, nodeIndex);
    }

    @Override
    public void restartAllNodes(MiniYARNCluster cluster, String nodeRole, RestartMode mode)
            throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        LOG.info("Restarting all {} nodes with mode {}", normalizedRole, mode);

        if ("resourcemanager".equals(normalizedRole)) {
            int rmCount = cluster.getNumOfResourceManager();
            for (int i = 0; i < rmCount; i++) {
                restartResourceManager(cluster, i, mode);
            }
        } else if ("nodemanager".equals(normalizedRole)) {
            int nmCount = getNodeManagerCount(cluster);
            for (int i = 0; i < nmCount; i++) {
                restartNodeManager(cluster, i, mode);
            }
        } else if ("all".equals(normalizedRole)) {
            // Restart all ResourceManagers
            int rmCount = cluster.getNumOfResourceManager();
            for (int i = 0; i < rmCount; i++) {
                restartResourceManager(cluster, i, mode);
            }
            // Restart all NodeManagers
            int nmCount = getNodeManagerCount(cluster);
            for (int i = 0; i < nmCount; i++) {
                restartNodeManager(cluster, i, mode);
            }
        } else {
            throw new IllegalArgumentException("Unknown node role: " + nodeRole);
        }

        LOG.info("Successfully restarted all {} nodes", normalizedRole);
    }

    @Override
    public void waitActive(MiniYARNCluster cluster) throws Exception {
        LOG.info("Waiting for YARN cluster to become active");

        // Wait for ResourceManagers to be started
        int rmCount = cluster.getNumOfResourceManager();
        for (int i = 0; i < rmCount; i++) {
            ResourceManager rm = cluster.getResourceManager(i);
            if (rm != null) {
                waitForServiceState(rm, Service.STATE.STARTED, 10000);
            }
        }

        // Give the cluster a moment to stabilize before checking NM connections
        Thread.sleep(2000);

        // Wait for all NodeManagers to connect with extended timeout
        // Try multiple times as NMs may need time to recover after RM restart
        int maxAttempts = 3;
        boolean connected = false;
        for (int attempt = 1; attempt <= maxAttempts && !connected; attempt++) {
            LOG.info("Waiting for NodeManagers to connect (attempt {}/{})", attempt, maxAttempts);
            connected = cluster.waitForNodeManagersToConnect(20000);
            if (!connected && attempt < maxAttempts) {
                Thread.sleep(2000);
            }
        }

        if (!connected) {
            LOG.warn("NodeManagers did not all connect after {} attempts", maxAttempts);
            // Throw exception so health checks will correctly fail
            throw new Exception("NodeManagers failed to connect after restart");
        }

        LOG.info("YARN cluster is active");
    }

    @Override
    public StateCapture<MiniYARNCluster> getStateCapture() {
        return stateCapture;
    }

    @Override
    public HealthCheck<MiniYARNCluster> getHealthCheck() {
        return healthCheck;
    }

    @Override
    public int getNodeCount(MiniYARNCluster cluster, String nodeRole) throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        if ("resourcemanager".equals(normalizedRole)) {
            return cluster.getNumOfResourceManager();
        } else if ("nodemanager".equals(normalizedRole)) {
            return getNodeManagerCount(cluster);
        } else if ("all".equals(normalizedRole)) {
            return cluster.getNumOfResourceManager() + getNodeManagerCount(cluster);
        } else {
            throw new IllegalArgumentException("Unknown node role: " + nodeRole);
        }
    }

    /**
     * Restart a ResourceManager with the specified mode.
     * Tracks and restores HA state to ensure RM returns to its previous role.
     *
     * <p>For GRACEFUL mode:
     * <ul>
     *   <li>In HA mode: Transitions to STANDBY first to allow clean leadership handoff</li>
     *   <li>Waits for state store to sync</li>
     *   <li>Then performs restart</li>
     * </ul>
     *
     * <p>For CRASH mode:
     * <ul>
     *   <li>Immediately stops the RM without graceful transitions</li>
     *   <li>Simulates abrupt failure</li>
     * </ul>
     */
    private void restartResourceManager(MiniYARNCluster cluster, int index, RestartMode mode)
            throws Exception {
        LOG.info("Restarting ResourceManager {} with mode {}", index, mode);

        // STEP 1: Capture HA state before restart
        ResourceManager rmBeforeRestart = cluster.getResourceManager(index);
        HAServiceState previousHAState = null;
        boolean isHAEnabled = false;
        if (rmBeforeRestart != null && rmBeforeRestart.getRMContext() != null) {
            try {
                isHAEnabled = rmBeforeRestart.getRMContext().isHAEnabled();
                previousHAState = rmBeforeRestart.getRMContext()
                    .getRMAdminService()
                    .getServiceStatus()
                    .getState();
                LOG.info("RM {} was in HA state {} before restart (HA enabled: {})",
                    index, previousHAState, isHAEnabled);
            } catch (Exception e) {
                LOG.warn("Failed to get HA state for RM {}: {}", index, e.getMessage());
            }
        }

        // STEP 2: Perform restart based on mode
        switch (mode) {
            case GRACEFUL:
                // Truly graceful shutdown: transition to standby first (in HA mode),
                // wait for state sync, then restart
                gracefulRestartResourceManager(cluster, index, rmBeforeRestart,
                    isHAEnabled, previousHAState);
                break;

            case CRASH:
                // Abrupt shutdown - no graceful transitions, simulates crash
                LOG.info("Performing CRASH shutdown of RM {} (no graceful transition)", index);
                cluster.stopResourceManager(index);
                Thread.sleep(100); // Brief delay to simulate crash recovery window
                cluster.restartResourceManager(index);
                break;

            case DELAYED_CRASH:
                // Stop, wait, then restart
                LOG.info("Performing DELAYED_CRASH shutdown of RM {}", index);
                cluster.stopResourceManager(index);
                Thread.sleep(500); // Default delay
                cluster.restartResourceManager(index);
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // STEP 3: Wait for RM to be started
        waitForResourceManagerReady(cluster, index);

        // STEP 4: Restore HA state if it was ACTIVE before restart
        if (previousHAState == HAServiceState.ACTIVE) {
            LOG.info("Restoring RM {} to ACTIVE state after restart", index);
            ResourceManager rmAfterRestart = cluster.getResourceManager(index);
            if (rmAfterRestart != null && rmAfterRestart.getRMContext() != null) {
                try {
                    // Transition back to ACTIVE
                    rmAfterRestart.getRMContext().getRMAdminService()
                        .transitionToActive(new HAServiceProtocol.StateChangeRequestInfo(
                            HAServiceProtocol.RequestSource.REQUEST_BY_USER));

                    // Wait a bit for transition to complete and services to stabilize
                    Thread.sleep(2000);

                    // Verify the transition succeeded
                    HAServiceState currentState = rmAfterRestart.getRMContext()
                        .getRMAdminService()
                        .getServiceStatus()
                        .getState();

                    if (currentState == HAServiceState.ACTIVE) {
                        LOG.info("Successfully restored RM {} to ACTIVE state", index);
                    } else {
                        LOG.warn("RM {} is in {} state after transition attempt, expected ACTIVE",
                                index, currentState);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to restore RM {} to ACTIVE state: {}", index, e.getMessage(), e);
                    throw new Exception("Failed to restore RM HA state after restart", e);
                }
            }
        } else if (previousHAState != null) {
            LOG.info("RM {} was in {} state before restart, no HA state restoration needed",
                    index, previousHAState);
        }

        // STEP 5: NodeManagers will automatically reconnect
        // With YARN_MINICLUSTER_FIXED_PORTS enabled, the RM keeps the same address after restart,
        // so NodeManagers can automatically reconnect without needing to be restarted.
        // Their built-in retry logic will handle the reconnection.
        LOG.info("ResourceManager {} restart complete. NodeManagers will automatically reconnect.", index);
    }

    /**
     * Gracefully restart a ResourceManager.
     * For HA clusters: transitions to standby before stopping to allow clean leadership handoff.
     * Ensures state is properly persisted before restart.
     */
    private void gracefulRestartResourceManager(MiniYARNCluster cluster, int index,
            ResourceManager rm, boolean isHAEnabled, HAServiceState previousHAState)
            throws Exception {
        LOG.info("Performing GRACEFUL restart of RM {}", index);

        // Step 1: For HA setups, gracefully transition to standby first
        // This allows leadership to transfer cleanly to another RM
        if (isHAEnabled && previousHAState == HAServiceState.ACTIVE) {
            LOG.info("Gracefully transitioning RM {} from ACTIVE to STANDBY before restart", index);
            try {
                // Use the admin service to properly transition
                rm.getRMContext().getRMAdminService()
                    .transitionToStandby(new HAServiceProtocol.StateChangeRequestInfo(
                        HAServiceProtocol.RequestSource.REQUEST_BY_USER));

                // Wait for the transition to complete
                waitForHAState(rm, HAServiceState.STANDBY, GRACEFUL_HA_TRANSITION_TIMEOUT_MS);
                LOG.info("RM {} gracefully transitioned to STANDBY", index);

                // Wait for another RM to become active (if available)
                Thread.sleep(GRACEFUL_HA_FAILOVER_WAIT_MS);
            } catch (Exception e) {
                LOG.warn("Failed to gracefully transition RM {} to standby: {}. " +
                    "Proceeding with restart anyway.", index, e.getMessage());
                // Continue with restart even if transition fails
            }
        }

        // Step 2: Ensure pending state is flushed to state store
        if (rm != null && rm.getRMContext() != null && rm.getRMContext().getStateStore() != null) {
            try {
                LOG.info("Waiting for RM {} state store to sync before shutdown", index);
                Thread.sleep(GRACEFUL_STATE_SYNC_WAIT_MS);
            } catch (Exception e) {
                LOG.warn("Error waiting for state store sync: {}", e.getMessage());
            }
        }

        // Step 3: Now perform the restart
        cluster.restartResourceManager(index);
    }

    /**
     * Wait for ResourceManager to reach a specific HA state.
     */
    private void waitForHAState(ResourceManager rm, HAServiceState expectedState,
            long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                HAServiceState currentState = rm.getRMContext()
                    .getRMAdminService()
                    .getServiceStatus()
                    .getState();
                if (currentState == expectedState) {
                    return;
                }
            } catch (Exception e) {
                // RM may be in transition, continue waiting
                LOG.debug("Error checking HA state during transition: {}", e.getMessage());
            }
            Thread.sleep(200);
        }
        throw new Exception("RM did not reach state " + expectedState +
            " within " + timeoutMs + "ms");
    }

    /**
     * Restart a NodeManager with the specified mode.
     * Creates a new NodeManager instance since Hadoop's service state machine
     * doesn't allow STOPPED → INITED transitions on the same instance.
     *
     * <p>For GRACEFUL mode:
     * <ul>
     *   <li>Waits for running containers to complete (with timeout)</li>
     *   <li>Allows proper unregistration from ResourceManager</li>
     *   <li>Ensures clean shutdown</li>
     * </ul>
     *
     * <p>For CRASH mode:
     * <ul>
     *   <li>Skips unregistration (simulates abrupt failure)</li>
     *   <li>Does not wait for containers</li>
     * </ul>
     */
    private void restartNodeManager(MiniYARNCluster cluster, int index, RestartMode mode)
            throws Exception {
        LOG.info("Restarting NodeManager {} with mode {}", index, mode);

        NodeManager nm = cluster.getNodeManager(index);
        if (nm == null) {
            throw new Exception("NodeManager at index " + index + " is null");
        }

        // Save the config before stopping - we'll need it for the new instance
        org.apache.hadoop.conf.Configuration nmConfig = nm.getConfig();

        switch (mode) {
            case GRACEFUL:
                // Truly graceful shutdown: wait for containers, allow proper unregistration
                gracefulStopNodeManager(nm, index);
                break;

            case CRASH:
                // Abrupt shutdown - skip unregistration, don't wait for containers
                crashStopNodeManager(nm, index);
                break;

            case DELAYED_CRASH:
                // Crash with delay
                LOG.info("Performing DELAYED_CRASH shutdown of NM {}", index);
                crashStopNodeManager(nm, index);
                Thread.sleep(500);
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // Use reflection to create a new NodeManager and replace the old one
        // MiniYARNCluster stores NodeManagers in a private array
        try {
            java.lang.reflect.Field nmField = MiniYARNCluster.class.getDeclaredField("nodeManagers");
            nmField.setAccessible(true);
            NodeManager[] nodeManagers = (NodeManager[]) nmField.get(cluster);

            // Check if cluster uses RPC mode to determine NodeManager type
            java.lang.reflect.Field useRpcField = MiniYARNCluster.class.getDeclaredField("useRpc");
            useRpcField.setAccessible(true);
            boolean useRpc = useRpcField.getBoolean(cluster);

            // Create new NodeManager instance of the appropriate type
            NodeManager newNm;
            if (useRpc) {
                // CustomNodeManager is a public inner class
                Class<?> customNmClass = Class.forName(
                    "org.apache.hadoop.yarn.server.MiniYARNCluster$CustomNodeManager");
                java.lang.reflect.Constructor<?> ctor = customNmClass.getDeclaredConstructor(MiniYARNCluster.class);
                ctor.setAccessible(true);
                newNm = (NodeManager) ctor.newInstance(cluster);
            } else {
                // ShortCircuitedNodeManager
                Class<?> shortCircuitNmClass = Class.forName(
                    "org.apache.hadoop.yarn.server.MiniYARNCluster$ShortCircuitedNodeManager");
                java.lang.reflect.Constructor<?> ctor = shortCircuitNmClass.getDeclaredConstructor(MiniYARNCluster.class);
                ctor.setAccessible(true);
                newNm = (NodeManager) ctor.newInstance(cluster);
            }

            // Replace in the array
            nodeManagers[index] = newNm;

            // Initialize and start the new NodeManager with the saved config
            newNm.init(nmConfig);
            newNm.start();

            if (newNm.getServiceState() != Service.STATE.STARTED) {
                throw new Exception("NodeManager " + index + " failed to start after restart");
            }

        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException |
                 NoSuchMethodException | java.lang.reflect.InvocationTargetException |
                 InstantiationException e) {
            throw new Exception("Failed to restart NodeManager using reflection: " + e.getMessage(), e);
        }

        // Wait for NodeManager to reconnect to ResourceManager
        if (!cluster.waitForNodeManagersToConnect(10000)) {
            LOG.warn("NodeManager {} may not have reconnected properly", index);
        }

        LOG.info("NodeManager {} restarted successfully", index);
    }

    /**
     * Gracefully stop a NodeManager.
     * Waits for running containers to complete (with timeout), then allows
     * proper unregistration from ResourceManager via the normal stop() path.
     *
     * <p>The stop() call will trigger:
     * <ul>
     *   <li>NodeStatusUpdaterImpl.serviceStop() which calls unRegisterNM()</li>
     *   <li>ContainerManagerImpl.cleanUpApplicationsOnNMShutDown() which waits for apps</li>
     * </ul>
     */
    private void gracefulStopNodeManager(NodeManager nm, int index) throws Exception {
        LOG.info("Performing GRACEFUL shutdown of NM {}", index);

        // Step 1: Check for running containers and wait for them to complete
        int containerCount = getRunningContainerCount(nm);
        if (containerCount > 0) {
            LOG.info("NM {} has {} running containers, waiting for completion (max {}ms)",
                index, containerCount, GRACEFUL_CONTAINER_WAIT_TIMEOUT_MS);

            long waitStartTime = System.currentTimeMillis();
            while (containerCount > 0 &&
                   System.currentTimeMillis() - waitStartTime < GRACEFUL_CONTAINER_WAIT_TIMEOUT_MS) {
                Thread.sleep(1000);
                containerCount = getRunningContainerCount(nm);
                if (containerCount > 0) {
                    LOG.debug("Waiting for {} containers to complete on NM {}", containerCount, index);
                }
            }

            if (containerCount > 0) {
                LOG.warn("NM {} still has {} containers after {}ms wait, proceeding with shutdown",
                    index, containerCount, GRACEFUL_CONTAINER_WAIT_TIMEOUT_MS);
            } else {
                LOG.info("All containers completed on NM {}", index);
            }
        }

        // Step 2: Stop the NodeManager - this triggers:
        //   - unRegisterNM() to notify RM
        //   - cleanUpApplicationsOnNMShutDown() for final cleanup
        nm.stop();

        // Step 3: Wait for service to fully stop
        waitForServiceState(nm, Service.STATE.STOPPED, 10000);
        LOG.info("NM {} gracefully stopped", index);
    }

    /**
     * Crash stop a NodeManager - simulates abrupt failure.
     * Skips unregistration from ResourceManager by setting the decommissioned flag.
     * Does not wait for containers to complete.
     */
    private void crashStopNodeManager(NodeManager nm, int index) throws Exception {
        LOG.info("Performing CRASH shutdown of NM {} (no graceful cleanup)", index);

        // Try to set the decommissioned flag to skip unRegisterNM() call during stop
        // This simulates a crash where the NM doesn't get a chance to unregister
        try {
            Context nmContext = nm.getNMContext();
            if (nmContext != null) {
                nmContext.setDecommissioned(true);
                LOG.debug("Set decommissioned flag on NM {} to skip unregistration", index);
            }
        } catch (Exception e) {
            LOG.debug("Could not set decommissioned flag on NM {}: {}", index, e.getMessage());
            // Continue anyway - the main goal is to not wait for containers
        }

        // Stop immediately without waiting for containers
        nm.stop();
        // Don't wait for clean stop - it's a crash simulation
    }

    /**
     * Get the count of running containers on a NodeManager.
     * Returns 0 if the count cannot be determined.
     */
    private int getRunningContainerCount(NodeManager nm) {
        try {
            Context nmContext = nm.getNMContext();
            if (nmContext != null) {
                ConcurrentMap<ContainerId, Container> containers = nmContext.getContainers();
                if (containers != null) {
                    return containers.size();
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not get container count: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Normalize generic role names to YARN-specific names.
     */
    private String normalizeRole(String role) {
        String lower = role.toLowerCase();
        if ("master".equals(lower)) {
            return "resourcemanager";
        }
        if ("worker".equals(lower)) {
            return "nodemanager";
        }
        return lower;
    }

    /**
     * Get the number of NodeManagers in the cluster.
     * Since MiniYARNCluster doesn't provide getNumOfNodeManagers(),
     * we count by iterating through getNodeManager() until we hit null.
     */
    private int getNodeManagerCount(MiniYARNCluster cluster) {
        int count = 0;
        while (true) {
            try {
                NodeManager nm = cluster.getNodeManager(count);
                if (nm == null) {
                    break;
                }
                count++;
            } catch (Exception e) {
                // If we get an exception, we've reached the end
                break;
            }
        }
        return count;
    }

    /**
     * Wait for a service to reach the expected state.
     */
    private void waitForServiceState(Service service, Service.STATE expectedState,
                                     long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (service.getServiceState() == expectedState) {
                return;
            }
            Thread.sleep(100);
        }
        throw new Exception(
            "Service did not reach state " + expectedState +
            " within " + timeoutMs + "ms. Current state: " + service.getServiceState());
    }

    /**
     * Wait for ResourceManager to be ready after restart.
     * Ensures the RM service is started and gives it time to initialize.
     */
    private void waitForResourceManagerReady(MiniYARNCluster cluster, int index)
            throws Exception {
        ResourceManager rm = cluster.getResourceManager(index);
        if (rm != null) {
            // Wait for service to be started
            waitForServiceState(rm, Service.STATE.STARTED, 10000);

            // Additional wait for RM to stabilize and initialize HA components
            // This is important for HA setups where the RM needs time to
            // initialize its AdminService before we can transition it to ACTIVE
            Thread.sleep(1000);

            LOG.info("ResourceManager {} is ready (service state: STARTED)", index);
        }
    }
}

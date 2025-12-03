package org.restarttest.adapter.yarn;

import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
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

/**
 * Adapter for Apache Hadoop YARN MiniYARNCluster to enable restart testing.
 *
 * Supports restarting ResourceManagers and NodeManagers with different restart modes.
 * Handles both single-RM and HA (High Availability) configurations.
 */
public class YarnClusterAdapter implements ClusterAdapter<MiniYARNCluster> {

    private static final Logger LOG = LoggerFactory.getLogger(YarnClusterAdapter.class);

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
     */
    private void restartResourceManager(MiniYARNCluster cluster, int index, RestartMode mode)
            throws Exception {
        LOG.info("Restarting ResourceManager {} with mode {}", index, mode);

        switch (mode) {
            case GRACEFUL:
                // Use built-in restart which does graceful shutdown
                cluster.restartResourceManager(index);
                break;

            case CRASH:
                // Stop abruptly then restart
                cluster.stopResourceManager(index);
                cluster.restartResourceManager(index);
                break;

            case DELAYED_CRASH:
                // Stop, wait, then restart
                cluster.stopResourceManager(index);
                Thread.sleep(500); // Default delay
                cluster.restartResourceManager(index);
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // Wait for RM to be ready
        waitForResourceManagerReady(cluster, index);
    }

    /**
     * Restart a NodeManager with the specified mode.
     * Note: MiniYARNCluster doesn't have built-in NodeManager restart methods,
     * so we implement custom restart logic.
     */
    private void restartNodeManager(MiniYARNCluster cluster, int index, RestartMode mode)
            throws Exception {
        LOG.info("Restarting NodeManager {} with mode {}", index, mode);

        NodeManager nm = cluster.getNodeManager(index);
        if (nm == null) {
            throw new Exception("NodeManager at index " + index + " is null");
        }

        switch (mode) {
            case GRACEFUL:
                // Graceful shutdown
                nm.stop();
                waitForServiceState(nm, Service.STATE.STOPPED, 5000);
                break;

            case CRASH:
            case DELAYED_CRASH:
                // Abrupt shutdown (stop without waiting)
                nm.stop();
                if (mode == RestartMode.DELAYED_CRASH) {
                    Thread.sleep(500);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // Restart the NodeManager
        nm.init(cluster.getConfig());
        nm.start();

        // Wait for NodeManager to reconnect to ResourceManager
        if (!cluster.waitForNodeManagersToConnect(10000)) {
            LOG.warn("NodeManager {} may not have reconnected properly", index);
        }

        LOG.info("NodeManager {} restarted successfully", index);
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
     */
    private void waitForResourceManagerReady(MiniYARNCluster cluster, int index)
            throws Exception {
        ResourceManager rm = cluster.getResourceManager(index);
        if (rm != null) {
            waitForServiceState(rm, Service.STATE.STARTED, 10000);
        }
    }
}

package org.restarttest.adapter.yarn.health;

import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.restarttest.health.HealthCheck;
import org.restarttest.health.HealthCheckResult;

/**
 * Health check to verify all NodeManagers are registered and healthy.
 *
 * Checks:
 * - Expected number of NodeManagers are registered with ResourceManager
 * - All registered NodeManagers are active
 */
public class YarnNodeManagersRegisteredCheck implements HealthCheck<MiniYARNCluster> {

    @Override
    public HealthCheckResult checkHealth(MiniYARNCluster cluster) throws Exception {
        HealthCheckResult result = new HealthCheckResult(true, getName());

        // Get expected NM count by counting non-null NodeManagers
        int expectedNMCount = getExpectedNodeManagerCount(cluster);
        result.addMetric("expected_nm_count", expectedNMCount);

        // Get registered NM count from RM
        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM == null) {
            result.addFailure("Cannot check NM registration: no active RM");
            return result;
        }

        ClusterMetricsInfo metrics = new ClusterMetricsInfo(activeRM);
        int registeredNMCount = metrics.getTotalNodes();
        int activeNMCount = metrics.getActiveNodes();

        result.addMetric("registered_nm_count", registeredNMCount);
        result.addMetric("active_nm_count", activeNMCount);

        if (registeredNMCount < expectedNMCount) {
            result.addFailure(
                "Not all NodeManagers registered: expected=" + expectedNMCount +
                ", registered=" + registeredNMCount);
        }

        if (activeNMCount < expectedNMCount) {
            result.addFailure(
                "Not all NodeManagers active: expected=" + expectedNMCount +
                ", active=" + activeNMCount);
        }

        return result;
    }

    @Override
    public String getName() {
        return "yarn-nodemanagers-registered";
    }

    /**
     * Count the number of NodeManagers by iterating until we hit null.
     */
    private int getExpectedNodeManagerCount(MiniYARNCluster cluster) {
        int count = 0;
        while (true) {
            try {
                NodeManager nm = cluster.getNodeManager(count);
                if (nm == null) {
                    break;
                }
                count++;
            } catch (Exception e) {
                break;
            }
        }
        return count;
    }
}

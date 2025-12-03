package org.restarttest.adapter.yarn.health;

import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.restarttest.health.HealthCheck;
import org.restarttest.health.HealthCheckResult;

/**
 * Health check to verify ResourceManager is active and operational.
 *
 * Checks:
 * - At least one RM is active
 * - Active RM is in STARTED service state
 * - Active RM is in ACTIVE HA state (if HA enabled)
 */
public class YarnResourceManagerActiveCheck implements HealthCheck<MiniYARNCluster> {

    @Override
    public HealthCheckResult checkHealth(MiniYARNCluster cluster) throws Exception {
        HealthCheckResult result = new HealthCheckResult(true, getName());

        int activeRMIndex = cluster.getActiveRMIndex();
        result.addMetric("active_rm_index", activeRMIndex);

        if (activeRMIndex == -1) {
            result.addFailure("No active ResourceManager found");
            return result;
        }

        ResourceManager activeRM = cluster.getResourceManager(activeRMIndex);
        if (activeRM == null) {
            result.addFailure("Active ResourceManager is null");
            return result;
        }

        // Check service state
        Service.STATE serviceState = activeRM.getServiceState();
        result.addMetric("rm_service_state", serviceState.name());

        if (serviceState != Service.STATE.STARTED) {
            result.addFailure("ResourceManager not in STARTED state: " + serviceState);
        }

        // Check HA state (if HA is enabled)
        try {
            HAServiceProtocol.HAServiceState haState = activeRM.getRMContext()
                .getRMAdminService()
                .getServiceStatus()
                .getState();
            result.addMetric("rm_ha_state", haState.name());

            if (haState != HAServiceProtocol.HAServiceState.ACTIVE) {
                result.addFailure("ResourceManager not in ACTIVE HA state: " + haState);
            }
        } catch (Exception e) {
            // HA may not be enabled, this is not necessarily a failure
            result.addMetric("rm_ha_state", "N/A");
        }

        return result;
    }

    @Override
    public String getName() {
        return "yarn-resourcemanager-active";
    }
}

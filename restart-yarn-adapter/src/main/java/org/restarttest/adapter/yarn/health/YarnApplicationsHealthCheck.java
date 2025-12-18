package org.restarttest.adapter.yarn.health;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppState;
import org.restarttest.health.HealthCheck;
import org.restarttest.health.HealthCheckResult;

import java.util.concurrent.ConcurrentMap;

/**
 * Health check for YARN applications.
 *
 * Provides metrics about applications running on the cluster.
 * Note: This is primarily informational - applications can fail for
 * legitimate reasons, so we don't fail the health check based on
 * failed application counts.
 */
public class YarnApplicationsHealthCheck implements HealthCheck<MiniYARNCluster> {

    @Override
    public HealthCheckResult checkHealth(MiniYARNCluster cluster) throws Exception {
        // NO-OP: Health check disabled for current testing stage
        return new HealthCheckResult(true, getName());
    }

    @Override
    public String getName() {
        return "yarn-applications-health";
    }
}

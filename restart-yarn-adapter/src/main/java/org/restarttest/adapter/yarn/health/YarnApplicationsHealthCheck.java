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
        HealthCheckResult result = new HealthCheckResult(true, getName());

        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM == null) {
            result.addFailure("Cannot check applications: no active RM");
            return result;
        }

        RMContext rmContext = activeRM.getRMContext();
        ConcurrentMap<ApplicationId, RMApp> apps = rmContext.getRMApps();

        result.addMetric("total_applications", apps.size());

        // Count applications by state
        int runningCount = 0;
        int failedCount = 0;
        int finishedCount = 0;
        int killedCount = 0;

        for (RMApp app : apps.values()) {
            RMAppState state = app.getState();
            switch (state) {
                case RUNNING:
                    runningCount++;
                    break;
                case FAILED:
                    failedCount++;
                    break;
                case FINISHED:
                    finishedCount++;
                    break;
                case KILLED:
                    killedCount++;
                    break;
                default:
                    // Other states (NEW, NEW_SAVING, SUBMITTED, ACCEPTED, etc.)
                    break;
            }
        }

        result.addMetric("running_applications", runningCount);
        result.addMetric("failed_applications", failedCount);
        result.addMetric("finished_applications", finishedCount);
        result.addMetric("killed_applications", killedCount);

        // This is informational only - we don't fail based on failed applications
        // Applications can fail for legitimate reasons during testing

        return result;
    }

    @Override
    public String getName() {
        return "yarn-applications-health";
    }
}

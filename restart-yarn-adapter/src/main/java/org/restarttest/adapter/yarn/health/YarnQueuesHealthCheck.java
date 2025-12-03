package org.restarttest.adapter.yarn.health;

import org.apache.hadoop.yarn.api.records.QueueState;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CSQueue;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.ParentQueue;
import org.restarttest.health.HealthCheck;
import org.restarttest.health.HealthCheckResult;

/**
 * Health check for YARN queues (Capacity Scheduler).
 *
 * Checks:
 * - Root queue exists
 * - No queues are in STOPPED state
 * - Queue hierarchy is accessible
 *
 * Note: This check only applies when using Capacity Scheduler.
 * For other schedulers, the check passes with informational metrics.
 */
public class YarnQueuesHealthCheck implements HealthCheck<MiniYARNCluster> {

    @Override
    public HealthCheckResult checkHealth(MiniYARNCluster cluster) throws Exception {
        HealthCheckResult result = new HealthCheckResult(true, getName());

        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM == null) {
            result.addFailure("Cannot check queues: no active RM");
            return result;
        }

        ResourceScheduler scheduler = activeRM.getRMContext().getScheduler();
        result.addMetric("scheduler_type", scheduler.getClass().getSimpleName());

        if (!(scheduler instanceof CapacityScheduler)) {
            // Not using capacity scheduler, skip queue checks
            // This is not a failure - just informational
            return result;
        }

        CapacityScheduler cs = (CapacityScheduler) scheduler;
        CSQueue rootQueue = cs.getQueue("root");

        if (rootQueue == null) {
            result.addFailure("Root queue not found");
            return result;
        }

        // Check queue health recursively
        checkQueueHealth(rootQueue, result);

        return result;
    }

    /**
     * Recursively check health of a queue and its children.
     */
    private void checkQueueHealth(CSQueue queue, HealthCheckResult result) {
        QueueState state = queue.getState();
        String queuePath = queue.getQueuePath();

        result.addMetric("queue." + queuePath + ".state", state.name());

        if (state == QueueState.STOPPED) {
            result.addFailure("Queue stopped: " + queuePath);
        }

        // Recursively check child queues
        if (queue instanceof ParentQueue) {
            ParentQueue parentQueue = (ParentQueue) queue;
            for (CSQueue childQueue : parentQueue.getChildQueues()) {
                checkQueueHealth(childQueue, result);
            }
        }
    }

    @Override
    public String getName() {
        return "yarn-queues-health";
    }
}

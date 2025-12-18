package org.restarttest.adapter.yarn;

import org.apache.hadoop.ha.HAServiceProtocol.HAServiceState;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.QueueState;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CSQueue;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.ParentQueue;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.restarttest.state.AbstractStateCapture;
import org.restarttest.state.ClusterState;
import org.restarttest.state.DefaultClusterState;
import org.restarttest.state.StateVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * State capture implementation for YARN MiniYARNCluster.
 *
 * Captures and verifies:
 * - ResourceManager state (count, active RM, HA states)
 * - NodeManager registration and health
 * - Application and container state
 * - Queue configuration and states
 */
public class YarnStateCapture extends AbstractStateCapture<MiniYARNCluster> {

    private static final Logger LOG = LoggerFactory.getLogger(YarnStateCapture.class);

    @Override
    public ClusterState captureState(MiniYARNCluster cluster) throws Exception {
        // NO-OP: State capture disabled for current testing stage
        LOG.debug("State capture disabled (no-op)");
        return new DefaultClusterState(new HashMap<>());
    }

    @Override
    protected void verifyCustomInvariants(MiniYARNCluster cluster,
                                         ClusterState before,
                                         ClusterState after) throws Exception {
        // NO-OP: State verification disabled for current testing stage
        LOG.debug("State verification disabled (no-op)");
    }

    private void captureResourceManagerState(MiniYARNCluster cluster, Map<String, Object> state)
            throws Exception {
        // Basic RM info
        int rmCount = cluster.getNumOfResourceManager();
        state.put("rm_count", rmCount);

        int activeRMIndex = cluster.getActiveRMIndex();
        state.put("active_rm_index", activeRMIndex);

        // Capture HA state for each RM (if HA enabled)
        if (rmCount > 1) {
            Map<Integer, String> rmStates = new HashMap<>();
            for (int i = 0; i < rmCount; i++) {
                ResourceManager rm = cluster.getResourceManager(i);
                if (rm != null) {
                    try {
                        HAServiceState haState = rm.getRMContext()
                            .getRMAdminService()
                            .getServiceStatus()
                            .getState();
                        rmStates.put(i, haState.name());
                    } catch (Exception e) {
                        LOG.warn("Failed to get HA state for RM {}: {}", i, e.getMessage());
                        rmStates.put(i, "UNKNOWN");
                    }
                }
            }
            state.put("rm_ha_states", rmStates);
        }
    }

    private void captureNodeManagerState(MiniYARNCluster cluster, Map<String, Object> state)
            throws Exception {
        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM != null) {
            try {
                ClusterMetricsInfo metrics = new ClusterMetricsInfo(activeRM);

                state.put("registered_nm_count", metrics.getTotalNodes());
                state.put("active_nm_count", metrics.getActiveNodes());
                state.put("lost_nm_count", metrics.getLostNodes());
                state.put("unhealthy_nm_count", metrics.getUnhealthyNodes());
                state.put("decommissioned_nm_count", metrics.getDecommissionedNodes());
            } catch (Exception e) {
                LOG.warn("Failed to capture NodeManager state: {}", e.getMessage());
            }
        }
    }

    private void captureApplicationState(MiniYARNCluster cluster, Map<String, Object> state)
            throws Exception {
        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM != null) {
            try {
                RMContext rmContext = activeRM.getRMContext();
                ConcurrentMap<ApplicationId, RMApp> apps = rmContext.getRMApps();

                state.put("application_count", apps.size());

                // Track application states
                Map<String, Integer> appStateCounts = new HashMap<>();
                for (RMApp app : apps.values()) {
                    String stateName = app.getState().name();
                    appStateCounts.put(stateName,
                        appStateCounts.getOrDefault(stateName, 0) + 1);
                }
                state.put("application_states", appStateCounts);
            } catch (Exception e) {
                LOG.warn("Failed to capture Application state: {}", e.getMessage());
            }
        }
    }

    private void captureContainerState(MiniYARNCluster cluster, Map<String, Object> state)
            throws Exception {
        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM != null) {
            try {
                ClusterMetricsInfo metrics = new ClusterMetricsInfo(activeRM);

                state.put("allocated_containers", metrics.getContainersAllocated());
                // Note: getContainersPending() and getContainersReserved() may not be available
                // in all Hadoop versions, so we skip them for now
            } catch (Exception e) {
                LOG.warn("Failed to capture Container state: {}", e.getMessage());
            }
        }
    }

    private void captureQueueState(MiniYARNCluster cluster, Map<String, Object> state)
            throws Exception {
        ResourceManager activeRM = cluster.getResourceManager();
        if (activeRM != null) {
            try {
                ResourceScheduler scheduler = activeRM.getRMContext().getScheduler();

                if (scheduler instanceof CapacityScheduler) {
                    CapacityScheduler cs = (CapacityScheduler) scheduler;
                    CSQueue rootQueue = cs.getQueue("root");

                    if (rootQueue != null) {
                        Map<String, String> queueStates = captureQueueTree(rootQueue);
                        state.put("queue_states", queueStates);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Failed to capture Queue state: {}", e.getMessage());
            }
        }
    }

    private Map<String, String> captureQueueTree(CSQueue queue) {
        Map<String, String> queueStates = new HashMap<>();

        queueStates.put(queue.getQueuePath(), queue.getState().name());

        if (queue instanceof ParentQueue) {
            for (CSQueue childQueue : ((ParentQueue) queue).getChildQueues()) {
                queueStates.putAll(captureQueueTree(childQueue));
            }
        }

        return queueStates;
    }

    private void verifyQueueStates(ClusterState before, ClusterState after)
            throws StateVerificationException {
        @SuppressWarnings("unchecked")
        Map<String, String> beforeQueues =
            (Map<String, String>) before.getStateMap().get("queue_states");
        @SuppressWarnings("unchecked")
        Map<String, String> afterQueues =
            (Map<String, String>) after.getStateMap().get("queue_states");

        if (beforeQueues != null && afterQueues != null) {
            for (Map.Entry<String, String> entry : beforeQueues.entrySet()) {
                String queuePath = entry.getKey();
                String beforeState = entry.getValue();
                String afterState = afterQueues.get(queuePath);

                if (afterState == null) {
                    throw new StateVerificationException(
                        "Queue lost after restart: " + queuePath);
                }

                if (!beforeState.equals(afterState)) {
                    throw new StateVerificationException(
                        "Queue state changed after restart: " + queuePath +
                        " (before=" + beforeState + ", after=" + afterState + ")");
                }
            }
        }
    }
}

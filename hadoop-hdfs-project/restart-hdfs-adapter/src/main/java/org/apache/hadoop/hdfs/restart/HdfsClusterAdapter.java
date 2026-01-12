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
package org.apache.hadoop.hdfs.restart;

import org.apache.hadoop.ha.HAServiceProtocol.HAServiceState;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSCluster.DataNodeProperties;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.restarttest.core.ClusterAdapter;
import org.restarttest.core.RestartMode;
import org.restarttest.health.HealthCheck;
import org.restarttest.state.StateCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for Hadoop MiniDFSCluster with StartupOption preservation.
 * <p>
 * This adapter provides restart capabilities for HDFS mini-clusters by directly
 * calling MiniDFSCluster's native restart methods. It preserves the original
 * StartupOption used when the NameNode was first started.
 * <p>
 * Supports restarting:
 * <ul>
 *   <li>NameNodes (role: "namenode" or "master")</li>
 *   <li>DataNodes (role: "datanode" or "worker")</li>
 *   <li>All nodes (role: "all")</li>
 * </ul>
 */
public class HdfsClusterAdapter implements ClusterAdapter<MiniDFSCluster> {
    private static final Logger LOG = LoggerFactory.getLogger(HdfsClusterAdapter.class);

    // Configurable delay for DELAYED_CRASH mode
    private static final long DEFAULT_CRASH_DELAY_MS = 500;
    private long crashDelayMs = DEFAULT_CRASH_DELAY_MS;

    private final NoOpStateCapture stateCapture;
    private final NoOpHealthCheck healthCheck;

    public HdfsClusterAdapter() {
        this.stateCapture = new NoOpStateCapture();
        this.healthCheck = new NoOpHealthCheck();
    }

    @Override
    public Class<MiniDFSCluster> getClusterType() {
        return MiniDFSCluster.class;
    }

    @Override
    public void restartNode(MiniDFSCluster cluster, String nodeRole,
                            int nodeIndex, RestartMode mode) throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        if ("namenode".equals(normalizedRole)) {
            restartNameNode(cluster, nodeIndex, mode);
        } else if ("datanode".equals(normalizedRole)) {
            restartDataNode(cluster, nodeIndex, mode);
        } else if ("all".equals(normalizedRole)) {
            restartAllNodes(cluster, "all", mode);
        } else {
            throw new IllegalArgumentException(
                "Unknown node role: " + nodeRole +
                ". Supported roles: namenode, datanode, master, worker, all");
        }
    }

    @Override
    public void restartAllNodes(MiniDFSCluster cluster, String nodeRole,
                                 RestartMode mode) throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        if ("namenode".equals(normalizedRole)) {
            int numNameNodes = cluster.getNumNameNodes();
            LOG.info("Restarting all {} NameNodes with mode {}", numNameNodes, mode);
            for (int i = 0; i < numNameNodes; i++) {
                restartNameNode(cluster, i, mode);
            }
        } else if ("datanode".equals(normalizedRole)) {
            int numDataNodes = cluster.getDataNodes().size();
            LOG.info("Restarting all {} DataNodes with mode {}", numDataNodes, mode);
            // Restart in reverse order to maintain index stability
            for (int i = numDataNodes - 1; i >= 0; i--) {
                restartDataNode(cluster, i, mode);
            }
        } else if ("all".equals(normalizedRole)) {
            // Restart NameNodes first, then DataNodes
            restartAllNodes(cluster, "namenode", mode);
            restartAllNodes(cluster, "datanode", mode);
        } else {
            throw new IllegalArgumentException(
                "Unknown node role: " + nodeRole);
        }
    }

    @Override
    public void waitActive(MiniDFSCluster cluster) throws Exception {
        LOG.info("Waiting for HDFS cluster to become active");
        cluster.waitActive();
        cluster.waitClusterUp();
        LOG.info("HDFS cluster is active");
    }

    @Override
    public StateCapture<MiniDFSCluster> getStateCapture() {
        return stateCapture;
    }

    @Override
    public HealthCheck<MiniDFSCluster> getHealthCheck() {
        return healthCheck;
    }

    @Override
    public int getNodeCount(MiniDFSCluster cluster, String nodeRole) throws Exception {
        String normalizedRole = normalizeRole(nodeRole);

        if ("namenode".equals(normalizedRole)) {
            return cluster.getNumNameNodes();
        } else if ("datanode".equals(normalizedRole)) {
            return cluster.getDataNodes().size();
        } else if ("all".equals(normalizedRole)) {
            return cluster.getNumNameNodes() + cluster.getDataNodes().size();
        } else {
            throw new IllegalArgumentException("Unknown node role: " + nodeRole);
        }
    }

    /**
     * Set the delay for DELAYED_CRASH mode.
     * @param delayMs delay in milliseconds
     */
    public void setCrashDelayMs(long delayMs) {
        this.crashDelayMs = delayMs;
    }

    /**
     * Get the current delay for DELAYED_CRASH mode.
     * @return delay in milliseconds
     */
    public long getCrashDelayMs() {
        return crashDelayMs;
    }

    /**
     * Normalize role names to standard HDFS terminology.
     * Maps "master" to "namenode" and "worker" to "datanode".
     */
    private String normalizeRole(String role) {
        String lower = role.toLowerCase();
        if ("master".equals(lower)) {
            return "namenode";
        } else if ("worker".equals(lower)) {
            return "datanode";
        }
        return lower;
    }

    /**
     * Restart a NameNode with the specified mode.
     * <p>
     * This method preserves both the original StartupOption from NameNodeInfo
     * and the HA state (Active/Standby/Observer) of the NameNode.
     * <p>
     * The StartupOption (e.g., REGULAR, UPGRADE, ROLLBACK) affects how the NameNode
     * initializes its state and is preserved across restarts by MiniDFSCluster.
     * <p>
     * In HA mode, a restarted NameNode comes up as Standby by default. This method
     * captures the HA state before restart and restores it afterward to maintain
     * cluster topology.
     *
     * @param cluster the MiniDFSCluster instance
     * @param nnIndex the index of the NameNode to restart
     * @param mode the restart mode (GRACEFUL, CRASH, DELAYED_CRASH)
     * @throws Exception if restart fails
     */
    private void restartNameNode(MiniDFSCluster cluster, int nnIndex, RestartMode mode)
            throws Exception {
        LOG.info("Restarting NameNode {} with mode {}", nnIndex, mode);

        // Capture the HA state before restart (for HA clusters)
        HAServiceState previousState = null;
        NameNode nn = cluster.getNameNode(nnIndex);
        if (nn != null && nn.getRpcServer() != null) {
            try {
                previousState = nn.getRpcServer().getServiceStatus().getState();
                LOG.info("NameNode {} HA state before restart: {}", nnIndex, previousState);
            } catch (Exception e) {
                LOG.debug("Could not get HA state for NameNode {} (may be non-HA cluster): {}",
                        nnIndex, e.getMessage());
            }
        }

        switch (mode) {
            case GRACEFUL:
                // Use built-in restart which handles StartupOption internally
                cluster.restartNameNode(nnIndex, true);
                break;

            case CRASH:
                // Shutdown abruptly (no graceful cleanup), then restart
                cluster.shutdownNameNode(nnIndex);
                // Restart with original StartupOption preserved by MiniDFSCluster
                cluster.restartNameNode(nnIndex, true);
                break;

            case DELAYED_CRASH:
                // Shutdown, wait, then restart
                cluster.shutdownNameNode(nnIndex);
                LOG.debug("Waiting {}ms before NameNode restart", crashDelayMs);
                Thread.sleep(crashDelayMs);
                cluster.restartNameNode(nnIndex, true);
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // Restore HA state if the NameNode was Active or Observer before restart
        if (previousState != null) {
            restoreHAState(cluster, nnIndex, previousState);
        }

        LOG.info("NameNode {} restarted successfully", nnIndex);
    }

    /**
     * Restore the HA state of a NameNode after restart.
     * <p>
     * In HA mode, a restarted NameNode comes up as Standby by default.
     * This method transitions the NameNode back to its previous state
     * (Active or Observer) to maintain the cluster topology.
     *
     * @param cluster the MiniDFSCluster instance
     * @param nnIndex the index of the NameNode
     * @param targetState the desired HA state
     */
    private void restoreHAState(MiniDFSCluster cluster, int nnIndex, HAServiceState targetState) {
        try {
            NameNode nn = cluster.getNameNode(nnIndex);
            if (nn == null || nn.getRpcServer() == null) {
                LOG.warn("NameNode {} is null or not ready after restart, cannot restore HA state", nnIndex);
                return;
            }

            HAServiceState currentState = nn.getRpcServer().getServiceStatus().getState();
            LOG.info("NameNode {} current HA state: {}, target state: {}",
                    nnIndex, currentState, targetState);

            if (currentState == targetState) {
                LOG.debug("NameNode {} already in target state {}", nnIndex, targetState);
                return;
            }

            switch (targetState) {
                case ACTIVE:
                    LOG.info("Transitioning NameNode {} to ACTIVE", nnIndex);
                    cluster.transitionToActive(nnIndex);
                    cluster.waitActive(nnIndex);
                    break;

                case OBSERVER:
                    LOG.info("Transitioning NameNode {} to OBSERVER", nnIndex);
                    cluster.transitionToObserver(nnIndex);
                    break;

                case STANDBY:
                    // NameNode comes up as Standby by default, no action needed
                    LOG.debug("NameNode {} target state is STANDBY, no transition needed", nnIndex);
                    break;

                default:
                    LOG.warn("Unknown or unsupported HA state {} for NameNode {}, skipping transition",
                            targetState, nnIndex);
            }

            LOG.info("NameNode {} HA state restored to {}", nnIndex, targetState);
        } catch (Exception e) {
            LOG.warn("Failed to restore HA state {} for NameNode {}: {}",
                    targetState, nnIndex, e.getMessage());
            // Log the full stack trace at debug level
            LOG.debug("HA state restoration failure details:", e);
        }
    }

    /**
     * Restart a DataNode with the specified mode.
     *
     * @param cluster the MiniDFSCluster instance
     * @param dnIndex the index of the DataNode to restart
     * @param mode the restart mode (GRACEFUL, CRASH, DELAYED_CRASH)
     * @throws Exception if restart fails
     */
    private void restartDataNode(MiniDFSCluster cluster, int dnIndex, RestartMode mode)
            throws Exception {
        LOG.info("Restarting DataNode {} with mode {}", dnIndex, mode);

        switch (mode) {
            case GRACEFUL:
                // Graceful restart keeps port for consistent re-registration
                if (!cluster.restartDataNode(dnIndex, true)) {
                    throw new Exception("Failed to restart DataNode " + dnIndex);
                }
                break;

            case CRASH:
                // Stop abruptly, then restart
                DataNodeProperties dnProp = cluster.stopDataNode(dnIndex);
                if (dnProp == null) {
                    throw new Exception("Failed to stop DataNode " + dnIndex);
                }
                if (!cluster.restartDataNode(dnProp, true)) {
                    throw new Exception("Failed to restart DataNode " + dnIndex);
                }
                break;

            case DELAYED_CRASH:
                // Stop, wait for state propagation, then restart
                DataNodeProperties dnProp2 = cluster.stopDataNode(dnIndex);
                if (dnProp2 == null) {
                    throw new Exception("Failed to stop DataNode " + dnIndex);
                }
                LOG.debug("Waiting {}ms before DataNode restart", crashDelayMs);
                Thread.sleep(crashDelayMs);
                if (!cluster.restartDataNode(dnProp2, true)) {
                    throw new Exception("Failed to restart DataNode " + dnIndex);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown restart mode: " + mode);
        }

        // Wait for DataNode to fully register
        cluster.waitActive();
        LOG.info("DataNode {} restarted successfully", dnIndex);
    }
}

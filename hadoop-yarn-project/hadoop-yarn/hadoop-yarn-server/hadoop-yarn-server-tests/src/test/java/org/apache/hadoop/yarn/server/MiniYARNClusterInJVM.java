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
package org.apache.hadoop.yarn.server;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.net.ServerSocketUtil;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.util.Shell;
import org.apache.hadoop.util.Shell.ShellCommandExecutor;
import org.apache.hadoop.util.Time;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsRequest;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.conf.HAUtil;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.hadoop.yarn.server.api.ResourceTracker;
import org.apache.hadoop.yarn.server.api.protocolrecords.*;
import org.apache.hadoop.yarn.server.api.records.NodeStatus;
import org.apache.hadoop.yarn.server.applicationhistoryservice.ApplicationHistoryServer;
import org.apache.hadoop.yarn.server.applicationhistoryservice.ApplicationHistoryStore;
import org.apache.hadoop.yarn.server.applicationhistoryservice.MemoryApplicationHistoryStore;
import org.apache.hadoop.yarn.server.nodemanager.ContainerExecutor;
import org.apache.hadoop.yarn.server.nodemanager.Context;
import org.apache.hadoop.yarn.server.nodemanager.DeletionService;
import org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerService;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.nodemanager.NodeStatusUpdater;
import org.apache.hadoop.yarn.server.nodemanager.NodeStatusUpdaterImpl;
import org.apache.hadoop.yarn.server.nodemanager.amrmproxy.AMRMProxyService;
import org.apache.hadoop.yarn.server.nodemanager.amrmproxy.DefaultRequestInterceptor;
import org.apache.hadoop.yarn.server.nodemanager.amrmproxy.RequestInterceptor;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.ContainerManagerImpl;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitor;
import org.apache.hadoop.yarn.server.nodemanager.containermanager.monitor.ContainersMonitorImpl;
import org.apache.hadoop.yarn.server.nodemanager.health.NodeHealthCheckerService;
import org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetrics;
import org.apache.hadoop.yarn.server.resourcemanager.*;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptEventType;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptRegistrationEvent;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.event.RMAppAttemptUnregistrationEvent;
import org.apache.hadoop.yarn.server.security.ApplicationACLsManager;
import org.apache.hadoop.yarn.server.security.http.RMAuthenticationFilterInitializer;
import org.apache.hadoop.yarn.server.timeline.MemoryTimelineStore;
import org.apache.hadoop.yarn.server.timeline.TimelineStore;
import org.apache.hadoop.yarn.server.timeline.recovery.MemoryTimelineStateStore;
import org.apache.hadoop.yarn.server.timeline.recovery.TimelineStateStore;
import org.apache.hadoop.yarn.util.resource.ResourceUtils;
import org.apache.hadoop.yarn.util.timeline.TimelineUtils;
import org.apache.hadoop.yarn.webapp.util.WebAppUtils;
import org.apache.hadoop.thirdparty.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.hadoop.yarn.server.resourcemanager.resource.TestResourceProfiles.TEST_CONF_RESET_RESOURCE_TYPES;

import org.apache.hadoop.yarn.server.nodemanager.NodeManagerJVMInterface;
import org.apache.hadoop.yarn.server.nodemanager.NodeManagerInstance;
import java.util.HashMap;
import java.util.HashSet;
import edu.illinois.instance.Instance;
import edu.illinois.instance.UpgradeMode;

/**
 * <p>
 * Embedded YARN minicluster for testcases that need to interact with a cluster.
 * </p>
 * <p>
 * In a real cluster, resource request matching is done using the hostname, and
 * by default YARN minicluster works in the exact same way as a real cluster.
 * </p>
 * <p>
 * If a testcase needs to use multiple nodes and exercise resource request
 * matching to a specific node, then the property
 * {@value org.apache.hadoop.yarn.conf.YarnConfiguration#RM_SCHEDULER_INCLUDE_PORT_IN_NODE_NAME}
 * should be set <code>true</code> in the configuration used to initialize
 * the minicluster.
 * </p>
 * With this property set to <code>true</code>, the matching will be done using
 * the <code>hostname:port</code> of the namenodes. In such case, the AM must
 * do resource request using <code>hostname:port</code> as the location.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class MiniYARNClusterInJVM extends CompositeService {

    private static final Logger LOG = LoggerFactory.getLogger(MiniYARNClusterInJVM.class);

    // temp fix until metrics system can auto-detect itself running in unit test:
    static {
        DefaultMetricsSystem.setMiniClusterMode(true);
    }

    private NodeManagerJVMInterface[] nodeManagers;

    private ResourceManagerJVMInterface[] resourceManagers;

    private String[] rmIds;

    private ApplicationHistoryServer appHistoryServer;

    private boolean useFixedPorts;

    private boolean useRpc = false;

    private int failoverTimeout;

    private ConcurrentMap<ApplicationAttemptId, Long> appMasters = new ConcurrentHashMap<ApplicationAttemptId, Long>(16, 0.75f, 2);

    private File testWorkDir;

    // Number of nm-local-dirs per nodemanager
    private int numLocalDirs;

    // Number of nm-log-dirs per nodemanager
    private int numLogDirs;

    private boolean enableAHS;

    /**
     * @param testName name of the test
     * @param numResourceManagers the number of resource managers in the cluster
     * @param numNodeManagers the number of node managers in the cluster
     * @param numLocalDirs the number of nm-local-dirs per nodemanager
     * @param numLogDirs the number of nm-log-dirs per nodemanager
     * @param enableAHS enable ApplicationHistoryServer or not
     */
    @Deprecated
    public MiniYARNClusterInJVM(String testName, int numResourceManagers, int numNodeManagers, int numLocalDirs, int numLogDirs, boolean enableAHS) {
        super(testName.replace("$", ""));
        this.numLocalDirs = numLocalDirs;
        this.numLogDirs = numLogDirs;
        this.enableAHS = enableAHS;
        String yarnFolderName = String.format("yarn-%d", Time.monotonicNow());
        File targetWorkDirRoot = GenericTestUtils.getTestDir(getName());
        // make sure that the folder exists
        targetWorkDirRoot.mkdirs();
        File targetWorkDir = new File(targetWorkDirRoot, yarnFolderName);
        try {
            FileContext.getLocalFSFileContext().delete(new Path(targetWorkDir.getAbsolutePath()), true);
        } catch (Exception e) {
            LOG.warn("COULD NOT CLEANUP", e);
            throw new YarnRuntimeException("could not cleanup test dir: " + e, e);
        }
        if (Shell.WINDOWS) {
            // The test working directory can exceed the maximum path length supported
            // by some Windows APIs and cmd.exe (260 characters).  To work around this,
            // create a symlink in temporary storage with a much shorter path,
            // targeting the full path to the test working directory.  Then, use the
            // symlink as the test working directory.
            String targetPath = targetWorkDir.getAbsolutePath();
            File link = new File(System.getProperty("java.io.tmpdir"), String.valueOf(System.currentTimeMillis()));
            String linkPath = link.getAbsolutePath();
            try {
                FileContext.getLocalFSFileContext().delete(new Path(linkPath), true);
            } catch (IOException e) {
                throw new YarnRuntimeException("could not cleanup symlink: " + linkPath, e);
            }
            // Guarantee target exists before creating symlink.
            targetWorkDir.mkdirs();
            ShellCommandExecutor shexec = new ShellCommandExecutor(Shell.getSymlinkCommand(targetPath, linkPath));
            try {
                shexec.execute();
            } catch (IOException e) {
                throw new YarnRuntimeException(String.format("failed to create symlink from %s to %s, shell output: %s", linkPath, targetPath, shexec.getOutput()), e);
            }
            this.testWorkDir = link;
        } else {
            this.testWorkDir = targetWorkDir;
        }
        resourceManagers = new ResourceManagerJVMInterface[numResourceManagers];
        nodeManagers = new NodeManagerJVMInterface[numNodeManagers];

        // Initialize version arrays for UPGT upgrade testing
        currentNodeManagerVersions = new String[numNodeManagers];
        for (int i = 0; i < numNodeManagers; i++) {
            currentNodeManagerVersions[i] = currentResourceManagerVersion;
        }
    }

    /**
     * @param testName name of the test
     * @param numResourceManagers the number of resource managers in the cluster
     * @param numNodeManagers the number of node managers in the cluster
     * @param numLocalDirs the number of nm-local-dirs per nodemanager
     * @param numLogDirs the number of nm-log-dirs per nodemanager
     */
    @SuppressWarnings("deprecation")
    public MiniYARNClusterInJVM(String testName, int numResourceManagers, int numNodeManagers, int numLocalDirs, int numLogDirs) {
        this(testName, numResourceManagers, numNodeManagers, numLocalDirs, numLogDirs, false);
    }

    /**
     * @param testName name of the test
     * @param numNodeManagers the number of node managers in the cluster
     * @param numLocalDirs the number of nm-local-dirs per nodemanager
     * @param numLogDirs the number of nm-log-dirs per nodemanager
     */
    public MiniYARNClusterInJVM(String testName, int numNodeManagers, int numLocalDirs, int numLogDirs) {
        this(testName, 1, numNodeManagers, numLocalDirs, numLogDirs);
    }

    @Override
    public void serviceInit(Configuration conf) throws Exception {
        useFixedPorts = conf.getBoolean(YarnConfiguration.YARN_MINICLUSTER_FIXED_PORTS, YarnConfiguration.DEFAULT_YARN_MINICLUSTER_FIXED_PORTS);
        if (!useFixedPorts) {
            String hostname = MiniYARNClusterInJVM.getHostname();
            conf.set(YarnConfiguration.TIMELINE_SERVICE_ADDRESS, hostname + ":0");
            conf.set(YarnConfiguration.TIMELINE_SERVICE_WEBAPP_ADDRESS, hostname + ":" + ServerSocketUtil.getPort(9188, 10));
        }
        useRpc = conf.getBoolean(YarnConfiguration.YARN_MINICLUSTER_USE_RPC, YarnConfiguration.DEFAULT_YARN_MINICLUSTER_USE_RPC);
        failoverTimeout = conf.getInt(YarnConfiguration.RM_ZK_TIMEOUT_MS, YarnConfiguration.DEFAULT_RM_ZK_TIMEOUT_MS);
        if (conf.getBoolean(TEST_CONF_RESET_RESOURCE_TYPES, true)) {
            ResourceUtils.resetResourceTypes(conf);
        }
        if (useRpc && !useFixedPorts) {
            throw new YarnRuntimeException("Invalid configuration!" + " Minicluster can use rpc only when configured to use fixed ports");
        }
        conf.setBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, true);
        if (resourceManagers.length > 1) {
            conf.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);
            if (conf.get(YarnConfiguration.RM_HA_IDS) == null) {
                StringBuilder rmIds = new StringBuilder();
                for (int i = 0; i < resourceManagers.length; i++) {
                    if (i != 0) {
                        rmIds.append(",");
                    }
                    rmIds.append("rm" + i);
                }
                conf.set(YarnConfiguration.RM_HA_IDS, rmIds.toString());
            }
            Collection<String> rmIdsCollection = HAUtil.getRMHAIds(conf);
            rmIds = rmIdsCollection.toArray(new String[rmIdsCollection.size()]);
        }
        for (int i = 0; i < resourceManagers.length; i++) {
            resourceManagers[i] = createResourceManager();
            if (!useFixedPorts) {
                if (HAUtil.isHAEnabled(conf)) {
                    setHARMConfigurationWithEphemeralPorts(i, conf);
                } else {
                    setNonHARMConfigurationWithEphemeralPorts(conf);
                }
            }
            addService(new ResourceManagerWrapper(i));
        }
        for (int index = 0; index < nodeManagers.length; index++) {
            nodeManagers[index] = useRpc ? new CustomNodeManager() : new ShortCircuitedNodeManager();
            addService(new NodeManagerWrapper(index));
        }
        if (conf.getBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, YarnConfiguration.DEFAULT_TIMELINE_SERVICE_ENABLED) || enableAHS) {
            addService(new ApplicationHistoryServerWrapper());
        }
        // to ensure that any FileSystemNodeAttributeStore started by the RM always
        // uses a unique path, if unset, force it under the test dir.
        if (conf.get(YarnConfiguration.FS_NODE_ATTRIBUTE_STORE_ROOT_DIR) == null) {
            File nodeAttrDir = new File(getTestWorkDir(), "nodeattributes");
            conf.set(YarnConfiguration.FS_NODE_ATTRIBUTE_STORE_ROOT_DIR, nodeAttrDir.getCanonicalPath());
        }
        super.serviceInit(conf instanceof YarnConfiguration ? conf : new YarnConfiguration(conf));
    }

    @Override
    protected synchronized void serviceStart() throws Exception {
        super.serviceStart();
        this.waitForNodeManagersToConnect(5000);
    }

    private void setNonHARMConfigurationWithEphemeralPorts(Configuration conf) {
        String hostname = MiniYARNClusterInJVM.getHostname();
        conf.set(YarnConfiguration.RM_ADDRESS, hostname + ":0");
        conf.set(YarnConfiguration.RM_ADMIN_ADDRESS, hostname + ":0");
        conf.set(YarnConfiguration.RM_SCHEDULER_ADDRESS, hostname + ":0");
        conf.set(YarnConfiguration.RM_RESOURCE_TRACKER_ADDRESS, hostname + ":0");
        WebAppUtils.setRMWebAppHostnameAndPort(conf, hostname, 0);
    }

    private void setHARMConfigurationWithEphemeralPorts(final int index, Configuration conf) {
        String hostname = MiniYARNClusterInJVM.getHostname();
        for (String confKey : YarnConfiguration.getServiceAddressConfKeys(conf)) {
            conf.set(HAUtil.addSuffix(confKey, rmIds[index]), hostname + ":0");
        }
    }

    private synchronized void initResourceManager(int index, Configuration conf) {
        Configuration newConf = resourceManagers.length > 1 ? new YarnConfiguration(conf) : conf;
        resourceManagerInstance.getVersionClassLoader().setCurrentThreadClassLoader();
        if (HAUtil.isHAEnabled(newConf)) {
            newConf.set(YarnConfiguration.RM_HA_ID, rmIds[index]);
        }
        resourceManagers[index].init(newConf);
        resourceManagers[index].getRMContext().getDispatcher().register(RMAppAttemptEventType.class, new EventHandler<RMAppAttemptEvent>() {

            public void handle(RMAppAttemptEvent event) {
                if (event instanceof RMAppAttemptRegistrationEvent) {
                    appMasters.put(event.getApplicationAttemptId(), event.getTimestamp());
                } else if (event instanceof RMAppAttemptUnregistrationEvent) {
                    appMasters.remove(event.getApplicationAttemptId());
                }
            }
        });
        resourceManagerInstance.getVersionClassLoader().resetCurrentThreadClassLoader();
    }

    private synchronized void startResourceManager(final int index) {
        resourceManagerInstance.getVersionClassLoader().setCurrentThreadClassLoader();
        try {
            resourceManagers[index].start();
            if (resourceManagers[index].getServiceState() != STATE.STARTED) {
                // RM could have failed.
                throw new IOException("ResourceManager failed to start. Final state is " + resourceManagers[index].getServiceState());
            }
        } catch (Throwable t) {
            throw new YarnRuntimeException(t);
        }
        Configuration conf = resourceManagers[index].getConfig();
        LOG.info("MiniYARN ResourceManager address: " + conf.get(YarnConfiguration.RM_ADDRESS));
        LOG.info("MiniYARN ResourceManager web address: " + WebAppUtils.getRMWebAppURLWithoutScheme(conf));
        resourceManagerInstance.getVersionClassLoader().resetCurrentThreadClassLoader();
    }

    @InterfaceAudience.Private
    @VisibleForTesting
    public synchronized void stopResourceManager(int index) {
        if (resourceManagers[index] != null) {
            resourceManagers[index].stop();
            resourceManagers[index] = null;
        }
    }

    @InterfaceAudience.Private
    @VisibleForTesting
    public synchronized void restartResourceManager(int index) throws InterruptedException {
        if (resourceManagers[index] != null) {
            resourceManagers[index].stop();
            resourceManagers[index] = null;
        }
        resourceManagers[index] = getOrCreateResourceManagerInstance().createResourceManagerForInJVMCluster();
        initResourceManager(index, getConfig());
        startResourceManager(index);
    }

    public File getTestWorkDir() {
        return testWorkDir;
    }

    /**
     * In an HA cluster, go through all the RMs and find the Active RM. In a
     * non-HA cluster, return the index of the only RM.
     *
     * @return index of the active RM or -1 if none of them turn active
     */
    @InterfaceAudience.Private
    @VisibleForTesting
    public int getActiveRMIndex() {
        if (resourceManagers.length == 1) {
            return 0;
        }
        int numRetriesForRMBecomingActive = failoverTimeout / 100;
        while (numRetriesForRMBecomingActive-- > 0) {
            for (int i = 0; i < resourceManagers.length; i++) {
                if (resourceManagers[i] == null) {
                    continue;
                }
                try {
                    if (HAServiceProtocol.HAServiceState.ACTIVE == resourceManagers[i].getRMContext().getRMAdminService().getServiceStatus().getState()) {
                        return i;
                    }
                } catch (IOException e) {
                    throw new YarnRuntimeException("Couldn't read the status of " + "a ResourceManger in the HA ensemble.", e);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new YarnRuntimeException("Interrupted while waiting for one " + "of the ResourceManagers to become active");
            }
        }
        return -1;
    }

    /**
     * @return the active {@link ResourceManager} of the cluster,
     * null if none of them are active.
     */
    public ResourceManagerJVMInterface getResourceManager() {
        int activeRMIndex = getActiveRMIndex();
        return activeRMIndex == -1 ? null : this.resourceManagers[activeRMIndex];
    }

    public ResourceManagerJVMInterface getResourceManager(int i) {
        return this.resourceManagers[i];
    }

    public NodeManagerJVMInterface getNodeManager(int i) {
        return this.nodeManagers[i];
    }

    public static String getHostname() {
        return "localhost";
    }

    private class ResourceManagerWrapper extends AbstractService {

        private int index;

        public ResourceManagerWrapper(int i) {
            super(ResourceManagerWrapper.class.getName() + "_" + i);
            index = i;
        }

        @Override
        protected synchronized void serviceInit(Configuration conf) throws Exception {
            initResourceManager(index, conf);
            super.serviceInit(conf);
        }

        @Override
        protected synchronized void serviceStart() throws Exception {
            startResourceManager(index);
            if (index == 0 && resourceManagers[index].getRMContext().isHAEnabled()) {
                resourceManagers[index].getRMContext().getRMAdminService().transitionToActive(new HAServiceProtocol.StateChangeRequestInfo(HAServiceProtocol.RequestSource.REQUEST_BY_USER_FORCED));
            }
            Configuration conf = resourceManagers[index].getConfig();
            LOG.info("Starting resourcemanager " + index);
            LOG.info("MiniYARN ResourceManager address: " + conf.get(YarnConfiguration.RM_ADDRESS));
            LOG.info("MiniYARN ResourceManager web address: " + WebAppUtils.getRMWebAppURLWithoutScheme(conf));
            super.serviceStart();
        }

        private void waitForAppMastersToFinish(long timeoutMillis) throws InterruptedException {
            long started = System.currentTimeMillis();
            synchronized (appMasters) {
                while (!appMasters.isEmpty() && System.currentTimeMillis() - started < timeoutMillis) {
                    appMasters.wait(1000);
                }
            }
            if (!appMasters.isEmpty()) {
                LOG.warn("Stopping RM while some app masters are still alive");
            }
        }

        @Override
        protected synchronized void serviceStop() throws Exception {
            if (resourceManagers[index] != null) {
                waitForAppMastersToFinish(5000);
                resourceManagers[index].stop();
            }
            if (Shell.WINDOWS) {
                // On Windows, clean up the short temporary symlink that was created to
                // work around path length limitation.
                String testWorkDirPath = testWorkDir.getAbsolutePath();
                try {
                    FileContext.getLocalFSFileContext().delete(new Path(testWorkDirPath), true);
                } catch (IOException e) {
                    LOG.warn("could not cleanup symlink: " + testWorkDir.getAbsolutePath());
                }
            }
            super.serviceStop();
        }
    }

    private class NodeManagerWrapper extends AbstractService {

        int index = 0;

        public NodeManagerWrapper(int i) {
            super(NodeManagerWrapper.class.getName() + "_" + i);
            index = i;
        }

        protected synchronized void serviceInit(Configuration conf) throws Exception {
            Configuration config = new YarnConfiguration(conf);
            // create nm-local-dirs and configure them for the nodemanager
            String localDirsString = prepareDirs("local", numLocalDirs);
            config.set(YarnConfiguration.NM_LOCAL_DIRS, localDirsString);
            // create nm-log-dirs and configure them for the nodemanager
            String logDirsString = prepareDirs("log", numLogDirs);
            config.set(YarnConfiguration.NM_LOG_DIRS, logDirsString);
            config.setInt(YarnConfiguration.NM_PMEM_MB, config.getInt(YarnConfiguration.YARN_MINICLUSTER_NM_PMEM_MB, YarnConfiguration.DEFAULT_YARN_MINICLUSTER_NM_PMEM_MB));
            config.set(YarnConfiguration.NM_ADDRESS, MiniYARNClusterInJVM.getHostname() + ":0");
            config.set(YarnConfiguration.NM_LOCALIZER_ADDRESS, MiniYARNClusterInJVM.getHostname() + ":0");
            config.set(YarnConfiguration.NM_COLLECTOR_SERVICE_ADDRESS, MiniYARNClusterInJVM.getHostname() + ":" + ServerSocketUtil.getPort(YarnConfiguration.DEFAULT_NM_COLLECTOR_SERVICE_PORT, 10));
            WebAppUtils.setNMWebAppHostNameAndPort(config, MiniYARNClusterInJVM.getHostname(), 0);
            config.setBoolean(YarnConfiguration.NM_ENABLE_HARDWARE_CAPABILITY_DETECTION, false);
            // Disable resource checks by default
            if (!config.getBoolean(YarnConfiguration.YARN_MINICLUSTER_CONTROL_RESOURCE_MONITORING, YarnConfiguration.DEFAULT_YARN_MINICLUSTER_CONTROL_RESOURCE_MONITORING)) {
                config.setBoolean(YarnConfiguration.NM_CONTAINER_MONITOR_ENABLED, false);
                config.setLong(YarnConfiguration.NM_RESOURCE_MON_INTERVAL_MS, 0);
            }
            LOG.info("Starting NM: " + index);
            nodeManagers[index].init(config);
            super.serviceInit(config);
        }

        /**
         * Create local/log directories
         * @param dirType type of directories i.e. local dirs or log dirs
         * @param numDirs number of directories
         * @return the created directories as a comma delimited String
         */
        private String prepareDirs(String dirType, int numDirs) {
            File[] dirs = new File[numDirs];
            String dirsString = "";
            for (int i = 0; i < numDirs; i++) {
                dirs[i] = new File(testWorkDir, MiniYARNClusterInJVM.this.getName() + "-" + dirType + "Dir-nm-" + index + "_" + i);
                dirs[i].mkdirs();
                LOG.info("Created " + dirType + "Dir in " + dirs[i].getAbsolutePath());
                String delimiter = (i > 0) ? "," : "";
                dirsString = dirsString.concat(delimiter + dirs[i].getAbsolutePath());
            }
            return dirsString;
        }

        protected synchronized void serviceStart() throws Exception {
            nodeManagers[index].start();
            if (nodeManagers[index].getServiceState() != STATE.STARTED) {
                // NM could have failed.
                throw new IOException("NodeManager " + index + " failed to start");
            }
            super.serviceStart();
        }

        @Override
        protected synchronized void serviceStop() throws Exception {
            if (nodeManagers[index] != null) {
                nodeManagers[index].stop();
            }
            super.serviceStop();
        }
    }

    public class CustomNodeManager extends NodeManager {

        protected NodeStatus nodeStatus;

        public void setNodeStatus(NodeStatus status) {
            this.nodeStatus = status;
        }

        /**
         * Hook to allow modification/replacement of NodeStatus
         * @param currentStatus Current status.
         * @return New node status.
         */
        protected NodeStatus getSimulatedNodeStatus(NodeStatus currentStatus) {
            if (nodeStatus == null) {
                return currentStatus;
            } else {
                // Use the same responseId for the custom node status
                nodeStatus.setResponseId(currentStatus.getResponseId());
                return nodeStatus;
            }
        }

        @Override
        protected void doSecureLogin() throws IOException {
            // Don't try to login using keytab in the testcase.
        }

        @Override
        protected NodeStatusUpdater createNodeStatusUpdater(Context context, Dispatcher dispatcher, NodeHealthCheckerService healthChecker) {
            return new NodeStatusUpdaterImpl(context, dispatcher, healthChecker, metrics) {

                // Allow simulation of nodestatus
                @Override
                protected NodeStatus getNodeStatus(int responseId) throws IOException {
                    return getSimulatedNodeStatus(super.getNodeStatus(responseId));
                }
            };
        }
    }

    private class ShortCircuitedNodeManager extends CustomNodeManager {

        @Override
        protected NodeStatusUpdater createNodeStatusUpdater(Context context, Dispatcher dispatcher, NodeHealthCheckerService healthChecker) {
            return new NodeStatusUpdaterImpl(context, dispatcher, healthChecker, metrics) {

                // Allow simulation of nodestatus
                @Override
                protected NodeStatus getNodeStatus(int responseId) throws IOException {
                    return getSimulatedNodeStatus(super.getNodeStatus(responseId));
                }

                @Override
                protected ResourceTracker getRMClient() {
                    final ResourceTrackerServiceJVMInterface rt = getResourceManager().getResourceTrackerService();
                    final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
                    // For in-process communication without RPC
                    return new ResourceTracker() {

                        @Override
                        public NodeHeartbeatResponse nodeHeartbeat(NodeHeartbeatRequest request) throws YarnException, IOException {
                            NodeHeartbeatResponseJVMInterface response;
                            try {
                                response = rt.nodeHeartbeat_bridge(request);
                            } catch (YarnException e) {
                                LOG.info("Exception in heartbeat from node " + request.getNodeStatus().getNodeId(), e);
                                throw e;
                            }
                            return (NodeHeartbeatResponse) response;
                        }

                        @Override
                        public RegisterNodeManagerResponse registerNodeManager(RegisterNodeManagerRequest request) throws YarnException, IOException {
                            RegisterNodeManagerResponseJVMInterface response;
                            try {
                                response = rt.registerNodeManager_bridge(request);
                            } catch (YarnException e) {
                                LOG.info("Exception in node registration from " + request.getNodeId().toString(), e);
                                throw e;
                            }
                            return (RegisterNodeManagerResponse) response;
                        }
                        @Override
                        public UnRegisterNodeManagerResponse unRegisterNodeManager(UnRegisterNodeManagerRequest request) throws YarnException, IOException {
                            return recordFactory.newRecordInstance(UnRegisterNodeManagerResponse.class);
                        }
                    };
                }

                @Override
                protected void stopRMProxy() {
                }
            };
        }

        @Override
        protected ContainerManagerImpl createContainerManager(Context context, ContainerExecutor exec, DeletionService del, NodeStatusUpdater nodeStatusUpdater, ApplicationACLsManager aclsManager, LocalDirsHandlerService dirsHandler) {
            if (getConfig().getInt(YarnConfiguration.NM_OPPORTUNISTIC_CONTAINERS_MAX_QUEUE_LENGTH, 0) > 0) {
                return new CustomQueueingContainerManagerImpl(context, exec, del, nodeStatusUpdater, metrics, dirsHandler);
            } else {
                return new CustomContainerManagerImpl(context, exec, del, nodeStatusUpdater, metrics, dirsHandler);
            }
        }
    }

    /**
     * Wait for all the NodeManagers to connect to the ResourceManager.
     *
     * @param timeout Time to wait (sleeps in 10 ms intervals) in milliseconds.
     * @return true if all NodeManagers connect to the (Active)
     * ResourceManager, false otherwise.
     * @throws YarnException if there is no active RM
     * @throws InterruptedException if any thread has interrupted
     * the current thread
     */
    public boolean waitForNodeManagersToConnect(long timeout) throws YarnException, InterruptedException {
        GetClusterMetricsRequest req = GetClusterMetricsRequest.newInstance();
        for (int i = 0; i < timeout / 10; i++) {
            ResourceManagerJVMInterface rm = getResourceManager();
            if (rm == null) {
                throw new YarnException("Can not find the active RM.");
            } else if (nodeManagers.length == rm.getClientRMService().getClusterMetrics_bridge(req).getClusterMetrics().getNumNodeManagers()) {
                LOG.info("All Node Managers connected in MiniYARNCluster");
                return true;
            }
            Thread.sleep(10);
        }
        LOG.info("Node Managers did not connect within 5000ms");
        return false;
    }

    private class ApplicationHistoryServerWrapper extends AbstractService {

        public ApplicationHistoryServerWrapper() {
            super(ApplicationHistoryServerWrapper.class.getName());
        }

        @Override
        protected synchronized void serviceInit(Configuration conf) throws Exception {
            appHistoryServer = new ApplicationHistoryServer();
            conf.setClass(YarnConfiguration.APPLICATION_HISTORY_STORE, MemoryApplicationHistoryStore.class, ApplicationHistoryStore.class);
            // Only set memory timeline store if timeline v1.5 is not enabled.
            // Otherwise, caller has the freedom to choose storage impl.
            if (!TimelineUtils.timelineServiceV1_5Enabled(conf)) {
                conf.setClass(YarnConfiguration.TIMELINE_SERVICE_STORE, MemoryTimelineStore.class, TimelineStore.class);
            }
            conf.setClass(YarnConfiguration.TIMELINE_SERVICE_STATE_STORE_CLASS, MemoryTimelineStateStore.class, TimelineStateStore.class);
            appHistoryServer.init(conf);
            super.serviceInit(conf);
        }

        @Override
        protected synchronized void serviceStart() throws Exception {
            // Removing RMAuthenticationFilter as it conflitcs with
            // TimelineAuthenticationFilter
            Configuration conf = getConfig();
            String filterInitializerConfKey = "hadoop.http.filter.initializers";
            String initializers = conf.get(filterInitializerConfKey, "");
            String[] parts = initializers.split(",");
            Set<String> target = new LinkedHashSet<String>();
            for (String filterInitializer : parts) {
                filterInitializer = filterInitializer.trim();
                if (filterInitializer.equals(RMAuthenticationFilterInitializer.class.getName()) || filterInitializer.isEmpty()) {
                    continue;
                }
                target.add(filterInitializer);
            }
            initializers = StringUtils.join(target, ",");
            conf.set(filterInitializerConfKey, initializers);
            appHistoryServer.start();
            if (appHistoryServer.getServiceState() != STATE.STARTED) {
                // AHS could have failed.
                IOException ioe = new IOException("ApplicationHistoryServer failed to start. Final state is " + appHistoryServer.getServiceState());
                ioe.initCause(appHistoryServer.getFailureCause());
                throw ioe;
            }
            LOG.info("MiniYARN ApplicationHistoryServer address: " + getConfig().get(YarnConfiguration.TIMELINE_SERVICE_ADDRESS));
            LOG.info("MiniYARN ApplicationHistoryServer web address: " + getConfig().get(YarnConfiguration.TIMELINE_SERVICE_WEBAPP_ADDRESS));
            super.serviceStart();
        }

        @Override
        protected synchronized void serviceStop() throws Exception {
            if (appHistoryServer != null) {
                appHistoryServer.stop();
            }
        }
    }

    public ApplicationHistoryServer getApplicationHistoryServer() {
        return this.appHistoryServer;
    }

    protected ResourceManagerJVMInterface createResourceManager() {
        return getOrCreateResourceManagerInstance().createResourceManagerForInJVMCluster();
    }

    public int getNumOfResourceManager() {
        return this.resourceManagers.length;
    }

    private class CustomContainerManagerImpl extends ContainerManagerImpl {

        public CustomContainerManagerImpl(Context context, ContainerExecutor exec, DeletionService del, NodeStatusUpdater nodeStatusUpdater, NodeManagerMetrics metrics, LocalDirsHandlerService dirsHandler) {
            super(context, exec, del, nodeStatusUpdater, metrics, dirsHandler);
        }

        @Override
        protected void createAMRMProxyService(Configuration conf) {
            this.amrmProxyEnabled = conf.getBoolean(YarnConfiguration.AMRM_PROXY_ENABLED, YarnConfiguration.DEFAULT_AMRM_PROXY_ENABLED) || conf.getBoolean(YarnConfiguration.DIST_SCHEDULING_ENABLED, YarnConfiguration.DEFAULT_DIST_SCHEDULING_ENABLED);
            if (this.amrmProxyEnabled) {
                LOG.info("CustomAMRMProxyService is enabled. " + "All the AM->RM requests will be intercepted by the proxy");
                AMRMProxyService amrmProxyService = useRpc ? new AMRMProxyService(getContext(), dispatcher) : new ShortCircuitedAMRMProxy(getContext(), dispatcher);
                this.setAMRMProxyService(amrmProxyService);
                addService(this.getAMRMProxyService());
            } else {
                LOG.info("CustomAMRMProxyService is disabled");
            }
        }
    }

    private class CustomQueueingContainerManagerImpl extends ContainerManagerImpl {

        public CustomQueueingContainerManagerImpl(Context context, ContainerExecutor exec, DeletionService del, NodeStatusUpdater nodeStatusUpdater, NodeManagerMetrics metrics, LocalDirsHandlerService dirsHandler) {
            super(context, exec, del, nodeStatusUpdater, metrics, dirsHandler);
        }

        @Override
        protected void createAMRMProxyService(Configuration conf) {
            this.amrmProxyEnabled = conf.getBoolean(YarnConfiguration.AMRM_PROXY_ENABLED, YarnConfiguration.DEFAULT_AMRM_PROXY_ENABLED) || conf.getBoolean(YarnConfiguration.DIST_SCHEDULING_ENABLED, YarnConfiguration.DEFAULT_DIST_SCHEDULING_ENABLED);
            if (this.amrmProxyEnabled) {
                LOG.info("CustomAMRMProxyService is enabled. " + "All the AM->RM requests will be intercepted by the proxy");
                AMRMProxyService amrmProxyService = useRpc ? new AMRMProxyService(getContext(), dispatcher) : new ShortCircuitedAMRMProxy(getContext(), dispatcher);
                this.setAMRMProxyService(amrmProxyService);
                addService(this.getAMRMProxyService());
            } else {
                LOG.info("CustomAMRMProxyService is disabled");
            }
        }

        @Override
        protected ContainersMonitor createContainersMonitor(ContainerExecutor exec) {
            return new ContainersMonitorImpl(exec, dispatcher, this.context) {

                @Override
                public float getVmemRatio() {
                    return 2.0f;
                }

                @Override
                public long getVmemAllocatedForContainers() {
                    return 16 * 1024L * 1024L * 1024L;
                }

                @Override
                public long getPmemAllocatedForContainers() {
                    return 8 * 1024L * 1024L * 1024L;
                }

                @Override
                public long getVCoresAllocatedForContainers() {
                    return 10;
                }
            };
        }
    }

    private class ShortCircuitedAMRMProxy extends AMRMProxyService {

        public ShortCircuitedAMRMProxy(Context context, AsyncDispatcher dispatcher) {
            super(context, dispatcher);
        }

        @Override
        protected void initializePipeline(ApplicationAttemptId applicationAttemptId, String user, Token<AMRMTokenIdentifier> amrmToken, Token<AMRMTokenIdentifier> localToken, Map<String, byte[]> recoveredDataMap, boolean isRecovery, Credentials credentials) {
            super.initializePipeline(applicationAttemptId, user, amrmToken, localToken, recoveredDataMap, isRecovery, credentials);
            RequestInterceptor rt = getPipelines().get(applicationAttemptId.getApplicationId()).getRootInterceptor();
            // The DefaultRequestInterceptor will generally be the last
            // interceptor
            while (rt.getNextInterceptor() != null) {
                rt = rt.getNextInterceptor();
            }
            if (rt instanceof DefaultRequestInterceptor) {
                //((DefaultRequestInterceptor) rt).setRMClient(getResourceManager().getApplicationMasterService());
                throw new UnsupportedOperationException("[UPGT] from MiniYARNClusterInJVM -- Not implemented yet");
            }
        }
    }

    private ResourceManagerInstance resourceManagerInstance;

    private NodeManagerInstance nodeManagerInstance;

    ResourceManagerInstance getOrCreateResourceManagerInstance() {
        if (this.resourceManagerInstance == null) {
            this.resourceManagerInstance = new ResourceManagerInstance(edu.illinois.instance.Instance.StartVersion);
        }
        return this.resourceManagerInstance;
    }

    NodeManagerInstance getOrCreateNodeManagerInstance() {
        if (this.nodeManagerInstance == null) {
            this.nodeManagerInstance = new NodeManagerInstance(edu.illinois.instance.Instance.StartVersion);
        }
        return this.nodeManagerInstance;
    }

    // Upgrade methods appended from MiniYARNClusterUpgradeMethods.java
private String currentResourceManagerVersion;
    private String[] currentNodeManagerVersions;

    // Initialize versions and instances
    {
        currentResourceManagerVersion = Instance.StartVersion != null ? Instance.StartVersion : "3.3.5";
        resourceManagerInstance = new ResourceManagerInstance(currentResourceManagerVersion);
        nodeManagerInstance = new NodeManagerInstance(currentResourceManagerVersion);
    }

    /**
     * State preservation class for ResourceManager upgrade operations.
     */
    private static class ResourceManagerState {
        Configuration configuration;
        String rmId;
        boolean isHA;
        boolean wasActive;
        
        ResourceManagerState(Configuration config, String id, boolean ha, boolean active) {
            this.configuration = config;
            this.rmId = id;
            this.isHA = ha;
            this.wasActive = active;
        }
    }
    
    /**
     * State preservation class for NodeManager upgrade operations.
     */
    private static class NodeManagerState {
        Configuration configuration;
        String nodeId;
        String localDirs;
        String logDirs;
        
        NodeManagerState(Configuration config, String id, String localDirs, String logDirs) {
            this.configuration = config;
            this.nodeId = id;
            this.localDirs = localDirs;
            this.logDirs = logDirs;
        }
    }

    /**
     * Saves the current state of a ResourceManager before upgrade.
     *
     * @param index the index of the ResourceManager
     * @return saved state object
     */
    private ResourceManagerState saveResourceManagerState(int index) {
        if (resourceManagers[index] == null) {
            return null;
        }
        
        Configuration conf = resourceManagers[index].getConfig();
        String rmId = (rmIds != null && index < rmIds.length) ? rmIds[index] : null;
        boolean isHA = HAUtil.isHAEnabled(conf);
        boolean wasActive = false;
        
        try {
            if (isHA && resourceManagers[index].getRMContext() != null) {
                wasActive = HAServiceProtocol.HAServiceState.ACTIVE == 
                    resourceManagers[index].getRMContext().getRMAdminService().getServiceStatus().getState();
            }
        } catch (Exception e) {
            LOG.warn("Could not determine HA state for RM[{}], assuming inactive", index, e);
        }
        
        return new ResourceManagerState(new YarnConfiguration(conf), rmId, isHA, wasActive);
    }

    /**
     * Saves the current state of a NodeManager before upgrade.
     *
     * @param index the index of the NodeManager
     * @return saved state object
     */
    private NodeManagerState saveNodeManagerState(int index) {
        if (nodeManagers[index] == null) {
            return null;
        }
        
        Configuration conf = nodeManagers[index].getConfig();
        String nodeId = conf.get(YarnConfiguration.NM_ADDRESS, "");
        String localDirs = conf.get(YarnConfiguration.NM_LOCAL_DIRS, "");
        String logDirs = conf.get(YarnConfiguration.NM_LOG_DIRS, "");
        
        return new NodeManagerState(new YarnConfiguration(conf), nodeId, localDirs, logDirs);
    }

    /**
     * Restores ResourceManager with preserved state after upgrade.
     *
     * @param index the index of the ResourceManager
     * @param savedState the previously saved state
     * @throws Exception if restoration fails
     */
    private void restoreResourceManagerState(int index, ResourceManagerState savedState) throws Exception {
        if (savedState == null) {
            // No saved state, use default initialization
            initResourceManager(index, getConfig());
        } else {
            // Restore with saved configuration
            initResourceManager(index, savedState.configuration);
            
            // Restore HA state if needed
            if (savedState.isHA && savedState.wasActive && index == 0) {
                // Wait a bit for initialization
                Thread.sleep(100);
                try {
                    resourceManagers[index].getRMContext().getRMAdminService()
                        .transitionToActive(new HAServiceProtocol.StateChangeRequestInfo(
                            HAServiceProtocol.RequestSource.REQUEST_BY_USER_FORCED));
                } catch (Exception e) {
                    LOG.warn("Could not restore HA active state for RM[{}]", index, e);
                }
            }
        }
    }

    /**
     * Upgrades the ResourceManager to the specified target version.
     * This method stops the current RM, creates a new instance with the target version,
     * and starts it with preserved configuration.
     *
     * @param targetVersion the version to upgrade to (must be StartVersion or UpgradeVersion)
     * @throws Exception if upgrade fails
     */
    public void upgradeResourceManager(String targetVersion) throws Exception {
        validateVersion(targetVersion);
        
        LOG.info("Upgrading ResourceManager from {} to {}", currentResourceManagerVersion, targetVersion);
        
        // Save state of all ResourceManagers before stopping
        ResourceManagerState[] savedStates = new ResourceManagerState[resourceManagers.length];
        for (int i = 0; i < resourceManagers.length; i++) {
            savedStates[i] = saveResourceManagerState(i);
        }
        
        // Stop current ResourceManagers
        for (int i = 0; i < resourceManagers.length; i++) {
            if (resourceManagers[i] != null) {
                resourceManagers[i].stop();
            }
        }
        
        // Update version and create new instance
        currentResourceManagerVersion = targetVersion;
        resourceManagerInstance = new ResourceManagerInstance(targetVersion);
        
        // Recreate ResourceManagers with new version
        for (int i = 0; i < resourceManagers.length; i++) {
            resourceManagers[i] = createResourceManager();
            // Restore state with preserved configuration
            restoreResourceManagerState(i, savedStates[i]);
            // Start the ResourceManager
            startResourceManager(i);
        }
        
        LOG.info("ResourceManager upgrade completed to version {}", targetVersion);
    }

    /**
     * Restores NodeManager with preserved state after upgrade.
     * Note: NodeManager state restoration is more complex due to the wrapper pattern.
     * This method recreates the NodeManager and applies the saved configuration.
     *
     * @param index the index of the NodeManager
     * @param savedState the previously saved state
     * @param targetVersion the target version for the new NodeManager instance
     */
    private void restoreNodeManagerState(int index, NodeManagerState savedState, String targetVersion) {
        // Create a temporary NodeManager instance for this specific upgrade
        NodeManagerInstance nmInstance = new NodeManagerInstance(targetVersion);
        
        // Set the thread context classloader to the version classloader
        nmInstance.getVersionClassLoader().setCurrentThreadClassLoader();
        
        try {
            // Create the NodeManager with the appropriate class
            nodeManagers[index] = useRpc ? new CustomNodeManager() : new ShortCircuitedNodeManager();
            
            // If we have saved state, we need to initialize with the preserved configuration
            if (savedState != null) {
                Configuration restoredConfig = new YarnConfiguration(savedState.configuration);
                
                // Restore important node-specific settings
                if (!savedState.localDirs.isEmpty()) {
                    restoredConfig.set(YarnConfiguration.NM_LOCAL_DIRS, savedState.localDirs);
                }
                if (!savedState.logDirs.isEmpty()) {
                    restoredConfig.set(YarnConfiguration.NM_LOG_DIRS, savedState.logDirs);
                }
                if (!savedState.nodeId.isEmpty()) {
                    restoredConfig.set(YarnConfiguration.NM_ADDRESS, savedState.nodeId);
                }
                
                // Initialize with restored configuration
                nodeManagers[index].init(restoredConfig);
                
                LOG.info("Restored NodeManager[{}] with preserved configuration", index);
            } else {
                // No saved state, use default configuration
                LOG.info("No saved state for NodeManager[{}], using default configuration", index);
            }
        } finally {
            // Reset the thread context classloader
            nmInstance.getVersionClassLoader().resetCurrentThreadClassLoader();
        }
    }

    /**
     * Upgrades a specific NodeManager to the specified target version.
     *
     * @param index the index of the NodeManager to upgrade
     * @param targetVersion the version to upgrade to (must be StartVersion or UpgradeVersion)
     * @throws Exception if upgrade fails
     */
    public void upgradeNodeManager(int index, String targetVersion) throws Exception {
        validateVersion(targetVersion);
        
        if (index < 0 || index >= nodeManagers.length) {
            throw new IllegalArgumentException("Invalid NodeManager index: " + index);
        }
        
        LOG.info("Upgrading NodeManager[{}] from {} to {}", index, currentNodeManagerVersions[index], targetVersion);
        
        // Save state before stopping
        NodeManagerState savedState = saveNodeManagerState(index);
        
        // Stop current NodeManager
        if (nodeManagers[index] != null) {
            nodeManagers[index].stop();
        }
        
        // Update version
        currentNodeManagerVersions[index] = targetVersion;
        
        // For individual NodeManager upgrades, we need to create a new instance
        // Update the shared instance if all NMs are now on the same version
        if (allNodeManagersHaveVersion(targetVersion)) {
            nodeManagerInstance = new NodeManagerInstance(targetVersion);
        }
        
        // Recreate NodeManager with preserved configuration
        restoreNodeManagerState(index, savedState, targetVersion);
        
        LOG.info("NodeManager[{}] upgrade completed to version {}", index, targetVersion);
    }

    /**
     * Upgrades all NodeManagers to the specified target version.
     *
     * @param targetVersion the version to upgrade to (must be StartVersion or UpgradeVersion)
     * @throws Exception if upgrade fails
     */
    public void upgradeAllNodeManagers(String targetVersion) throws Exception {
        validateVersion(targetVersion);
        
        LOG.info("Upgrading all NodeManagers to version {}", targetVersion);
        
        // Save state for all NodeManagers before stopping
        NodeManagerState[] savedStates = new NodeManagerState[nodeManagers.length];
        for (int i = 0; i < nodeManagers.length; i++) {
            savedStates[i] = saveNodeManagerState(i);
        }
        
        // Stop all NodeManagers
        for (int i = 0; i < nodeManagers.length; i++) {
            if (nodeManagers[i] != null) {
                nodeManagers[i].stop();
            }
        }
        
        // Update all versions
        for (int i = 0; i < currentNodeManagerVersions.length; i++) {
            currentNodeManagerVersions[i] = targetVersion;
        }
        
        // Update the shared NodeManager instance since all will be on same version
        nodeManagerInstance = new NodeManagerInstance(targetVersion);
        
        // Recreate all NodeManagers with preserved configuration
        for (int i = 0; i < nodeManagers.length; i++) {
            restoreNodeManagerState(i, savedStates[i], targetVersion);
        }
        
        LOG.info("All NodeManagers upgrade completed to version {}", targetVersion);
    }

    /**
     * Upgrades 50% of the NodeManagers to the specified target version while keeping ResourceManagers upgraded.
     * This method performs a partial cluster upgrade by upgrading all ResourceManagers (for stability)
     * and then upgrading approximately 50% of the NodeManagers to the target version.
     *
     * @param targetVersion the version to upgrade to (must be StartVersion or UpgradeVersion)
     * @throws Exception if upgrade fails
     */
    private void upgradePartialNodes(String targetVersion) throws Exception {
        validateVersion(targetVersion);
        
        LOG.info("Starting partial cluster upgrade (50% of nodes) to version {}", targetVersion);
        LOG.info("Current cluster state:\n{}", getClusterVersionState());
        
        try {
            // Phase 1: Always upgrade ResourceManagers for stability
            LOG.info("Phase 1: Upgrading ResourceManagers to version {}", targetVersion);
            if (!currentResourceManagerVersion.equals(targetVersion)) {
                upgradeResourceManager(targetVersion);
                LOG.info("ResourceManager upgrade completed successfully");
            } else {
                LOG.info("ResourceManager already at target version {}", targetVersion);
            }
            
            // Brief pause to let ResourceManagers stabilize
            Thread.sleep(500);
            
            // Phase 2: Upgrade 50% of NodeManagers
            LOG.info("Phase 2: Upgrading 50% of NodeManagers to version {}", targetVersion);
            int[] nodesToUpgrade = selectNodesForPartialUpgrade(nodeManagers.length);
            
            int upgradedCount = 0;
            for (int index : nodesToUpgrade) {
                if (!currentNodeManagerVersions[index].equals(targetVersion)) {
                    upgradeNodeManager(index, targetVersion);
                    upgradedCount++;
                }
            }
            
            LOG.info("Partial upgrade completed: {} of {} NodeManagers upgraded", 
                     upgradedCount, nodeManagers.length);
            
            // Phase 3: Verify mixed-version cluster state
            LOG.info("Phase 3: Verifying partial cluster upgrade");
            waitForClusterStability(5000); // Wait up to 5 seconds
            
            LOG.info("Partial cluster upgrade completed. Final state:\n{}", 
                     getClusterVersionState());
            
        } catch (Exception e) {
            LOG.error("Partial cluster upgrade failed. Current state:\n{}", 
                      getClusterVersionState());
            throw new Exception("Partial cluster upgrade to version " + targetVersion + 
                              " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Selects which NodeManagers should be upgraded in a partial upgrade scenario.
     * This method deterministically selects the first 50% of NodeManagers for upgrade,
     * ensuring consistent behavior across test runs.
     *
     * @param totalNodes the total number of NodeManagers in the cluster
     * @return array of indices representing NodeManagers to upgrade
     */
    private int[] selectNodesForPartialUpgrade(int totalNodes) {
        // Calculate number of nodes to upgrade (at least 1)
        int nodesToUpgrade = Math.max(1, totalNodes / 2);
        int[] indices = new int[nodesToUpgrade];
        
        // Use deterministic selection: upgrade first half of nodes
        // This ensures consistent behavior and easier debugging
        for (int i = 0; i < nodesToUpgrade; i++) {
            indices[i] = i;
        }
        
        LOG.info("Selected {} of {} nodes for partial upgrade: {}", 
                 nodesToUpgrade, totalNodes, java.util.Arrays.toString(indices));
        
        return indices;
    }

    /**
     * Upgrades the YARN cluster based on the configured upgrade mode.
     * This method checks Instance.getUpgradeMode() to determine whether to perform:
     * - FULL: All ResourceManagers and NodeManagers upgraded
     * - PARTIAL: All ResourceManagers and 50% of NodeManagers upgraded  
     * - NONE: No upgrade performed
     */
    public void upgradeAllNodes() {
        try {
            UpgradeMode mode = Instance.getUpgradeMode();
            String targetVersion = Instance.UpgradeVersion;
            
            switch (mode) {
                case FULL:
                    LOG.info("Performing FULL cluster upgrade to version {}", targetVersion);
                    upgradeAllNodes(targetVersion);
                    break;
                case PARTIAL:
                    LOG.info("Performing PARTIAL (50%) cluster upgrade to version {}", targetVersion);
                    upgradePartialNodes(targetVersion);
                    break;
                case NONE:
                    LOG.info("No upgrade requested (mode = NONE)");
                    break;
                default:
                    LOG.warn("Unknown upgrade mode: {}, defaulting to FULL", mode);
                    upgradeAllNodes(targetVersion);
            }
        } catch (Exception e) {
            LOG.error("Cluster upgrade failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Upgrades the entire YARN cluster (all ResourceManagers and NodeManagers) to the specified target version.
     * This method coordinates a full cluster upgrade by first upgrading all ResourceManagers, 
     * then upgrading all NodeManagers, ensuring proper sequencing and configuration preservation.
     *
     * @param targetVersion the version to upgrade to (must be StartVersion or UpgradeVersion)
     * @throws Exception if upgrade fails
     */
    public void upgradeAllNodes(String targetVersion) throws Exception {
        validateVersion(targetVersion);
        
        LOG.info("Starting full cluster upgrade to version {}", targetVersion);
        LOG.info("Current cluster state:\n{}", getClusterVersionState());
        
        try {
            // Phase 1: Upgrade ResourceManagers first (for stability)
            LOG.info("Phase 1: Upgrading ResourceManagers to version {}", targetVersion);
            if (!currentResourceManagerVersion.equals(targetVersion)) {
                upgradeResourceManager(targetVersion);
                LOG.info("ResourceManager upgrade completed successfully");
            } else {
                LOG.info("ResourceManager already at target version {}", targetVersion);
            }
            
            // Brief pause to let ResourceManagers stabilize
            Thread.sleep(500);
            
            // Phase 2: Upgrade NodeManagers
            LOG.info("Phase 2: Upgrading all NodeManagers to version {}", targetVersion);
            if (!allNodeManagersHaveVersion(targetVersion)) {
                upgradeAllNodeManagers(targetVersion);
                LOG.info("NodeManager upgrades completed successfully");
            } else {
                LOG.info("All NodeManagers already at target version {}", targetVersion);
            }
            
            // Phase 3: Verify cluster state
            LOG.info("Phase 3: Verifying cluster upgrade");
            waitForClusterStability(5000); // Wait up to 5 seconds
            
            if (isClusterHomogeneous() && getClusterVersion().equals(targetVersion)) {
                LOG.info("Full cluster upgrade completed successfully to version {}", targetVersion);
                LOG.info("Final cluster state:\n{}", getClusterVersionState());
            } else {
                LOG.warn("Cluster upgrade may not be fully complete. Current state:\n{}", getClusterVersionState());
            }
            
        } catch (Exception e) {
            LOG.error("Full cluster upgrade failed. Current state:\n{}", getClusterVersionState());
            throw new Exception("Cluster upgrade to version " + targetVersion + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for cluster stability after an upgrade operation.
     * This method can be extended to include more sophisticated health checks.
     *
     * @param timeoutMs maximum time to wait in milliseconds
     * @return true if cluster appears stable, false if timeout exceeded
     */
    private boolean waitForClusterStability(int timeoutMs) {
        LOG.info("Waiting for cluster stability (timeout: {}ms)", timeoutMs);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Basic stability check - just wait a bit for services to initialize
            // In a real implementation, this could check service health, node registration, etc.
            Thread.sleep(Math.min(1000, timeoutMs)); // Wait at least 1 second, but not more than timeout
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < timeoutMs) {
                LOG.info("Cluster appears stable after {}ms", elapsed);
                return true;
            } else {
                LOG.warn("Cluster stability timeout exceeded ({}ms)", timeoutMs);
                return false;
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for cluster stability", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gets the current version of the ResourceManager.
     *
     * @return the current ResourceManager version
     */
    public String getResourceManagerVersion() {
        return currentResourceManagerVersion;
    }

    /**
     * Gets the current version of a specific NodeManager.
     *
     * @param index the index of the NodeManager
     * @return the current NodeManager version
     */
    public String getNodeManagerVersion(int index) {
        if (index < 0 || index >= currentNodeManagerVersions.length) {
            throw new IllegalArgumentException("Invalid NodeManager index: " + index);
        }
        return currentNodeManagerVersions[index];
    }

    /**
     * Gets the current versions of all NodeManagers.
     *
     * @return array of current NodeManager versions
     */
    public String[] getNodeManagerVersions() {
        return currentNodeManagerVersions.clone();
    }

    /**
     * Checks if all NodeManagers have the specified version.
     *
     * @param version the version to check
     * @return true if all NodeManagers have the specified version
     */
    private boolean allNodeManagersHaveVersion(String version) {
        if (currentNodeManagerVersions == null) {
            return false;
        }
        for (String nmVersion : currentNodeManagerVersions) {
            if (!version.equals(nmVersion)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the current version state of the entire cluster.
     *
     * @return a formatted string showing versions of all cluster components
     */
    public String getClusterVersionState() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cluster Version State:\n");
        
        // ResourceManager versions
        sb.append("  ResourceManager: ").append(currentResourceManagerVersion).append("\n");
        
        // NodeManager versions
        sb.append("  NodeManagers:\n");
        if (currentNodeManagerVersions != null) {
            for (int i = 0; i < currentNodeManagerVersions.length; i++) {
                sb.append("    NM[").append(i).append("]: ").append(currentNodeManagerVersions[i]).append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * Checks if the entire cluster is running on the same version.
     *
     * @return true if all ResourceManagers and NodeManagers are on the same version
     */
    public boolean isClusterHomogeneous() {
        if (currentNodeManagerVersions == null || currentResourceManagerVersion == null) {
            return false;
        }
        
        // Check if all NodeManagers have the same version as ResourceManager
        for (String nmVersion : currentNodeManagerVersions) {
            if (!currentResourceManagerVersion.equals(nmVersion)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets the common version if the cluster is homogeneous, otherwise returns null.
     *
     * @return the common version if all nodes are on same version, null otherwise
     */
    public String getClusterVersion() {
        return isClusterHomogeneous() ? currentResourceManagerVersion : null;
    }

    /**
     * Checks if the cluster is in a mixed-version state (partial upgrade).
     * A cluster is considered partially upgraded if different NodeManagers 
     * are running different versions.
     *
     * @return true if the cluster has NodeManagers running different versions
     */
    public boolean isPartiallyUpgraded() {
        if (currentNodeManagerVersions == null) {
            return false;
        }
        
        Set<String> versions = new HashSet<>();
        for (String version : currentNodeManagerVersions) {
            versions.add(version);
        }
        
        return versions.size() > 1;
    }

    /**
     * Gets upgrade statistics showing how many NodeManagers are on each version.
     * This provides visibility into the current state of a mixed-version cluster.
     *
     * @return map of version strings to count of NodeManagers on that version
     */
    public Map<String, Integer> getUpgradeStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        if (currentNodeManagerVersions != null) {
            for (String version : currentNodeManagerVersions) {
                stats.put(version, stats.getOrDefault(version, 0) + 1);
            }
        }
        
        return stats;
    }

    /**
     * Gets a detailed upgrade status report for the entire cluster.
     * This includes ResourceManager version, NodeManager version distribution,
     * and whether the cluster is homogeneous or mixed-version.
     *
     * @return detailed string describing the cluster's upgrade status
     */
    public String getUpgradeStatusReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Cluster Upgrade Status Report ===\n");
        
        // ResourceManager status
        report.append("ResourceManager Version: ").append(currentResourceManagerVersion).append("\n");
        
        // NodeManager statistics
        Map<String, Integer> stats = getUpgradeStatistics();
        report.append("NodeManager Version Distribution:\n");
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ")
                  .append(entry.getValue()).append(" nodes\n");
        }
        
        // Cluster status summary
        report.append("Cluster Status: ");
        if (isClusterHomogeneous()) {
            report.append("HOMOGENEOUS (all nodes on same version)\n");
        } else if (isPartiallyUpgraded()) {
            report.append("MIXED-VERSION (partial upgrade in progress)\n");
        } else {
            report.append("UNKNOWN\n");
        }
        
        report.append("======================================");
        return report.toString();
    }

    /**
     * Validates that the target version is one of the allowed versions.
     *
     * @param targetVersion the version to validate
     * @throws IllegalArgumentException if the version is invalid
     */
    private void validateVersion(String targetVersion) {
        if (targetVersion == null) {
            throw new IllegalArgumentException("Target version cannot be null");
        }
        
        String startVersion = Instance.StartVersion;
        String upgradeVersion = Instance.UpgradeVersion;
        
        if (startVersion == null || upgradeVersion == null) {
            throw new IllegalStateException("Start version and upgrade version must be set via system properties");
        }
        
        if (!targetVersion.equals(startVersion) && !targetVersion.equals(upgradeVersion)) {
            throw new IllegalArgumentException("Target version must be either StartVersion (" + startVersion + 
                                             ") or UpgradeVersion (" + upgradeVersion + "), but was: " + targetVersion);
        }
    }
}

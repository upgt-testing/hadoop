package org.apache.hadoop.yarn.server.nodemanager;

public interface NMContextJVMInterface extends ContextJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandler();

    org.apache.hadoop.yarn.server.nodemanager.logaggregation.tracker.NMLogAggregationStatusTrackerJVMInterface getNMLogAggregationStatusTracker();

    java.lang.Object getContainerStateTransitionListener();

    java.util.concurrent.ConcurrentMap getKnownCollectors();

    java.util.concurrent.ConcurrentMap getIncreasedContainers();

    void setContainerExecutor_bridge(org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.Object getNodeResourceMonitor();

    java.util.concurrent.ConcurrentMap getContainers();

    boolean isDistributedSchedulingEnabled();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getNMStateStore();

    void setResourcePluginManager_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.ResourcePluginManagerJVMInterface arg0);

    int getHttpPort();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    org.apache.hadoop.yarn.server.nodemanager.DeletionServiceJVMInterface getDeletionService();

    void setNodeManagerMetrics_bridge(org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetricsJVMInterface arg0);

    void setContainerStateTransitionListener_bridge(java.lang.Object arg0);

    org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocatorJVMInterface getContainerAllocator();

    void setDecommissioned(boolean arg0);

    java.util.concurrent.ConcurrentMap getApplications();

    void setNMTimelinePublisher_bridge(org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface arg0);

    void setSystemCrendentialsForApps(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.security.Credentials> arg0);

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    void setQueueableContainerAllocator_bridge(org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocatorJVMInterface arg0);

    void setNodeResourceMonitor_bridge(java.lang.Object arg0);

    void setDeletionService_bridge(org.apache.hadoop.yarn.server.nodemanager.DeletionServiceJVMInterface arg0);

    void setWebServer_bridge(org.apache.hadoop.yarn.server.nodemanager.webapp.WebServerJVMInterface arg0);

    org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManagerJVMInterface getContainerTokenSecretManager();

    org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNMJVMInterface getNMTokenSecretManager();

    java.util.Map getSystemCredentialsForApps();

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();

    void setNodeStatusUpdater_bridge(java.lang.Object arg0);

    boolean getDecommissioned();

    java.util.concurrent.ConcurrentMap getRegisteringCollectors();

    org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface getNMTimelinePublisher();

    org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetricsJVMInterface getNodeManagerMetrics();

    void setContainerManager_bridge(java.lang.Object arg0);

    void setNMLogAggregationStatusTracker_bridge(org.apache.hadoop.yarn.server.nodemanager.logaggregation.tracker.NMLogAggregationStatusTrackerJVMInterface arg0);

    java.lang.Object getNodeStatusUpdater();

    org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface getContainerExecutor();

    java.lang.Object getContainerManager();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    java.util.concurrent.ConcurrentLinkedQueue getLogAggregationStatusForApps();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.ResourcePluginManagerJVMInterface getResourcePluginManager();
}

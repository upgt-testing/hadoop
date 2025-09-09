package org.apache.hadoop.yarn.server.nodemanager;

public interface ContextJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandler();

    org.apache.hadoop.yarn.server.nodemanager.logaggregation.tracker.NMLogAggregationStatusTrackerJVMInterface getNMLogAggregationStatusTracker();

    java.util.concurrent.ConcurrentMap getKnownCollectors();

    java.lang.Object getContainerStateTransitionListener();

    java.util.concurrent.ConcurrentMap getIncreasedContainers();

    java.lang.Object getNodeResourceMonitor();

    java.util.concurrent.ConcurrentMap getContainers();

    boolean isDistributedSchedulingEnabled();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getNMStateStore();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    int getHttpPort();

    org.apache.hadoop.yarn.server.nodemanager.DeletionServiceJVMInterface getDeletionService();

    org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocatorJVMInterface getContainerAllocator();

    void setDecommissioned(boolean arg0);

    java.util.concurrent.ConcurrentMap getApplications();

    void setNMTimelinePublisher_bridge(org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface arg0);

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.AuxServicesJVMInterface getAuxServices();

    org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManagerJVMInterface getContainerTokenSecretManager();

    org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNMJVMInterface getNMTokenSecretManager();

    java.util.Map getSystemCredentialsForApps();

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();

    boolean getDecommissioned();

    java.util.concurrent.ConcurrentMap getRegisteringCollectors();

    void setAuxServices_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.AuxServicesJVMInterface arg0);

    org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface getNMTimelinePublisher();

    org.apache.hadoop.yarn.server.nodemanager.metrics.NodeManagerMetricsJVMInterface getNodeManagerMetrics();

    java.lang.Object getNodeStatusUpdater();

    java.lang.Object getContainerManager();

    org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface getContainerExecutor();

    java.util.concurrent.ConcurrentLinkedQueue getLogAggregationStatusForApps();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.ResourcePluginManagerJVMInterface getResourcePluginManager();
}

package org.apache.hadoop.yarn.server.nodemanager;

public interface NMContextJVMInterface extends ContextJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandler();

    java.lang.Object getContainerStateTransitionListener();

    java.util.concurrent.ConcurrentMap getKnownCollectors();

    java.util.concurrent.ConcurrentMap getIncreasedContainers();

    void setContainerExecutor_bridge(org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.Object getNodeResourceMonitor();

    java.util.concurrent.ConcurrentMap getContainers();

    boolean isDistributedSchedulingEnabled();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getNMStateStore();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    int getHttpPort();

    void setContainerStateTransitionListener_bridge(java.lang.Object arg0);

    org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocatorJVMInterface getContainerAllocator();

    void setDecommissioned(boolean arg0);

    java.util.concurrent.ConcurrentMap getApplications();

    void setNMTimelinePublisher_bridge(org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface arg0);

    void setQueueableContainerAllocator_bridge(org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocatorJVMInterface arg0);

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    void setSystemCrendentialsForApps(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.security.Credentials> arg0);

    void setNodeResourceMonitor_bridge(java.lang.Object arg0);

    void setWebServer_bridge(org.apache.hadoop.yarn.server.nodemanager.webapp.WebServerJVMInterface arg0);

    org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManagerJVMInterface getContainerTokenSecretManager();

    org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNMJVMInterface getNMTokenSecretManager();

    java.util.Map getSystemCredentialsForApps();

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();

    void setNodeStatusUpdater_bridge(java.lang.Object arg0);

    boolean getDecommissioned();

    java.util.concurrent.ConcurrentMap getRegisteringCollectors();

    org.apache.hadoop.yarn.server.nodemanager.timelineservice.NMTimelinePublisherJVMInterface getNMTimelinePublisher();

    java.lang.Object getNodeStatusUpdater();

    void setContainerManager_bridge(java.lang.Object arg0);

    java.lang.Object getContainerManager();

    org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface getContainerExecutor();

    java.util.concurrent.ConcurrentLinkedQueue getLogAggregationStatusForApps();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();
}

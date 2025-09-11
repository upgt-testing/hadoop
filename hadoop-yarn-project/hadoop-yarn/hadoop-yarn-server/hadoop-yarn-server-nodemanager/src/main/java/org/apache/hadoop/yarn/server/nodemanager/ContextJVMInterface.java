package org.apache.hadoop.yarn.server.nodemanager;

public interface ContextJVMInterface {

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandler();

    java.util.concurrent.ConcurrentMap getIncreasedContainers();

    org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManagerJVMInterface getContainerTokenSecretManager();

    org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNMJVMInterface getNMTokenSecretManager();

    java.util.Map getSystemCredentialsForApps();

    java.lang.Object getNodeResourceMonitor();

    java.util.concurrent.ConcurrentMap getContainers();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getNMStateStore();

    boolean getDecommissioned();

    int getHttpPort();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    java.lang.Object getNodeStatusUpdater();

    void setDecommissioned(boolean arg0);

    java.lang.Object getContainerManager();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    java.util.concurrent.ConcurrentLinkedQueue getLogAggregationStatusForApps();

    java.util.concurrent.ConcurrentMap getApplications();
}

package org.apache.hadoop.yarn.server.nodemanager;

public interface NMContextJVMInterface extends ContextJVMInterface {

    void setSystemCrendentialsForApps(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.security.Credentials> arg0);

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    void setNodeResourceMonitor_bridge(java.lang.Object arg0);

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandler();

    void setWebServer_bridge(org.apache.hadoop.yarn.server.nodemanager.webapp.WebServerJVMInterface arg0);

    java.util.concurrent.ConcurrentMap getIncreasedContainers();

    org.apache.hadoop.yarn.server.nodemanager.security.NMContainerTokenSecretManagerJVMInterface getContainerTokenSecretManager();

    org.apache.hadoop.yarn.server.nodemanager.security.NMTokenSecretManagerInNMJVMInterface getNMTokenSecretManager();

    java.util.Map getSystemCredentialsForApps();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.Object getNodeResourceMonitor();

    java.util.concurrent.ConcurrentMap getContainers();

    void setNodeStatusUpdater_bridge(java.lang.Object arg0);

    boolean getDecommissioned();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getNMStateStore();

    int getHttpPort();

    org.apache.hadoop.yarn.server.security.ApplicationACLsManagerJVMInterface getApplicationACLsManager();

    java.lang.Object getNodeStatusUpdater();

    void setContainerManager_bridge(java.lang.Object arg0);

    void setDecommissioned(boolean arg0);

    java.lang.Object getContainerManager();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    java.util.concurrent.ConcurrentLinkedQueue getLogAggregationStatusForApps();

    java.util.concurrent.ConcurrentMap getApplications();
}

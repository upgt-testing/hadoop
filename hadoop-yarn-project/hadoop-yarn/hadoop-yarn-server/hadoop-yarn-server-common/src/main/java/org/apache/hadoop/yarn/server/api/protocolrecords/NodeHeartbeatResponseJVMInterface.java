package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface NodeHeartbeatResponseJVMInterface {

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.util.List getContainersToSignalList();

    void setContainerTokenMasterKey_bridge(java.lang.Object arg0);

    boolean getAreNodeLabelsAcceptedByRM();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    long getNextHeartBeatInterval();

    java.lang.Object getContainerTokenMasterKey();

    void setAppCollectors(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.yarn.server.api.records.AppCollectorData> arg0);

    java.util.Map getAppCollectors();

    void addAllContainersToDecrease(java.util.Collection<org.apache.hadoop.yarn.api.records.Container> arg0);

    void addContainersToBeRemovedFromNM(java.util.List<org.apache.hadoop.yarn.api.records.ContainerId> arg0);

    java.lang.Object getNodeAction();

    void setResponseId(int arg0);

    org.apache.hadoop.yarn.server.api.records.ContainerQueuingLimitJVMInterface getContainerQueuingLimit();

    boolean getAreNodeAttributesAcceptedByRM();

    void addAllContainersToCleanup(java.util.List<org.apache.hadoop.yarn.api.records.ContainerId> arg0);

    java.lang.Object getNMTokenMasterKey();

    java.util.List getContainersToBeRemovedFromNM();

    void addAllContainersToUpdate(java.util.Collection<org.apache.hadoop.yarn.api.records.Container> arg0);

    void addAllApplicationsToCleanup(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationId> arg0);

    void setAreNodeAttributesAcceptedByRM(boolean arg0);

    void setDiagnosticsMessage(java.lang.String arg0);

    void setContainerQueuingLimit_bridge(org.apache.hadoop.yarn.server.api.records.ContainerQueuingLimitJVMInterface arg0);

    java.util.List getContainersToCleanup();

    java.lang.String getDiagnosticsMessage();

    java.util.List getContainersToUpdate();

    void addAllContainersToSignal(java.util.List<org.apache.hadoop.yarn.api.protocolrecords.SignalContainerRequest> arg0);

    int getResponseId();

    java.util.List getContainersToDecrease();

    void setNMTokenMasterKey_bridge(java.lang.Object arg0);

    void setNodeAction_bridge(java.lang.Object arg0);

    java.util.Map getSystemCredentialsForApps();

    java.util.List getApplicationsToCleanup();

    void setSystemCredentialsForApps(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, java.nio.ByteBuffer> arg0);

    void setNextHeartBeatInterval(long arg0);

    void setAreNodeLabelsAcceptedByRM(boolean arg0);
}

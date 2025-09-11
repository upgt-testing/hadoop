package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface NodeHeartbeatResponseJVMInterface {

    java.util.List getContainersToSignalList();
    

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


    void addAllContainersToCleanup(java.util.List<org.apache.hadoop.yarn.api.records.ContainerId> arg0);

    java.util.List getContainersToBeRemovedFromNM();

    java.lang.Object getNMTokenMasterKey();

    void addAllApplicationsToCleanup(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationId> arg0);

    void addAllContainersToUpdate(java.util.Collection<org.apache.hadoop.yarn.api.records.Container> arg0);

    void setDiagnosticsMessage(java.lang.String arg0);


    java.util.List getContainersToCleanup();

    java.lang.String getDiagnosticsMessage();

    java.util.List getContainersToUpdate();

    void addAllContainersToSignal(java.util.List<org.apache.hadoop.yarn.api.protocolrecords.SignalContainerRequest> arg0);

    int getResponseId();


    java.util.List getContainersToDecrease();


    java.util.Map getSystemCredentialsForApps();

    java.util.List getApplicationsToCleanup();


    void setNextHeartBeatInterval(long arg0);

    void setAreNodeLabelsAcceptedByRM(boolean arg0);
}

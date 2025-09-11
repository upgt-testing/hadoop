package org.apache.hadoop.yarn.api.protocolrecords;

public interface AllocateResponseJVMInterface {

    void setCollectorInfo_bridge(org.apache.hadoop.yarn.api.records.CollectorInfoJVMInterface arg0);

    java.util.List getAllocatedContainers();

    org.apache.hadoop.yarn.api.records.PreemptionMessageJVMInterface getPreemptionMessage();

    void setUpdatedContainers(java.util.List<org.apache.hadoop.yarn.api.records.UpdatedContainer> arg0);

    void setApplicationPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    java.util.List getUpdatedNodes();

    void setAllocatedContainers(java.util.List<org.apache.hadoop.yarn.api.records.Container> arg0);

    void setNumClusterNodes(int arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getAvailableResources();

    java.util.List getNMTokens();

    void setResponseId(int arg0);

    void setAvailableResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setUpdateErrors(java.util.List<org.apache.hadoop.yarn.api.records.UpdateContainerError> arg0);

    void setCompletedContainersStatuses(java.util.List<org.apache.hadoop.yarn.api.records.ContainerStatus> arg0);

    java.util.List getContainersFromPreviousAttempts();

    org.apache.hadoop.yarn.api.records.CollectorInfoJVMInterface getCollectorInfo();

    java.util.List getRejectedSchedulingRequests();

    int getNumClusterNodes();

    void setContainersFromPreviousAttempts(java.util.List<org.apache.hadoop.yarn.api.records.Container> arg0);

    void setNMTokens(java.util.List<org.apache.hadoop.yarn.api.records.NMToken> arg0);

    java.lang.Object getAMCommand();

    int getResponseId();

    java.util.List getCompletedContainersStatuses();

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getAMRMToken();

    void setUpdatedNodes(java.util.List<org.apache.hadoop.yarn.api.records.NodeReport> arg0);

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getApplicationPriority();

    void setAMCommand_bridge(java.lang.Object arg0);

    void setPreemptionMessage_bridge(org.apache.hadoop.yarn.api.records.PreemptionMessageJVMInterface arg0);

    java.util.List getUpdateErrors();

    void setRejectedSchedulingRequests(java.util.List<org.apache.hadoop.yarn.api.records.RejectedSchedulingRequest> arg0);

    java.util.List getUpdatedContainers();

    void setAMRMToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);
}

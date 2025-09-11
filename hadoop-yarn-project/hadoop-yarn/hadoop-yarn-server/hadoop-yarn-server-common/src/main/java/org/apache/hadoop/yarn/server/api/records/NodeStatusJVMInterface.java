package org.apache.hadoop.yarn.server.api.records;

public interface NodeStatusJVMInterface {

    org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface getNodeHealthStatus();

    void setContainersUtilization_bridge(org.apache.hadoop.yarn.api.records.ResourceUtilizationJVMInterface arg0);

    org.apache.hadoop.yarn.server.api.records.OpportunisticContainersStatusJVMInterface getOpportunisticContainersStatus();

    void setContainersStatuses(java.util.List<org.apache.hadoop.yarn.api.records.ContainerStatus> arg0);

    int getResponseId();

    java.util.List getIncreasedContainers();

    void setNodeHealthStatus_bridge(org.apache.hadoop.yarn.server.api.records.NodeHealthStatusJVMInterface arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    void setNodeUtilization_bridge(org.apache.hadoop.yarn.api.records.ResourceUtilizationJVMInterface arg0);

    void setResponseId(int arg0);

    void setKeepAliveApplications(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationId> arg0);

    java.util.List getContainersStatuses();

    void setOpportunisticContainersStatus_bridge(org.apache.hadoop.yarn.server.api.records.OpportunisticContainersStatusJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceUtilizationJVMInterface getContainersUtilization();

    void setIncreasedContainers(java.util.List<org.apache.hadoop.yarn.api.records.Container> arg0);

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    org.apache.hadoop.yarn.api.records.ResourceUtilizationJVMInterface getNodeUtilization();

    java.util.List getKeepAliveApplications();
}

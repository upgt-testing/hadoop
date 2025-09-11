package org.apache.hadoop.yarn.api.records;

public interface ContainerJVMInterface {

    void setAllocationRequestId(long arg0);

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    long getAllocationRequestId();

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setNodeHttpAddress(java.lang.String arg0);

    void setAllocationTags(java.util.Set<java.lang.String> arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    void setContainerToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    int getVersion();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.String getNodeHttpAddress();

    java.util.Set getAllocationTags();

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getContainerToken();

    void setId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getId();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    void setVersion(int arg0);

    java.lang.Object getExecutionType();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setExecutionType_bridge(java.lang.Object arg0);
}

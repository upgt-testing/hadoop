package org.apache.hadoop.yarn.api.records;

public interface ContainerJVMInterface {

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    long getAllocationRequestId();

    void setNodeHttpAddress(java.lang.String arg0);

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setAllocationRequestId(long arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    void setContainerToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    int getVersion();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.String getNodeHttpAddress();

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getContainerToken();

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getId();

    void setId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setVersion(int arg0);

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    java.lang.Object getExecutionType();

    void setExecutionType_bridge(java.lang.Object arg0);
}

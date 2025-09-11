package org.apache.hadoop.yarn.api.records;

public interface ContainerReportJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setNodeHttpAddress(java.lang.String arg0);

    long getFinishTime();

    void setLogUrl(java.lang.String arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();

    java.lang.String getLogUrl();

    void setDiagnosticsInfo(java.lang.String arg0);

    void setContainerExitStatus(int arg0);

    java.lang.String getNodeHttpAddress();

    long getCreationTime();

    java.lang.Object getContainerState();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getAssignedNode();

    void setContainerState_bridge(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getAllocatedResource();

    int getContainerExitStatus();

    void setContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setAllocatedResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setAssignedNode_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.String getDiagnosticsInfo();

    void setCreationTime(long arg0);

    java.lang.Object getExecutionType();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setExecutionType_bridge(java.lang.Object arg0);

    void setFinishTime(long arg0);
}

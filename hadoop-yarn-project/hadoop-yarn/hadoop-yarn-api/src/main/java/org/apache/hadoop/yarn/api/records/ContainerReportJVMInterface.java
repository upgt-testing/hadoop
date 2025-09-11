package org.apache.hadoop.yarn.api.records;

public interface ContainerReportJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    long getFinishTime();

    void setLogUrl(java.lang.String arg0);

    java.lang.String getLogUrl();

    void setContainerExitStatus(int arg0);

    java.lang.Object getContainerState();

    java.lang.String getNodeHttpAddress();

    void setContainerState_bridge(java.lang.Object arg0);

    int getContainerExitStatus();

    void setExposedPorts(java.util.Map<java.lang.String, java.util.List<java.util.Map<java.lang.String, java.lang.String>>> arg0);

    void setAllocatedResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setAssignedNode_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.lang.String getDiagnosticsInfo();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    java.lang.Object getExecutionType();

    void setExecutionType_bridge(java.lang.Object arg0);

    void setNodeHttpAddress(java.lang.String arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();

    void setDiagnosticsInfo(java.lang.String arg0);

    long getCreationTime();

    java.lang.String getExposedPorts();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getAssignedNode();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getAllocatedResource();

    void setContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setCreationTime(long arg0);

    void setFinishTime(long arg0);
}

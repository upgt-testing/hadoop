package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface RegisterNodeManagerRequestJVMInterface {

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.util.List getRunningApplications();

    void setHttpPort(int arg0);

    void setNodeStatus_bridge(org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface arg0);

    void setNodeAttributes(java.util.Set<org.apache.hadoop.yarn.api.records.NodeAttribute> arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    void setContainerStatuses(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.NMContainerStatus> arg0);

    java.util.List getNMContainerStatuses();

    void setRunningApplications(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationId> arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    java.util.List getLogAggregationReportsForApps();

    void setNodeLabels(java.util.Set<org.apache.hadoop.yarn.api.records.NodeLabel> arg0);

    void setLogAggregationReportsForApps(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport> arg0);

    org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface getNodeStatus();

    java.util.Set getNodeLabels();

    java.lang.String getNMVersion();

    int getHttpPort();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getPhysicalResource();

    java.util.Set getNodeAttributes();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    void setNMVersion(java.lang.String arg0);

    void setPhysicalResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);
}

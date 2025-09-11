package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface RegisterNodeManagerRequestJVMInterface {

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setHttpPort(int arg0);

    java.util.List getRunningApplications();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    void setContainerStatuses(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.NMContainerStatus> arg0);

    java.util.List getNMContainerStatuses();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    void setRunningApplications(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationId> arg0);

    void setNodeLabels(java.util.Set<org.apache.hadoop.yarn.api.records.NodeLabel> arg0);

    java.util.Set getNodeLabels();

    java.lang.String getNMVersion();

    int getHttpPort();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getPhysicalResource();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    void setNMVersion(java.lang.String arg0);

    void setPhysicalResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);
}

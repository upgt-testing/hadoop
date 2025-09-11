package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface NodeHeartbeatRequestJVMInterface {

    void setNodeAttributes(java.util.Set<org.apache.hadoop.yarn.api.records.NodeAttribute> arg0);

    void setNodeStatus_bridge(org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface arg0);

    void setLastKnownContainerTokenMasterKey_bridge(java.lang.Object arg0);

    java.lang.Object getLastKnownContainerTokenMasterKey();

    java.util.List getLogAggregationReportsForApps();

    void setNodeLabels(java.util.Set<org.apache.hadoop.yarn.api.records.NodeLabel> arg0);

    java.lang.Object getLastKnownNMTokenMasterKey();

    org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface getNodeStatus();

    void setLogAggregationReportsForApps(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport> arg0);

    java.util.Set getNodeLabels();

    java.util.Map getRegisteringCollectors();

    java.util.Set getNodeAttributes();

    void setLastKnownNMTokenMasterKey_bridge(java.lang.Object arg0);

    void setRegisteringCollectors(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.yarn.server.api.records.AppCollectorData> arg0);
}

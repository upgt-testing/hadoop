package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface NodeHeartbeatRequestJVMInterface {

    java.lang.Object getLastKnownNMTokenMasterKey();

    void setLogAggregationReportsForApps(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.LogAggregationReport> arg0);

    org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface getNodeStatus();

    java.util.Set getNodeLabels();

    java.util.Map getRegisteringCollectors();

    void setNodeStatus_bridge(org.apache.hadoop.yarn.server.api.records.NodeStatusJVMInterface arg0);

    void setLastKnownContainerTokenMasterKey_bridge(java.lang.Object arg0);

    java.util.List getLogAggregationReportsForApps();

    java.lang.Object getLastKnownContainerTokenMasterKey();

    void setLastKnownNMTokenMasterKey_bridge(java.lang.Object arg0);

    void setRegisteringCollectors(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationId, org.apache.hadoop.yarn.server.api.records.AppCollectorData> arg0);

    void setNodeLabels(java.util.Set<org.apache.hadoop.yarn.api.records.NodeLabel> arg0);
}

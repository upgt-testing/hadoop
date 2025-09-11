package org.apache.hadoop.yarn.server.scheduler;

public interface OpportunisticContainerContextJVMInterface {

    java.lang.Object getAppParams();

    void addToOutstandingReqs(java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg0);

    java.util.Map getNodeMap();

    void updateAllocationParams_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0, org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg1, org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg2, int arg3);

    java.util.Set getBlacklist();

    void setContainerIdGenerator_bridge(java.lang.Object arg0);

    void matchAllocationToOutstandingRequest_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0, java.util.List<org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerAllocator.Allocation> arg1);

    org.apache.hadoop.yarn.server.metrics.OpportunisticSchedulerMetricsJVMInterface getOppSchedulerMetrics();

    void updateNodeList(java.util.List<org.apache.hadoop.yarn.server.api.protocolrecords.RemoteNode> arg0);

    java.lang.Object getContainerIdGenerator();

    java.util.TreeMap getOutstandingOpReqs();
}

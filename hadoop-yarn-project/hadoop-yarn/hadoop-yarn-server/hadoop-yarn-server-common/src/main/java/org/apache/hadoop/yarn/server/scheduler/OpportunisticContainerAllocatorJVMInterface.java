package org.apache.hadoop.yarn.server.scheduler;

public interface OpportunisticContainerAllocatorJVMInterface {

    java.lang.Object partitionAskList(java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg0);

    java.util.List allocateContainers_bridge(org.apache.hadoop.yarn.api.records.ResourceBlacklistRequestJVMInterface arg0, java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg1, org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg2, org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerContextJVMInterface arg3, long arg4, java.lang.String arg5) throws org.apache.hadoop.yarn.exceptions.YarnException;
}

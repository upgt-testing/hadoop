package org.apache.hadoop.yarn.api.records;

public interface ApplicationSubmissionContextJVMInterface {

    java.util.List getAMContainerResourceRequests();

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    java.util.Map getApplicationTimeouts();

    void setAMContainerResourceRequest_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface getReservationID();

    void setMaxAppAttempts(int arg0);

    java.lang.String getApplicationType();

    void setUnmanagedAM(boolean arg0);

    void setApplicationTags(java.util.Set<java.lang.String> arg0);

    void setKeepContainersAcrossApplicationAttempts(boolean arg0);

    void setApplicationName(java.lang.String arg0);

    java.lang.String getQueue();

    void setAMContainerResourceRequests(java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg0);

    java.util.Set getApplicationTags();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setNodeLabelExpression(java.lang.String arg0);

    boolean getCancelTokensWhenComplete();

    void setApplicationType(java.lang.String arg0);

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setAMContainerSpec_bridge(org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface arg0);

    void setLogAggregationContext_bridge(org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface arg0);

    void setQueue(java.lang.String arg0);

    void setReservationID_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0);

    boolean getKeepContainersAcrossApplicationAttempts();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    long getAttemptFailuresValidityInterval();

    org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface getAMContainerSpec();

    java.lang.String getApplicationName();

    void setAttemptFailuresValidityInterval(long arg0);

    org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface getLogAggregationContext();

    org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface getAMContainerResourceRequest();

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, java.lang.Long> arg0);

    void setCancelTokensWhenComplete(boolean arg0);

    java.lang.String getNodeLabelExpression();

    boolean getUnmanagedAM();

    int getMaxAppAttempts();
}

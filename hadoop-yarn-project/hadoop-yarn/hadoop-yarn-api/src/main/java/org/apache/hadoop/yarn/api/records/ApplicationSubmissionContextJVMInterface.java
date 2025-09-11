package org.apache.hadoop.yarn.api.records;

public interface ApplicationSubmissionContextJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setAMContainerResourceRequest_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    void setMaxAppAttempts(int arg0);

    org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface getReservationID();

    java.lang.String getApplicationType();

    void setUnmanagedAM(boolean arg0);

    void setApplicationTags(java.util.Set<java.lang.String> arg0);

    void setApplicationName(java.lang.String arg0);

    void setKeepContainersAcrossApplicationAttempts(boolean arg0);

    java.lang.String getQueue();

    java.util.Set getApplicationTags();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setNodeLabelExpression(java.lang.String arg0);

    void setApplicationType(java.lang.String arg0);

    boolean getCancelTokensWhenComplete();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setAMContainerSpec_bridge(org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface arg0);

    void setLogAggregationContext_bridge(org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface arg0);

    void setQueue(java.lang.String arg0);

    void setReservationID_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0);

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    boolean getKeepContainersAcrossApplicationAttempts();

    long getAttemptFailuresValidityInterval();

    org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface getAMContainerSpec();

    java.lang.String getApplicationName();

    void setAttemptFailuresValidityInterval(long arg0);

    org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface getLogAggregationContext();

    org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface getAMContainerResourceRequest();

    void setCancelTokensWhenComplete(boolean arg0);

    java.lang.String getNodeLabelExpression();

    int getMaxAppAttempts();

    boolean getUnmanagedAM();
}

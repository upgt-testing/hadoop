package org.apache.hadoop.yarn.api.records;

public interface ApplicationReportJVMInterface {

    long getFinishTime();

    java.util.Map getApplicationTimeouts();

    java.lang.String getOriginalTrackingUrl();

    void setOriginalTrackingUrl(java.lang.String arg0);

    void setClientToAMToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    java.lang.String getApplicationType();

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, org.apache.hadoop.yarn.api.records.ApplicationTimeout> arg0);

    java.lang.String getAppNodeLabelExpression();

    void setUser(java.lang.String arg0);

    void setAppNodeLabelExpression(java.lang.String arg0);

    void setApplicationTags(java.util.Set<java.lang.String> arg0);

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getCurrentApplicationAttemptId();

    void setHost(java.lang.String arg0);

    java.lang.Object getFinalApplicationStatus();

    java.util.Set getApplicationTags();

    void setApplicationResourceUsageReport_bridge(org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReportJVMInterface arg0);

    long getLaunchTime();

    void setRpcPort(int arg0);

    long getStartTime();

    int getRpcPort();

    void setUnmanagedApp(boolean arg0);

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getClientToAMToken();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setAmNodeLabelExpression(java.lang.String arg0);

    java.lang.String getTrackingUrl();

    void setQueue(java.lang.String arg0);

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getAMRMToken();

    java.lang.Object getYarnApplicationState();

    void setCurrentApplicationAttemptId_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    java.lang.String getName();

    void setProgress(float arg0);

    void setLogAggregationStatus_bridge(java.lang.Object arg0);

    void setTrackingUrl(java.lang.String arg0);

    void setFinishTime(long arg0);

    long getSubmitTime();

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    boolean isUnmanagedApp();

    org.apache.hadoop.yarn.api.records.ApplicationResourceUsageReportJVMInterface getApplicationResourceUsageReport();

    void setSubmitTime(long arg0);

    java.lang.String getQueue();

    float getProgress();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setFinalApplicationStatus_bridge(java.lang.Object arg0);

    void setApplicationType(java.lang.String arg0);

    void setStartTime(long arg0);

    java.lang.String getAmNodeLabelExpression();

    java.lang.String getUser();

    java.lang.String getDiagnostics();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    void setLaunchTime(long arg0);

    java.lang.String getHost();

    void setYarnApplicationState_bridge(java.lang.Object arg0);

    java.lang.Object getLogAggregationStatus();

    void setDiagnostics(java.lang.String arg0);

    void setName(java.lang.String arg0);

    void setAMRMToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);
}

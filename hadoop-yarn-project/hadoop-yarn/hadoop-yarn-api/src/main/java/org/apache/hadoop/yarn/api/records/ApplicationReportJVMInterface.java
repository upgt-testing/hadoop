package org.apache.hadoop.yarn.api.records;

public interface ApplicationReportJVMInterface {

    long getFinishTime();

    java.util.Map getApplicationTimeouts();

    java.lang.String getOriginalTrackingUrl();

    void setOriginalTrackingUrl(java.lang.String arg0);

    java.lang.String getApplicationType();

    void setClientToAMToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    java.lang.String getAppNodeLabelExpression();

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, org.apache.hadoop.yarn.api.records.ApplicationTimeout> arg0);

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

    void setQueue(java.lang.String arg0);

    java.lang.String getTrackingUrl();

    java.lang.Object getYarnApplicationState();

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getAMRMToken();

    void setCurrentApplicationAttemptId_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    java.lang.String getName();

    void setProgress(float arg0);

    void setLogAggregationStatus_bridge(java.lang.Object arg0);

    void setFinishTime(long arg0);

    void setTrackingUrl(java.lang.String arg0);

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    long getSubmitTime();

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

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    java.lang.String getDiagnostics();

    void setLaunchTime(long arg0);

    void setYarnApplicationState_bridge(java.lang.Object arg0);

    java.lang.String getHost();

    java.lang.Object getLogAggregationStatus();

    void setDiagnostics(java.lang.String arg0);

    void setName(java.lang.String arg0);

    void setAMRMToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);
}

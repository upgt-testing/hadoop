package org.apache.hadoop.yarn.api.records;

public interface ApplicationAttemptReportJVMInterface {

    long getFinishTime();

    java.lang.String getOriginalTrackingUrl();

    void setOriginalTrackingUrl(java.lang.String arg0);

    java.lang.String getTrackingUrl();

    java.lang.String getDiagnostics();

    void setHost(java.lang.String arg0);

    void setYarnApplicationAttemptState_bridge(java.lang.Object arg0);

    java.lang.String getHost();

    java.lang.Object getYarnApplicationAttemptState();

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getAMContainerId();

    void setRpcPort(int arg0);

    long getStartTime();

    void setDiagnostics(java.lang.String arg0);

    int getRpcPort();

    void setApplicationAttemptId_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    void setStartTime(long arg0);

    void setAMContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setFinishTime(long arg0);

    void setTrackingUrl(java.lang.String arg0);

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();
}

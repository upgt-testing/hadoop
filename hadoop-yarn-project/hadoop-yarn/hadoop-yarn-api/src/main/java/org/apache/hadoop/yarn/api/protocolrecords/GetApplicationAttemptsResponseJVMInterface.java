package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetApplicationAttemptsResponseJVMInterface {

    java.util.List getApplicationAttemptList();

    void setApplicationAttemptList(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationAttemptReport> arg0);
}

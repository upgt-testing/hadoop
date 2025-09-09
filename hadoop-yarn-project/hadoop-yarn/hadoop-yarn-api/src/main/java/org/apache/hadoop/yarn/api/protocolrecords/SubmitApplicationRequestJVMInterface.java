package org.apache.hadoop.yarn.api.protocolrecords;

public interface SubmitApplicationRequestJVMInterface {

    void setApplicationSubmissionContext_bridge(org.apache.hadoop.yarn.api.records.ApplicationSubmissionContextJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ApplicationSubmissionContextJVMInterface getApplicationSubmissionContext();
}

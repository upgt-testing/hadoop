package org.apache.hadoop.yarn.api.protocolrecords;

public interface UpdateApplicationTimeoutsResponseJVMInterface {

    java.util.Map getApplicationTimeouts();

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, java.lang.String> arg0);
}

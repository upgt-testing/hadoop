package org.apache.hadoop.yarn.api.protocolrecords;

public interface UpdateApplicationTimeoutsRequestJVMInterface {

    java.util.Map getApplicationTimeouts();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, java.lang.String> arg0);
}

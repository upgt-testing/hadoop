package org.apache.hadoop.yarn.api.protocolrecords;

public interface UpdateApplicationTimeoutsRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    java.util.Map getApplicationTimeouts();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    void setApplicationTimeouts(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationTimeoutType, java.lang.String> arg0);
}

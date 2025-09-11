package org.apache.hadoop.yarn.api.protocolrecords;

public interface MoveApplicationAcrossQueuesRequestJVMInterface {

    java.lang.String getTargetQueue();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setTargetQueue(java.lang.String arg0);

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);
}

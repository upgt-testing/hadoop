package org.apache.hadoop.yarn.api.protocolrecords;

public interface UpdateApplicationPriorityRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setApplicationPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getApplicationPriority();
}

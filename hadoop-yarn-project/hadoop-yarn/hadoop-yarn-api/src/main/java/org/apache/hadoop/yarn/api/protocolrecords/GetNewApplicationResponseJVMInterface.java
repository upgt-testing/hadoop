package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetNewApplicationResponseJVMInterface {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getMaximumResourceCapability();

    void setMaximumResourceCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);
}

package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetNewApplicationResponseJVMInterface {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setMaximumResourceCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getMaximumResourceCapability();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);
}

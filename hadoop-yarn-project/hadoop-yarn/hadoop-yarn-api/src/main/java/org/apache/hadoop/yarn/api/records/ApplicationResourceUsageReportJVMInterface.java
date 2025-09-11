package org.apache.hadoop.yarn.api.records;

public interface ApplicationResourceUsageReportJVMInterface {

    float getQueueUsagePercentage();

    void setVcoreSeconds(long arg0);

    long getPreemptedVcoreSeconds();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getNeededResources();

    int getNumUsedContainers();

    float getClusterUsagePercentage();

    void setNeededResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    long getVcoreSeconds();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getUsedResources();

    void setUsedResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setPreemptedMemorySeconds(long arg0);

    void setNumUsedContainers(int arg0);

    void setMemorySeconds(long arg0);

    long getMemorySeconds();

    void setClusterUsagePercentage(float arg0);

    void setQueueUsagePercentage(float arg0);

    void setNumReservedContainers(int arg0);

    void setReservedResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getReservedResources();

    int getNumReservedContainers();

    void setPreemptedVcoreSeconds(long arg0);

    long getPreemptedMemorySeconds();
}

package org.apache.hadoop.yarn.api.records;

public interface ApplicationResourceUsageReportJVMInterface {

    float getQueueUsagePercentage();

    void setVcoreSeconds(long arg0);

    java.util.Map getPreemptedResourceSecondsMap();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getNeededResources();

    void setNeededResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getUsedResources();

    long getVcoreSeconds();

    void setNumUsedContainers(int arg0);

    long getMemorySeconds();

    void setQueueUsagePercentage(float arg0);

    void setClusterUsagePercentage(float arg0);

    void setReservedResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getReservedResources();

    int getNumReservedContainers();

    java.util.Map getResourceSecondsMap();

    long getPreemptedVcoreSeconds();

    int getNumUsedContainers();

    float getClusterUsagePercentage();

    void setPreemptedResourceSecondsMap(java.util.Map<java.lang.String, java.lang.Long> arg0);

    void setUsedResources_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setPreemptedMemorySeconds(long arg0);

    void setMemorySeconds(long arg0);

    void setNumReservedContainers(int arg0);

    void setResourceSecondsMap(java.util.Map<java.lang.String, java.lang.Long> arg0);

    void setPreemptedVcoreSeconds(long arg0);

    long getPreemptedMemorySeconds();
}

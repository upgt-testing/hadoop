package org.apache.hadoop.yarn.server.nodemanager.metrics;

public interface NodeManagerMetricsJVMInterface {

    void endInitingContainer();

    long getCacheSizeBeforeClean();

    void endReInitingContainer();

    void changeContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0, org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg1);

    void setGoodLocalDirsDiskUtilizationPerc(int arg0);

    int getContainerUsedVMemGB();

    void killedContainer();

    void setBadLocalDirs(int arg0);

    void setPrivateBytesDeleted(long arg0);

    int getGoodLocalDirsDiskUtilizationPerc();

    void completedContainer();

    org.apache.hadoop.metrics2.source.JvmMetricsJVMInterface getJvmMetrics();

    void failedContainer();

    void setTotalBytesDeleted(long arg0);

    long getPrivateBytesDeleted();

    void releaseContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void rollbackContainerOnFailure();

    void startOpportunisticContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void allocateContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void endRunningContainer();

    void setNodeUsedMemGB(long arg0);

    int getAllocatedOpportunisticVCores();

    int getReInitializingContainer();

    int getNodeUsedVMemGB();

    int getFailedContainers();

    void setBadLogDirs(int arg0);

    int getCompletedContainers();

    int getBadLocalDirs();

    float getContainerCpuUtilization();

    void initingContainer();

    void setNodeCpuUtilization(float arg0);

    long getPublicBytesDeleted();

    float getNodeCpuUtilization();

    void setPublicBytesDeleted(long arg0);

    int getGoodLogDirsDiskUtilizationPerc();

    void setGoodLogDirsDiskUtilizationPerc(int arg0);

    void addContainerLaunchDuration(long arg0);

    int getRunningOpportunisticContainers();

    void launchedContainer();

    int getRunningContainers();

    int getBadLogDirs();

    int getContainerUsedMemGB();

    int getContainersRolledbackOnFailure();

    void setCacheSizeBeforeClean(long arg0);

    long getTotalBytesDeleted();

    void addResource_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void completeOpportunisticContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void setNodeUsedVMemGB(long arg0);

    void setContainerCpuUtilization(float arg0);

    int getNodeUsedMemGB();

    void setContainerUsedVMemGB(long arg0);

    void setContainerUsedMemGB(long arg0);

    void runningContainer();

    int getKilledContainers();

    long getAllocatedOpportunisticGB();

    void reInitingContainer();
}

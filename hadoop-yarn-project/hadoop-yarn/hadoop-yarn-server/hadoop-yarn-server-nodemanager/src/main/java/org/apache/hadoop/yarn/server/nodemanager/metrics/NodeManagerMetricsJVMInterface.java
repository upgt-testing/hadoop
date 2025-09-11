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

    int getQueuedGuaranteedContainers();

    void setPrivateBytesDeleted(long arg0);

    int getGoodLocalDirsDiskUtilizationPerc();

    float getNodeGpuUtilization();

    void completedContainer();

    org.apache.hadoop.metrics2.source.JvmMetricsJVMInterface getJvmMetrics();

    int getQueuedOpportunisticContainers();

    void failedContainer();

    void setTotalBytesDeleted(long arg0);

    long getPrivateBytesDeleted();

    void releaseContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void startOpportunisticContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void rollbackContainerOnFailure();

    void allocateContainer_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    void endRunningContainer();

    void setNodeUsedMemGB(long arg0);

    int getAllocatedOpportunisticVCores();

    int getReInitializingContainer();

    int getNodeUsedVMemGB();

    int getFailedContainers();

    void setQueuedContainers(int arg0, int arg1);

    void localizationCacheHitMiss(long arg0);

    void runningApplication();

    void endPausedContainer();

    void setBadLogDirs(int arg0);

    int getCompletedContainers();

    void setNodeGpuUtilization(float arg0);

    int getBadLocalDirs();

    float getContainerCpuUtilization();

    void initingContainer();

    void setNodeCpuUtilization(float arg0);

    void pausedContainer();

    long getPublicBytesDeleted();

    float getNodeCpuUtilization();

    int getPausedContainers();

    void endRunningApplication();

    void setPublicBytesDeleted(long arg0);

    void localizationComplete(long arg0);

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

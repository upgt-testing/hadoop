package org.apache.hadoop.yarn.server.api.records;

public interface OpportunisticContainersStatusJVMInterface {

    int getOpportCoresUsed();

    int getWaitQueueLength();

    void setQueuedOpportContainers(int arg0);

    long getOpportMemoryUsed();

    void setRunningOpportContainers(int arg0);

    void setOpportMemoryUsed(long arg0);

    void setEstimatedQueueWaitTime(int arg0);

    int getQueuedOpportContainers();

    void setOpportCoresUsed(int arg0);

    int getRunningOpportContainers();

    void setWaitQueueLength(int arg0);

    int getEstimatedQueueWaitTime();
}

package org.apache.hadoop.yarn.server.api.records;

public interface OpportunisticContainersStatusJVMInterface {

    int getOpportCoresUsed();

    void setQueuedOpportContainers(int arg0);

    void setRunningOpportContainers(int arg0);

    int getQueuedOpportContainers();

    void setEstimatedQueueWaitTime(int arg0);

    void setOpportCoresUsed(int arg0);

    int getRunningOpportContainers();

    void setWaitQueueLength(int arg0);

    int getOpportQueueCapacity();

    int getWaitQueueLength();

    long getOpportMemoryUsed();

    void setOpportMemoryUsed(long arg0);

    void setOpportQueueCapacity(int arg0);

    int getEstimatedQueueWaitTime();
}

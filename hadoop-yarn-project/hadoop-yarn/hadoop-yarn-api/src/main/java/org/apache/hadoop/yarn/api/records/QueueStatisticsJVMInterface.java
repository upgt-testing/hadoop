package org.apache.hadoop.yarn.api.records;

public interface QueueStatisticsJVMInterface {

    void setReservedContainers(long arg0);

    long getAllocatedContainers();

    long getNumAppsRunning();

    void setNumAppsRunning(long arg0);

    void setAllocatedMemoryMB(long arg0);

    void setNumAppsSubmitted(long arg0);

    void setPendingContainers(long arg0);

    long getReservedVCores();

    long getNumAppsFailed();

    void setReservedVCores(long arg0);

    long getPendingContainers();

    long getReservedMemoryMB();

    void setAvailableVCores(long arg0);

    long getPendingVCores();

    long getNumAppsKilled();

    void setNumAppsFailed(long arg0);

    void setPendingMemoryMB(long arg0);

    void setNumAppsCompleted(long arg0);

    long getNumAppsSubmitted();

    long getAvailableMemoryMB();

    long getAvailableVCores();

    void setAvailableMemoryMB(long arg0);

    void setNumAppsPending(long arg0);

    void setAllocatedContainers(long arg0);

    void setPendingVCores(long arg0);

    void setAllocatedVCores(long arg0);

    void setNumAppsKilled(long arg0);

    void setNumActiveUsers(long arg0);

    long getAllocatedMemoryMB();

    long getPendingMemoryMB();

    long getNumActiveUsers();

    long getNumAppsCompleted();

    long getAllocatedVCores();

    long getNumAppsPending();

    long getReservedContainers();

    void setReservedMemoryMB(long arg0);
}

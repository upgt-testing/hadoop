package org.apache.hadoop.yarn.server.api.records;

public interface ContainerQueuingLimitJVMInterface {

    void setMaxQueueLength(int arg0);

    void setMaxQueueWaitTimeInMs(int arg0);

    int getMaxQueueWaitTimeInMs();

    int getMaxQueueLength();
}

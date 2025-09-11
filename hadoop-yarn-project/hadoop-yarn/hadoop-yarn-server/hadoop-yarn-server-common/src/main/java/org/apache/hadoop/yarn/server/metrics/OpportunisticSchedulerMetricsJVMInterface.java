package org.apache.hadoop.yarn.server.metrics;

public interface OpportunisticSchedulerMetricsJVMInterface {

    long getAggregatedRackLocalContainers();

    void incrNodeLocalOppContainers();

    int getAllocatedContainers();

    long getAggregatedAllocatedContainers();

    void incrAllocatedOppContainers(int arg0);

    void addAllocateOLatencyEntry(long arg0);

    void incrOffSwitchOppContainers();

    long getAggregatedNodeLocalContainers();

    long getAggregatedOffSwitchContainers();

    long getAggregatedReleasedContainers();

    void incrReleasedOppContainers(int arg0);

    void incrRackLocalOppContainers();
}

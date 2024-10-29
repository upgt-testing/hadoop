package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface OpportunisticSchedulerMetricsInterface {

    int getAllocatedContainers();

    long getAggregatedAllocatedContainers();

    long getAggregatedReleasedContainers();

    long getAggregatedNodeLocalContainers();

    long getAggregatedRackLocalContainers();

    long getAggregatedOffSwitchContainers();

    void incrAllocatedOppContainers(int numContainers);

    void incrReleasedOppContainers(int numContainers);

    void incrNodeLocalOppContainers();

    void incrRackLocalOppContainers();

    void incrOffSwitchOppContainers();

    void addAllocateOLatencyEntry(long latency);
}

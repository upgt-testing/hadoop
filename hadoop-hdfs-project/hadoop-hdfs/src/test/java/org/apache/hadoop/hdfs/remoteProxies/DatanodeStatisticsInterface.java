package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DatanodeStatisticsInterface {

    long getCapacityTotal();

    long getCapacityUsed();

    float getCapacityUsedPercent();

    long getCapacityRemaining();

    float getCapacityRemainingPercent();

    long getBlockPoolUsed();

    float getPercentBlockPoolUsed();

    long getCacheCapacity();

    long getCacheUsed();

    int getXceiverCount();

    int getInServiceXceiverCount();

    int getNumDatanodesInService();

    long getCapacityUsedNonDFS();

    long[] getStats();

    int getExpiredHeartbeats();

    long getProvidedCapacity();
}

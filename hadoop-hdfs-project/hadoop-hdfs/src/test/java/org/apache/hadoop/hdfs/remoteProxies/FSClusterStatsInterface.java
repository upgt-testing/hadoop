package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface FSClusterStatsInterface {

    int getTotalLoad();

    boolean isAvoidingStaleDataNodesForWrite();

    int getNumDatanodesInService();

    double getInServiceXceiverAverage();
}

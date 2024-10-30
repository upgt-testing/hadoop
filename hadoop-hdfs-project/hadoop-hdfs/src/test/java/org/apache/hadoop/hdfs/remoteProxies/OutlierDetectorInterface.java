package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.OutlierMetrics;

import java.util.*;
import java.io.*;

public interface OutlierDetectorInterface {

    Map<String, Double> getOutliers(Map<String, Double> stats);

    Map<String, OutlierMetrics> getOutlierMetrics(Map<String, Double> stats);

    void setMinNumResources(long minNodes);

    long getMinOutlierDetectionNodes();

    void setLowThresholdMs(long thresholdMs);

    long getLowThresholdMs();
}

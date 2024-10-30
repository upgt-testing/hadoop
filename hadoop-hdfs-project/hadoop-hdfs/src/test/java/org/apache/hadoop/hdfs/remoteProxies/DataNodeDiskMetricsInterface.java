package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.protocol.SlowDiskReports;

import java.util.*;
import java.io.*;

public interface DataNodeDiskMetricsInterface {

    Map<String, Map<SlowDiskReports.DiskOp, Double>> getDiskOutliersStats();

    void shutdownAndWait();

    void addSlowDiskForTesting(String slowDiskPath, Map<SlowDiskReports.DiskOp, Double> latencies);

    List<String> getSlowDisksToExclude();

    void setLowThresholdMs(long thresholdMs);

    long getLowThresholdMs();

    void setMinOutlierDetectionDisks(long minDisks);

    long getMinOutlierDetectionDisks();

    OutlierDetectorInterface getSlowDiskDetector();
}

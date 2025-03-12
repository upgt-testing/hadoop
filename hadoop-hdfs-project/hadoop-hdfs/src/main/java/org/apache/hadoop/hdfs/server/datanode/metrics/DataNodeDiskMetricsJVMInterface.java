package org.apache.hadoop.hdfs.server.datanode.metrics;

import org.apache.hadoop.hdfs.server.protocol.SlowDiskReports;

import java.util.Map;

public interface DataNodeDiskMetricsJVMInterface {
    void addSlowDiskForTesting(String slowDiskPath, Map<SlowDiskReports.DiskOp, Double> latencies);
}

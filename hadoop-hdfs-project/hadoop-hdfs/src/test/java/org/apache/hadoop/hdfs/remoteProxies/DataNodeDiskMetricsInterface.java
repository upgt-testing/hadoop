package org.apache.hadoop.hdfs.remoteProxies;

public interface DataNodeDiskMetricsInterface {
    void setMinOutlierDetectionDisks(long arg0);
    long getLowThresholdMs();
    long getMinOutlierDetectionDisks();
    java.util.List<java.lang.String> getSlowDisksToExclude();
    void detectAndUpdateDiskOutliers(java.util.Map<java.lang.String, java.lang.Double> arg0, java.util.Map<java.lang.String, java.lang.Double> arg1, java.util.Map<java.lang.String, java.lang.Double> arg2);
    void shutdownAndWait();
    void setLowThresholdMs(long arg0);
    void startDiskOutlierDetectionThread();
    java.util.Map<java.lang.String, java.util.Map<org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp, java.lang.Double>> getDiskOutliersStats();
    void addSlowDiskForTesting(java.lang.String arg0, java.util.Map<org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp, java.lang.Double> arg1);
    OutlierDetectorInterface getSlowDiskDetector();
    void addDiskStat(java.util.Map<java.lang.String, java.util.Map<org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp, java.lang.Double>> arg0, java.lang.String arg1, org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp arg2, double arg3);
}
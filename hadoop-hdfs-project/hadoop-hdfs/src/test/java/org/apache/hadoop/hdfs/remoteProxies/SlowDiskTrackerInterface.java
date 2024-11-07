package org.apache.hadoop.hdfs.remoteProxies;

public interface SlowDiskTrackerInterface {
    void cleanUpOldReports(long arg0);
    void setReportValidityMs(long arg0);
    void addSlowDiskReport(java.lang.String arg0, SlowDiskReportsInterface arg1);
    long getReportValidityMs();
    java.util.ArrayList<org.apache.hadoop.hdfs.server.blockmanagement.SlowDiskTracker.DiskLatency> getSlowDisksReport();
    java.lang.String getSlowDiskIDForReport(java.lang.String arg0, java.lang.String arg1);
    java.util.ArrayList<org.apache.hadoop.hdfs.server.blockmanagement.SlowDiskTracker.DiskLatency> getSlowDisks(java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.blockmanagement.SlowDiskTracker.DiskLatency> arg0, int arg1, long arg2);
    void checkAndUpdateReportIfNecessary();
    void updateSlowDiskReportAsync(long arg0);
    java.lang.String getSlowDiskReportAsJsonString();
}
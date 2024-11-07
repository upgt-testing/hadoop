package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotDiffReportInterface {
    java.util.List<org.apache.hadoop.hdfs.protocol.SnapshotDiffReport.DiffReportEntry> getDiffList();
    java.lang.String toString();
    DiffStatsInterface getStats();
    java.lang.String getLaterSnapshotName();
    java.lang.String getSnapshotRoot();
    java.lang.String getFromSnapshot();
}
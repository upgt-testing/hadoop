package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotDiffReportListingInterface {
    java.util.List<org.apache.hadoop.hdfs.protocol.SnapshotDiffReportListing.DiffReportListingEntry> getCreateList();
    java.util.List<org.apache.hadoop.hdfs.protocol.SnapshotDiffReportListing.DiffReportListingEntry> getModifyList();
    int getLastIndex();
    boolean getIsFromEarlier();
    java.util.List<org.apache.hadoop.hdfs.protocol.SnapshotDiffReportListing.DiffReportListingEntry> getDeleteList();
    byte[] getLastPath();
}
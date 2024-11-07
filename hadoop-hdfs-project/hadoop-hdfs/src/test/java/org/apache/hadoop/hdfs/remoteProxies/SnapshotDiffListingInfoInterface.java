package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotDiffListingInfoInterface {
    SnapshotInterface getEarlier();
    SnapshotDiffReportListingInterface generateReport();
    boolean addFileDiff(INodeFileInterface arg0, byte[][] arg1);
    boolean isFromEarlier();
    boolean addDirDiff(long arg0, byte[][] arg1, ChildrenDiffInterface arg2);
    void setLastIndex(int arg0);
    byte[][] newPath(byte[][] arg0, byte[] arg1);
    SnapshotInterface getLater();
    byte[][] findRenameTargetPath(INodeInterface arg0, SnapshotInterface arg1);
    void setLastPath(byte[][] arg0);
    int getTotalEntries();
}
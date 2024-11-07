package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotDiffInfoInterface {
    SnapshotDiffReportInterface generateReport();
    void addDirDiff(INodeDirectoryInterface arg0, byte[][] arg1, ChildrenDiffInterface arg2);
    RenameEntryInterface getEntry(long arg0);
    SnapshotInterface getTo();
    void incrementDirsCompared();
    void incrementFilesCompared();
    void incrementFilesProcessed();
    void addFileDiff(INodeFileInterface arg0, byte[][] arg1);
//    java.util.List<org.apache.hadoop.hdfs.protocol.SnapshotDiffReport.DiffReportEntry> generateReport(ChildrenDiffInterface arg0, byte[][] arg1, boolean arg2, java.util.Map<java.lang.Long, org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotDiffInfo.RenameEntry> arg3);
    void incrementDirsProcessed();
    SnapshotInterface getFrom();
    boolean isFromEarlier();
    void setRenameTarget(long arg0, byte[][] arg1);
    void addChildrenListingTime(long arg0);
}
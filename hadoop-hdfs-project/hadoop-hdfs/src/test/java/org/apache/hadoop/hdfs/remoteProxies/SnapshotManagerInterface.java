package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotManagerInterface {
    void setSnapshotCounter(int arg0);
    INodeDirectoryInterface getSnapshottableRoot(INodesInPathInterface arg0) throws java.io.IOException;
    int getMaxSnapshotID();
    int getSnapshotCounter();
    void checkNestedSnapshottable(INodeDirectoryInterface arg0, java.lang.String arg1) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    int getNumSnapshottableDirs();
    BeanInterface[] getSnapshots();
    BeanInterface[] getSnapshottableDirectories();
    void setSnapshottable(java.lang.String arg0, boolean arg1) throws java.io.IOException;
    void registerMXBean();
    void renameSnapshot(INodesInPathInterface arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, long arg4) throws java.io.IOException;
    SnapshottableDirectoryStatusInterface[] getSnapshottableDirListing(java.lang.String arg0);
    void setCaptureOpenFiles(boolean arg0);
    void clearSnapshottableDirs();
    java.util.Map<java.lang.Integer, org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot> read(java.io.DataInput arg0, LoaderInterface arg1) throws java.io.IOException;
    SnapshotDiffReportListingInterface diff(INodesInPathInterface arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, byte[] arg4, int arg5, int arg6) throws java.io.IOException;
    INodeDirectoryInterface[] getSnapshottableDirs();
    boolean isDescendantOfSnapshotRoot(INodeDirectoryInterface arg0);
    boolean isAllowNestedSnapshots();
    void setAllowNestedSnapshots(boolean arg0);
    java.lang.String createSnapshot(LeaseManagerInterface arg0, INodesInPathInterface arg1, java.lang.String arg2, java.lang.String arg3, long arg4) throws java.io.IOException;
    boolean getSkipCaptureAccessTimeOnlyChange();
    INodeDirectoryInterface getSnapshottableAncestorDir(INodesInPathInterface arg0) throws java.io.IOException;
    void addSnapshottable(INodeDirectoryInterface arg0);
    int getNumSnapshots();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void removeSnapshottable(INodeDirectoryInterface arg0);
    void shutdown();
    BeanInterface toBean(INodeDirectoryInterface arg0);
    void removeSnapshottable(java.util.List<org.apache.hadoop.hdfs.server.namenode.INodeDirectory> arg0);
    void setNumSnapshots(int arg0);
    BeanInterface toBean(SnapshotInterface arg0);
    void deleteSnapshot(INodesInPathInterface arg0, java.lang.String arg1, ReclaimContextInterface arg2, long arg3) throws java.io.IOException;
    void resetSnapshottable(java.lang.String arg0) throws java.io.IOException;
    SnapshotDiffReportInterface diff(INodesInPathInterface arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.io.IOException;
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectorySnapshottableFeatureInterface {
    SnapshotInterface getSnapshotByName(INodeDirectoryInterface arg0, java.lang.String arg1) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    void destroyDstSubtree(ReclaimContextInterface arg0, INodeInterface arg1, int arg2, int arg3);
    SnapshotInterface addSnapshot(INodeDirectoryInterface arg0, int arg1, java.lang.String arg2, LeaseManagerInterface arg3, boolean arg4, int arg5, long arg6) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    boolean removeChild(INodeDirectoryInterface arg0, INodeInterface arg1, int arg2);
    void computeDiffRecursively(INodeDirectoryInterface arg0, INodeInterface arg1, java.util.List<byte[]> arg2, SnapshotDiffInfoInterface arg3);
    void setSnapshotQuota(int arg0);
    int searchSnapshot(byte[] arg0);
    int getSnapshotQuota();
    void cleanDirectory(ReclaimContextInterface arg0, INodeDirectoryInterface arg1, int arg2, int arg3);
    byte[][] findRenameTargetPath(INodeDirectoryInterface arg0, WithNameInterface arg1, int arg2);
    boolean computeDiffBetweenSnapshots(SnapshotInterface arg0, SnapshotInterface arg1, ChildrenDiffInterface arg2, INodeDirectoryInterface arg3);
    void cleanDeletedINode(ReclaimContextInterface arg0, INodeInterface arg1, int arg2, int arg3);
    SnapshotInterface getSnapshotById(int arg0);
    QuotaCountsInterface computeQuotaUsage4CurrentDirectory(BlockStoragePolicySuiteInterface arg0, byte arg1);
    void addSnapshot(SnapshotInterface arg0);
    DirectoryDiffListInterface getDiffs();
    boolean computeDiffRecursively(INodeDirectoryInterface arg0, INodeInterface arg1, java.util.List<byte[]> arg2, SnapshotDiffListingInfoInterface arg3, byte[][] arg4, int arg5, boolean arg6);
    SnapshotDiffListingInfoInterface computeDiff(INodeDirectoryInterface arg0, INodeDirectoryInterface arg1, java.lang.String arg2, java.lang.String arg3, byte[] arg4, int arg5, int arg6) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    SnapshotDiffInfoInterface computeDiff(INodeDirectoryInterface arg0, INodeDirectoryInterface arg1, java.lang.String arg2, java.lang.String arg3) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    org.apache.hadoop.hdfs.util.ReadOnlyList<org.apache.hadoop.hdfs.server.namenode.INode> getChildrenList(INodeDirectoryInterface arg0, int arg1);
    int getLastSnapshotId();
    INodeInterface saveChild2Snapshot(INodeDirectoryInterface arg0, INodeInterface arg1, int arg2, INodeInterface arg3);
    void clear(ReclaimContextInterface arg0, INodeDirectoryInterface arg1);
    void getSnapshotDirectory(java.util.List<org.apache.hadoop.hdfs.server.namenode.INodeDirectory> arg0);
    INodeInterface getChild(INodeDirectoryInterface arg0, byte[] arg1, int arg2);
    java.lang.String toString();
    SnapshotInterface getSnapshot(byte[] arg0);
    org.apache.hadoop.hdfs.util.ReadOnlyList<org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot> getSnapshotList();
    boolean addChild(INodeDirectoryInterface arg0, INodeInterface arg1, boolean arg2, int arg3);
    void computeContentSummary4Snapshot(BlockStoragePolicySuiteInterface arg0, ContentCountsInterface arg1) throws org.apache.hadoop.security.AccessControlException;
    java.util.Map<org.apache.hadoop.hdfs.server.namenode.INode, org.apache.hadoop.hdfs.server.namenode.INode> cloneDiffList(java.util.List<org.apache.hadoop.hdfs.server.namenode.INode> arg0);
    void dumpTreeRecursively(INodeDirectoryInterface arg0, java.io.PrintWriter arg1, java.lang.StringBuilder arg2, int arg3);
    SnapshotInterface removeSnapshot(ReclaimContextInterface arg0, INodeDirectoryInterface arg1, java.lang.String arg2, long arg3) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    void renameSnapshot(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2, long arg3) throws org.apache.hadoop.hdfs.protocol.SnapshotException;
    int getNumSnapshots();
}
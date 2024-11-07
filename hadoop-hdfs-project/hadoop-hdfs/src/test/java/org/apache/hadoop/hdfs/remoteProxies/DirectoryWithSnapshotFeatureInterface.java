package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryWithSnapshotFeatureInterface {
    void destroyDstSubtree(ReclaimContextInterface arg0, INodeInterface arg1, int arg2, int arg3);
    org.apache.hadoop.hdfs.util.ReadOnlyList<org.apache.hadoop.hdfs.server.namenode.INode> getChildrenList(INodeDirectoryInterface arg0, int arg1);
    DirectoryDiffListInterface getDiffs();
    void computeContentSummary4Snapshot(BlockStoragePolicySuiteInterface arg0, ContentCountsInterface arg1) throws org.apache.hadoop.security.AccessControlException;
    boolean addChild(INodeDirectoryInterface arg0, INodeInterface arg1, boolean arg2, int arg3);
    java.lang.String toString();
    java.util.Map<org.apache.hadoop.hdfs.server.namenode.INode, org.apache.hadoop.hdfs.server.namenode.INode> cloneDiffList(java.util.List<org.apache.hadoop.hdfs.server.namenode.INode> arg0);
    void cleanDeletedINode(ReclaimContextInterface arg0, INodeInterface arg1, int arg2, int arg3);
    QuotaCountsInterface computeQuotaUsage4CurrentDirectory(BlockStoragePolicySuiteInterface arg0, byte arg1);
    void getSnapshotDirectory(java.util.List<org.apache.hadoop.hdfs.server.namenode.INodeDirectory> arg0);
    boolean computeDiffBetweenSnapshots(SnapshotInterface arg0, SnapshotInterface arg1, ChildrenDiffInterface arg2, INodeDirectoryInterface arg3);
    boolean removeChild(INodeDirectoryInterface arg0, INodeInterface arg1, int arg2);
    void clear(ReclaimContextInterface arg0, INodeDirectoryInterface arg1);
    int getLastSnapshotId();
    void cleanDirectory(ReclaimContextInterface arg0, INodeDirectoryInterface arg1, int arg2, int arg3);
    INodeInterface saveChild2Snapshot(INodeDirectoryInterface arg0, INodeInterface arg1, int arg2, INodeInterface arg3);
    INodeInterface getChild(INodeDirectoryInterface arg0, byte[] arg1, int arg2);
}
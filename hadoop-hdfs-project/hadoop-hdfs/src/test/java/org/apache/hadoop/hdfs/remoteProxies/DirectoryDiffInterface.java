package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryDiffInterface {
    int compareTo(java.lang.Integer arg0);
    java.lang.String toString();
//    void setPosterior(D arg0);
    void combinePosteriorAndCollectBlocks(ReclaimContextInterface arg0, INodeDirectoryInterface arg1, DirectoryDiffInterface arg2);
    boolean isSnapshotRoot();
    void destroyDiffAndCollectBlocks(ReclaimContextInterface arg0, INodeDirectoryInterface arg1);
    void setSnapshotRoot(org.apache.hadoop.hdfs.server.namenode.INodeDirectoryAttributes arg0);
//    A getSnapshotINode();
    void writeSnapshot(java.io.DataOutput arg0) throws java.io.IOException;
    org.apache.hadoop.hdfs.util.ReadOnlyList<org.apache.hadoop.hdfs.server.namenode.INode> getChildrenList(INodeDirectoryInterface arg0);
    int getSnapshotId();
//    D getPosterior();
    INodeInterface getChild(byte[] arg0, boolean arg1, INodeDirectoryInterface arg2);
    int getChildrenSize();
    ChildrenDiffInterface getChildrenDiff();
//    void saveSnapshotCopy(A arg0);
    void setSnapshotId(int arg0);
    void write(java.io.DataOutput arg0, ReferenceMapInterface arg1) throws java.io.IOException;
}
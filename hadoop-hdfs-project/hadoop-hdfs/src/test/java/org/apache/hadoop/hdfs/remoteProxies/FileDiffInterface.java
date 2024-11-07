package org.apache.hadoop.hdfs.remoteProxies;

public interface FileDiffInterface {
    int getSnapshotId();
    BlockInfoInterface[] getBlocks();
    void setBlocks(BlockInfoInterface[] arg0);
    void write(java.io.DataOutput arg0, ReferenceMapInterface arg1) throws java.io.IOException;
    void combinePosteriorAndCollectBlocks(ReclaimContextInterface arg0, INodeFileInterface arg1, FileDiffInterface arg2);
//    void saveSnapshotCopy(A arg0);
//    D getPosterior();
    int compareTo(java.lang.Integer arg0);
//    void setPosterior(D arg0);
//    A getSnapshotINode();
    java.lang.String toString();
    void destroyAndCollectSnapshotBlocks(BlocksMapUpdateInfoInterface arg0);
    long getFileSize();
    void writeSnapshot(java.io.DataOutput arg0) throws java.io.IOException;
    void destroyDiffAndCollectBlocks(ReclaimContextInterface arg0, INodeFileInterface arg1);
    void setSnapshotId(int arg0);
}
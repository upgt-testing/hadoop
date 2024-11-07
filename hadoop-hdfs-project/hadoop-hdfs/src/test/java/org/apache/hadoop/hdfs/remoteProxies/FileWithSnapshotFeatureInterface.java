package org.apache.hadoop.hdfs.remoteProxies;

public interface FileWithSnapshotFeatureInterface {
    void updateQuotaAndCollectBlocks(ReclaimContextInterface arg0, INodeFileInterface arg1, FileDiffInterface arg2);
    void deleteCurrentFile();
    boolean changedBetweenSnapshots(INodeFileInterface arg0, SnapshotInterface arg1, SnapshotInterface arg2);
    short getMaxBlockRepInDiffs(FileDiffInterface arg0);
    boolean isCurrentFileDeleted();
    java.lang.String getDetailedString();
    java.lang.String toString();
    void clearDiffs();
    FileDiffListInterface getDiffs();
    void collectBlocksAndClear(ReclaimContextInterface arg0, INodeFileInterface arg1);
    void cleanFile(ReclaimContextInterface arg0, INodeFileInterface arg1, int arg2, int arg3, byte arg4);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface FileDiffListInterface {
//    java.util.Spliterator<T> spliterator();
//    D checkAndAddLatestSnapshotDiff(int arg0, N arg1);
    void createDiffsIfNeeded();
    int getLastSnapshotId();
    BlockInfoInterface[] findEarlierSnapshotBlocks(int arg0);
    int getPrior(int arg0, boolean arg1);
//    D createDiff(int arg0, N arg1);
    int updatePrior(int arg0, int arg1);
    int getPrior(int arg0);
    boolean isEmpty();
    void clear();
    void combineAndCollectSnapshotBlocks(ReclaimContextInterface arg0, INodeFileInterface arg1, FileDiffInterface arg2);
    org.apache.hadoop.hdfs.server.namenode.INodeFileAttributes createSnapshotCopy(INodeFileInterface arg0);
//    org.apache.hadoop.hdfs.server.namenode.snapshot.DiffList<D> newDiffs();
    int getSnapshotById(int arg0);
//    java.util.Iterator<T> iterator();
    BlockInfoInterface[] findLaterSnapshotBlocks(int arg0);
//    D addDiff(int arg0, N arg1);
    int getDiffIndexById(int arg0);
    java.lang.String toString();
    void destroyAndCollectSnapshotBlocks(BlocksMapUpdateInfoInterface arg0);
//    A createSnapshotCopy(N arg0);
//    void deleteSnapshotDiff(ReclaimContextInterface arg0, int arg1, int arg2, N arg3);
//    A getSnapshotINode(int arg0, A arg1);
    <D extends Comparable<Integer>> org.apache.hadoop.hdfs.server.namenode.snapshot.DiffList<D> asList();
    int[] changedBetweenSnapshots(SnapshotInterface arg0, SnapshotInterface arg1);
    <D> D getLast();
//    java.util.Iterator<D> iterator();
//    void addFirst(D arg0);
    FileDiffInterface createDiff(int arg0, INodeFileInterface arg1);
//    D getDiffById(int arg0);
    void saveSelf2Snapshot(int arg0, INodeFileInterface arg1, org.apache.hadoop.hdfs.server.namenode.INodeFileAttributes arg2, boolean arg3);
//    void forEach(java.util.function.Consumer<? super T> arg0);
//    D addLast(D arg0);
//    D saveSelf2Snapshot(int arg0, N arg1, A arg2);
}
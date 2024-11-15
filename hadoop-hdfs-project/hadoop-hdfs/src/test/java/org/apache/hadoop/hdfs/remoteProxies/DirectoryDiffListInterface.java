package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryDiffListInterface {
    boolean replaceCreatedChild(INodeInterface arg0, INodeInterface arg1);
    org.apache.hadoop.hdfs.server.namenode.INodeDirectoryAttributes createSnapshotCopy(INodeDirectoryInterface arg0);
//    D getDiffById(int arg0);
//    A getSnapshotINode(int arg0, A arg1);
//    D saveSelf2Snapshot(int arg0, N arg1, A arg2);
    int getLastSnapshotId();
    <D extends Comparable<Integer>> org.apache.hadoop.hdfs.server.namenode.snapshot.DiffList<D> asList();
    int[] changedBetweenSnapshots(SnapshotInterface arg0, SnapshotInterface arg1);
//    void addFirst(D arg0);
//    D addDiff(int arg0, N arg1);
    DirectoryDiffInterface createDiff(int arg0, INodeDirectoryInterface arg1);
    int getPrior(int arg0, boolean arg1);
//    D addLast(D arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    boolean removeDeletedChild(INodeInterface arg0);
//    java.util.Iterator<D> iterator();
//    A createSnapshotCopy(N arg0);
    int getDiffIndexById(int arg0);
    org.apache.hadoop.hdfs.server.namenode.snapshot.DiffList<org.apache.hadoop.hdfs.server.namenode.snapshot.DirectoryWithSnapshotFeature.DirectoryDiff> newDiffs();
    void createDiffsIfNeeded();
    <D> D getLast();
    int getPrior(int arg0);
    void clear();
//    D createDiff(int arg0, N arg1);
//    void deleteSnapshotDiff(ReclaimContextInterface arg0, int arg1, int arg2, N arg3);
//    org.apache.hadoop.hdfs.server.namenode.snapshot.DiffList<D> newDiffs();
//    java.util.Iterator<T> iterator();
    int updatePrior(int arg0, int arg1);
//    java.util.Spliterator<T> spliterator();
    int getSnapshotById(int arg0);
    java.util.List<org.apache.hadoop.hdfs.server.namenode.snapshot.DirectoryWithSnapshotFeature.DirectoryDiff> getDiffListBetweenSnapshots(int arg0, int arg1, INodeDirectoryInterface arg2);
    java.lang.String toString();
//    D checkAndAddLatestSnapshotDiff(int arg0, N arg1);
    boolean isEmpty();
    int findSnapshotDeleted(INodeInterface arg0);
}
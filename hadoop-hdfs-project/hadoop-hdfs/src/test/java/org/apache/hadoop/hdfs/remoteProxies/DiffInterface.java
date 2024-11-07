package org.apache.hadoop.hdfs.remoteProxies;

public interface DiffInterface<K, E> {
    java.util.List<E> apply2Current(java.util.List<E> arg0);
    <E> void remove(java.util.List<E> arg0, int arg1, E arg2);
    void addCreated(E arg0, int arg1);
    boolean isEmpty();
    java.util.List<E> getDeletedUnmodifiable();
    void clearDeleted();
    void undoModify(E arg0, E arg1, UndoInfoInterface<E> arg2);
    ContainerInterface<E> accessPrevious(K arg0);
    void addDeleted(E arg0, int arg1);
    boolean removeCreated(E arg0);
    void combinePosterior(DiffInterface<K, E> arg0, org.apache.hadoop.hdfs.util.Diff.Processor<E> arg1);
    int create(E arg0);
    java.util.List<E> getCreatedUnmodifiable();
    ContainerInterface<E> accessCurrent(K arg0);
    void clearCreated();
    UndoInfoInterface<E> modify(E arg0, E arg1);
    UndoInfoInterface<E> delete(E arg0);
    boolean containsDeleted(E arg0);
    <K, E> ContainerInterface<E> accessPrevious(K arg0, java.util.List<E> arg1, java.util.List<E> arg2);
    void undoCreate(E arg0, int arg1);
    E setCreated(int arg0, E arg1);
    E getDeleted(K arg0);
    <K, E> int search(java.util.List<E> arg0, K arg1);
    java.lang.String toString();
    void undoDelete(E arg0, UndoInfoInterface<E> arg1);
//    boolean containsDeleted(K arg0);
    java.util.List<E> apply2Previous(java.util.List<E> arg0);
    boolean removeDeleted(E arg0);
    <K, E> java.util.List<E> apply2Previous(java.util.List<E> arg0, java.util.List<E> arg1, java.util.List<E> arg2);
}
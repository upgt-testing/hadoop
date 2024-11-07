package org.apache.hadoop.hdfs.remoteProxies;

public interface BatchedListEntriesInterface<E> {
    E get(int arg0);
    int size();
    boolean hasMore();
}
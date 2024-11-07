package org.apache.hadoop.hdfs.remoteProxies;

public interface UnmodifiableIteratorInterface<E> {
    boolean hasNext();
    E next();
    void remove();
    void forEachRemaining(java.util.function.Consumer<? super E> arg0);
}
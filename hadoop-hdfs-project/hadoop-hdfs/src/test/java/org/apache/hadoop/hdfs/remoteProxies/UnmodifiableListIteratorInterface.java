package org.apache.hadoop.hdfs.remoteProxies;

public interface UnmodifiableListIteratorInterface<E> {
    boolean hasNext();
    E previous();
    E next();
    int previousIndex();
    void set(E arg0);
    void forEachRemaining(java.util.function.Consumer<? super E> arg0);
    int nextIndex();
    boolean hasPrevious();
    void add(E arg0);
    void remove();
}
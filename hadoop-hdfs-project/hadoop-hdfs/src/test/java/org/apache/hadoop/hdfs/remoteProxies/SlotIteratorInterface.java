package org.apache.hadoop.hdfs.remoteProxies;

public interface SlotIteratorInterface {
//    E next();
//    void forEachRemaining(java.util.function.Consumer<? super E> arg0);
    SlotInterface next();
    void remove();
    boolean hasNext();
}
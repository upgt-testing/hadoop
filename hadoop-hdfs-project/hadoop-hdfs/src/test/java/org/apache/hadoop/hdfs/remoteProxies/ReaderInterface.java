package org.apache.hadoop.hdfs.remoteProxies;

public interface ReaderInterface<U> {
//    java.util.Spliterator<T> spliterator();
    void close() throws java.io.IOException;
//    void forEach(java.util.function.Consumer<? super T> arg0);
    java.util.Optional<U> resolve(BlockInterface arg0) throws java.io.IOException;
//    java.util.Iterator<T> iterator();
}
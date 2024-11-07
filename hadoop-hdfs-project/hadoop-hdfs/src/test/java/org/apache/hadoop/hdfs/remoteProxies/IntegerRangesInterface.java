package org.apache.hadoop.hdfs.remoteProxies;

public interface IntegerRangesInterface {
//    void forEach(java.util.function.Consumer<? super T> arg0);
//    java.util.Spliterator<T> spliterator();
    java.util.Iterator<java.lang.Integer> iterator();
//    java.util.Iterator<T> iterator();
    int convertToInt(java.lang.String arg0, int arg1);
    int getRangeStart();
    java.lang.String toString();
    boolean isEmpty();
    boolean isIncluded(int arg0);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface LightWeightGSetInterface<K, E> {
    E remove(K arg0);
    boolean contains(K arg0);
    E get(K arg0);
//    void forEach(java.util.function.Consumer<? super T> arg0);
    void printDetails(java.io.PrintStream arg0);
    java.lang.String toString();
    int actualArrayLength(int arg0);
//    java.util.Spliterator<T> spliterator();
    java.util.Iterator<E> iterator();
    int computeCapacity(long arg0, double arg1, java.lang.String arg2);
//    java.util.Iterator<T> iterator();
    E put(E arg0);
    void clear();
    int size();
    java.util.Collection<E> values();
    int getIndex(K arg0);
    E remove(int arg0, K arg1);
    E convert(org.apache.hadoop.util.LightWeightGSet.LinkedElement arg0);
    int computeCapacity(double arg0, java.lang.String arg1);
}
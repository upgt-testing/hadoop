package org.apache.hadoop.hdfs.remoteProxies;

public interface EnumCountersInterface<E> {
    long sum();
    void set(EnumCountersInterface<E> arg0);
    boolean anyGreaterOrEqual(long arg0);
    void subtract(EnumCountersInterface<E> arg0);
    void subtract(E arg0, long arg1);
    void negation();
    void reset();
    java.lang.String toString();
    long get(E arg0);
    boolean equals(java.lang.Object arg0);
    void add(EnumCountersInterface<E> arg0);
    void add(E arg0, long arg1);
    long[] asArray();
    boolean allLessOrEqual(long arg0);
    void set(E arg0, long arg1);
    int hashCode();
    void reset(long arg0);
    EnumCountersInterface<E> deepCopyEnumCounter();
}
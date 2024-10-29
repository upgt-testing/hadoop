package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface EnumCountersInterface {

    //long get(E e);

    long[] asArray();

    void negation();

    //void set(E e, long value);

    //void set(EnumCounters<E> that);

    void reset();

    //void add(E e, long value);

    //void add(EnumCounters<E> that);

    //void subtract(E e, long value);

    //void subtract(EnumCounters<E> that);

    long sum();

    boolean equals(Object obj);

    //EnumCounters<E> deepCopyEnumCounter();

    int hashCode();

    String toString();

    void reset(long val);

    boolean allLessOrEqual(long val);

    boolean anyGreaterOrEqual(long val);
}

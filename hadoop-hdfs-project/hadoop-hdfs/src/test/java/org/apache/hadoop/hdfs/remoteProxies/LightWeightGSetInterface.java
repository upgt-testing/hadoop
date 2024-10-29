package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface LightWeightGSetInterface {

    int size();

    //E get(K key);

    //boolean contains(K key);

    //E put(E element);

    //E remove(K key);

    //Collection<E> values();

    //Iterator<E> iterator();

    String toString();

    void printDetails(PrintStream out);

    void clear();
}

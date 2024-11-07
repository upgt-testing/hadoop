package org.apache.hadoop.hdfs.remoteProxies;

public interface ShmIdInterface {
    boolean equals(java.lang.Object arg0);
    ShmIdInterface createRandom();
    long getLo();
    long getHi();
    int compareTo(ShmIdInterface arg0);
    int hashCode();
    java.lang.String toString();
}
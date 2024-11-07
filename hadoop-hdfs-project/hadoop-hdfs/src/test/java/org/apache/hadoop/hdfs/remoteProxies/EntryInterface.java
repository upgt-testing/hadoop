package org.apache.hadoop.hdfs.remoteProxies;

public interface EntryInterface {
    int hashCode();
    java.lang.String getName();
    java.lang.String getPattern();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    boolean isInclusive();
}
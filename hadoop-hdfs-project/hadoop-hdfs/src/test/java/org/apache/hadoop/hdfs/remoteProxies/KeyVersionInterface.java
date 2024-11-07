package org.apache.hadoop.hdfs.remoteProxies;

public interface KeyVersionInterface {
    int hashCode();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    java.lang.String getVersionName();
    java.lang.String getName();
    byte[] getMaterial();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface HostPortInterface {
    java.lang.String toString();
    boolean hasPort();
    boolean hasHost();
    java.lang.String normalizeHost(java.lang.String arg0);
    int getPort();
    int parsePort(java.lang.String arg0) throws java.lang.IllegalArgumentException;
    java.lang.String getHost();
    int getPort(int arg0);
}
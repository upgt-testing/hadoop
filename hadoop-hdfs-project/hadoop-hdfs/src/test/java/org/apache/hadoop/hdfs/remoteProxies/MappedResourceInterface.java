package org.apache.hadoop.hdfs.remoteProxies;

public interface MappedResourceInterface<E> {
    int hashCode();
    org.eclipse.jetty.http.pathmap.PathSpec getPathSpec();
    int compareTo(MappedResourceInterface<E> arg0);
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    E getResource();
}
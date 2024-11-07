package org.apache.hadoop.hdfs.remoteProxies;

public interface ProvidedStorageLocationInterface {
    long getLength();
    boolean equals(java.lang.Object arg0);
    long getOffset();
    int hashCode();
    byte[] getNonce();
    PathInterface getPath();
}
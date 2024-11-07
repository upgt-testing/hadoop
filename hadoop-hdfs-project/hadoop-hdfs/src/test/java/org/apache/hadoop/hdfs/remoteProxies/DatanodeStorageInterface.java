package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeStorageInterface {
    boolean equals(java.lang.Object arg0);
    boolean isValidStorageId(java.lang.String arg0);
    int hashCode();
    java.lang.String getStorageID();
    java.lang.String toString();
    java.lang.String generateUuid();
    org.apache.hadoop.hdfs.server.protocol.DatanodeStorage.State getState();
    org.apache.hadoop.fs.StorageType getStorageType();
}
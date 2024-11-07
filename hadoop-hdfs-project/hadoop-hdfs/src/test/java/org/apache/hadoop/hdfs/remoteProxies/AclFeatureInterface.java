package org.apache.hadoop.hdfs.remoteProxies;

public interface AclFeatureInterface {
    int getEntryAt(int arg0);
    boolean equals(java.lang.Object arg0);
    int getRefCount();
    java.lang.String toString();
    int decrementAndGetRefCount();
    int hashCode();
    int getEntriesSize();
    int incrementAndGetRefCount();
}
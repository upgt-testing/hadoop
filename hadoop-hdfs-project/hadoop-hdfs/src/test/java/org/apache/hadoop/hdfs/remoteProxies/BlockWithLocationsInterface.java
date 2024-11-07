package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockWithLocationsInterface {
    java.lang.StringBuilder appendString(int arg0, java.lang.StringBuilder arg1);
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    java.lang.String[] getStorageIDs();
    BlockInterface getBlock();
    java.lang.String[] getDatanodeUuids();
    java.lang.String toString();
}
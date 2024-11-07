package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageReportInterface {
    long getBlockPoolUsed();
    boolean isFailed();
    long getRemaining();
    DatanodeStorageInterface getStorage();
    long getDfsUsed();
    long getCapacity();
    long getNonDfsUsed();
}
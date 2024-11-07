package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageReceivedDeletedBlocksInterface {
    java.lang.String toString();
    DatanodeStorageInterface getStorage();
    java.lang.String getStorageID();
    ReceivedDeletedBlockInfoInterface[] getBlocks();
}
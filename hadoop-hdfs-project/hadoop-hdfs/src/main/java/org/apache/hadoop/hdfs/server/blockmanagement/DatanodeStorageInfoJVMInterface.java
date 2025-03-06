package org.apache.hadoop.hdfs.server.blockmanagement;

public interface DatanodeStorageInfoJVMInterface {
    boolean areBlocksOnFailedStorage();
    boolean areBlockContentsStale();
    String getStorageID();
}

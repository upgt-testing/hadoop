package org.apache.hadoop.hdfs.server.datanode.fsdataset.impl;


public interface FsVolumeImplJVMInterface {
    String getStorageID();
    void setCapacityForTesting(long capacity);
}

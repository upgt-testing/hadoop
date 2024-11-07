package org.apache.hadoop.hdfs.remoteProxies;

public interface VolumeBuilderInterface {
    void addBpStorageDirectories(java.lang.String arg0, java.util.List<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> arg1);
    StorageDirectoryInterface getStorageDirectory();
    void build();
}
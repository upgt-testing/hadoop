package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeStorageReportInterface {
    StorageReportInterface[] getStorageReports();
    DatanodeInfoInterface getDatanodeInfo();
}
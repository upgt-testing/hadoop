package org.apache.hadoop.hdfs.server.namenode;

public interface QuotaCountsJVMInterface {
    long getNameSpace();
    long getStorageSpace();
}

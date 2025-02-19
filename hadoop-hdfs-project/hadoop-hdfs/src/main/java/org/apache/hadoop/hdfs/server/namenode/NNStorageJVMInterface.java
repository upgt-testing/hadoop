package org.apache.hadoop.hdfs.server.namenode;

public interface NNStorageJVMInterface {
    long getMostRecentCheckpointTxId();
    int getNumStorageDirs();
}

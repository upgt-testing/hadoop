package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.common.StorageDirectoryJVMInterface;

import java.net.URI;

public interface NNStorageJVMInterface {
    long getMostRecentCheckpointTxId();
    int getNumStorageDirs();
    StorageDirectoryJVMInterface getStorageDirectory(URI uri);
}

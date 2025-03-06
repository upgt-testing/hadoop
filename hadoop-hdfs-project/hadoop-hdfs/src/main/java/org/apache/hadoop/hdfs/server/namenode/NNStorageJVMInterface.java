package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.common.StorageDirectoryJVMInterface;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public interface NNStorageJVMInterface {
    StorageDirectoryJVMInterface getStorageDir(int idx);
    long getMostRecentCheckpointTxId();
    int getNumStorageDirs();
    StorageDirectoryJVMInterface getStorageDirectory(URI uri);
    long getCTime();
    void writeAll() throws IOException;
    int getServiceLayoutVersion();
}

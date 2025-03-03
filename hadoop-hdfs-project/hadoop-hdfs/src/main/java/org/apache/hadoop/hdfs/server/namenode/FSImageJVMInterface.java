package org.apache.hadoop.hdfs.server.namenode;

public interface FSImageJVMInterface {
    long getMostRecentCheckpointTxId();
    NNStorageJVMInterface getStorage();
    int getLayoutVersion();
    int getNamespaceID();
    String getClusterID();
    String getBlockPoolID();
    long getLastAppliedTxId();
    FSEditLogJVMInterface getEditLog();
}

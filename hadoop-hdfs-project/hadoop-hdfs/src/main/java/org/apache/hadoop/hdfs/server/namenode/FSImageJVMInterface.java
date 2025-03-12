package org.apache.hadoop.hdfs.server.namenode;

import java.io.IOException;

public interface FSImageJVMInterface {
    boolean hasRollbackFSImage() throws IOException;
    long getMostRecentCheckpointTxId();
    NNStorageJVMInterface getStorage();
    int getLayoutVersion();
    int getNamespaceID();
    String getClusterID();
    String getBlockPoolID();
    long getLastAppliedTxId();
    FSEditLogJVMInterface getEditLog();
}

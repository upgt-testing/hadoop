package org.apache.hadoop.hdfs.remoteProxies;

public interface SaveNamespaceContextInterface {
    void reportErrorOnStorageDirectory(StorageDirectoryInterface arg0);
    FSNamesystemInterface getSourceNamesystem();
    java.util.List<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> getErrorSDs();
    void checkCancelled() throws org.apache.hadoop.hdfs.server.namenode.SaveNamespaceCancelledException;
    long getTxId();
    void markComplete();
}
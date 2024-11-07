package org.apache.hadoop.hdfs.remoteProxies;

public interface FSImageStorageInspectorInterface {
    boolean isUpgradeFinalized();
    boolean needToSave();
//    java.util.List<org.apache.hadoop.hdfs.server.namenode.FSImageStorageInspector.FSImageFile> getLatestImages() throws java.io.IOException;
    void inspectDirectory(StorageDirectoryInterface arg0) throws java.io.IOException;
    long getMaxSeenTxId();
}
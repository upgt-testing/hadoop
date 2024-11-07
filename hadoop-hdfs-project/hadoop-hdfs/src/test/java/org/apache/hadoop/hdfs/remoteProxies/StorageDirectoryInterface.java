package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageDirectoryInterface {
    java.io.File getFinalizedTmp();
    org.apache.hadoop.hdfs.server.common.Storage.StorageState analyzeStorage(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption arg0, StorageInterface arg1) throws java.io.IOException;
    java.io.File getBlockPoolCurrentDir(java.lang.String arg0, StorageLocationInterface arg1);
    void read(java.io.File arg0, StorageInterface arg1) throws java.io.IOException;
    java.io.File getCurrentDir();
    java.io.File getStorageLocationFile(StorageLocationInterface arg0);
    boolean isShared();
    java.io.File getPreviousVersionFile();
    void doRecover(org.apache.hadoop.hdfs.server.common.Storage.StorageState arg0) throws java.io.IOException;
    void lock() throws java.io.IOException;
    boolean isLockSupported() throws java.io.IOException;
    long getDirecorySize();
    void deleteAsync(java.io.File arg0) throws java.io.IOException;
    void checkEmptyCurrent() throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException, java.io.IOException;
    java.io.File getRemovedTmp();
    java.io.File getLastCheckpointTmp();
    void setStorageUuid(java.lang.String arg0);
    java.io.File getRoot();
    java.nio.channels.FileLock tryLock() throws java.io.IOException;
    java.lang.String getStorageUuid();
    void unlock() throws java.io.IOException;
    java.io.File getVersionFile();
    java.io.File getPreviousCheckpoint();
    org.apache.hadoop.hdfs.server.common.Storage.StorageDirType getStorageDirType();
    java.lang.String toString();
    void clearDirectory() throws java.io.IOException;
    java.io.File getPreviousDir();
    java.io.File getPreviousTmp();
    org.apache.hadoop.hdfs.server.common.Storage.StorageState analyzeStorage(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption arg0, StorageInterface arg1, boolean arg2) throws java.io.IOException;
    boolean hasSomeData() throws java.io.IOException;
    StorageLocationInterface getStorageLocation();
}
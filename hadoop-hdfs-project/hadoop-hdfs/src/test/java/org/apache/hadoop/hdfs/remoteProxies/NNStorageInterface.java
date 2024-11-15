package org.apache.hadoop.hdfs.remoteProxies;

public interface NNStorageInterface {
    java.util.Collection<java.net.URI> getImageDirectories() throws java.io.IOException;
    java.util.Iterator<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> dirIterator(boolean arg0);
    java.lang.String getNameNodeFileName(org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg0, long arg1);
    StorageDirectoryInterface getSingularStorageDir();
    int getNumStorageDirs(org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeDirType arg0);
    long getCTime();
    java.lang.String getImageFileName(long arg0);
    java.io.File getFsImage(long arg0, java.util.EnumSet<org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile> arg1);
    void setClusterID(java.lang.String arg0);
    java.lang.Iterable<StorageDirectoryInterface> dirIterable(org.apache.hadoop.hdfs.server.common.Storage.StorageDirType arg0);
    java.lang.String listStorageDirectories();
    java.io.File getTemporaryEditsFile(StorageDirectoryInterface arg0, long arg1, long arg2, long arg3);
    void readPreviousVersionProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    void format(StorageDirectoryInterface arg0) throws java.io.IOException;
    void rename(java.io.File arg0, java.io.File arg1) throws java.io.IOException;
    void readProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.io.File getFsImageName(long arg0, org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg1);
    java.lang.String newBlockPoolID() throws java.net.UnknownHostException;
    void reportErrorsOnDirectories(java.util.List<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> arg0);
    java.io.File[] getFsImageNameCheckpoint(long arg0);
    int getNamespaceID();
    java.lang.String getProperty(java.util.Properties arg0, StorageDirectoryInterface arg1, java.lang.String arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    NamespaceInfoInterface newNamespaceInfo() throws java.net.UnknownHostException;
    void writeProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.lang.String getInProgressEditsFileName(long arg0);
    void addStorageDir(StorageDirectoryInterface arg0);
    void checkVersionUpgradable(int arg0) throws java.io.IOException;
    NamespaceInfoInterface getNamespaceInfo();
    java.lang.String getDeprecatedProperty(java.lang.String arg0);
    java.util.List<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> getStorageDirs();
    void inspectStorageDirs(FSImageStorageInspectorInterface arg0) throws java.io.IOException;
    java.lang.String determineClusterId();
    void format(NamespaceInfoInterface arg0) throws java.io.IOException;
    java.lang.String getFinalizedEditsFileName(long arg0, long arg1);
    java.lang.String getBuildVersion();
    void setBlockPoolID(java.lang.String arg0);
    java.lang.String getCheckpointImageFileName(long arg0);
    java.io.File getStorageFile(StorageDirectoryInterface arg0, org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg1);
    java.lang.String getClusterIdFromColonSeparatedString(java.lang.String arg0);
    java.util.Iterator<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> dirIterator();
    void writeTransactionIdFileToStorage(long arg0);
    int newNamespaceID();
    java.lang.String getTemporaryEditsFileName(long arg0, long arg1, long arg2);
    java.util.List<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> getRemovedStorageDirs();
    java.lang.String getRollbackImageFileName(long arg0);
    java.lang.String newClusterID();
    void setDeprecatedPropertiesForUpgrade(java.util.Properties arg0);
    java.io.File getFsImageName(long arg0);
    java.lang.String getClusterID();
    boolean containsStorageDir(StorageLocationInterface arg0, java.lang.String arg1) throws java.io.IOException;
    void checkSchemeConsistency(java.net.URI arg0) throws java.io.IOException;
    void checkStorageType(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    long getMostRecentCheckpointTime();
    void processStartupOptionsForUpgrade(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption arg0, int arg1) throws java.io.IOException;
    int getNumStorageDirs();
    int getLayoutVersion();
    void updateNameDirSize();
    void checkOldLayoutStorage(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.lang.String getNNDirectorySize();
    boolean containsStorageDir(java.io.File arg0) throws java.io.IOException;
    int getServiceLayoutVersion();
    void setPropertiesFromFields(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    java.lang.String getLegacyOIVImageFileName(long arg0);
    StorageDirectoryInterface getStorageDirectory(java.net.URI arg0);
    FSImageStorageInspectorInterface readAndInspectDirs(java.util.EnumSet<org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile> arg0, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption arg1) throws java.io.IOException;
    long readTransactionIdFile(StorageDirectoryInterface arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    void setClusterId(java.util.Properties arg0, int arg1, StorageDirectoryInterface arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    StorageDirectoryInterface getStorageDir(int arg0);
    java.io.File getStorageFile(StorageDirectoryInterface arg0, org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg1, long arg2);
    void writeTransactionIdFile(StorageDirectoryInterface arg0, long arg1) throws java.io.IOException;
    java.util.List<java.io.File> getFiles(org.apache.hadoop.hdfs.server.common.Storage.StorageDirType arg0, java.lang.String arg1);
    void unlockAll() throws java.io.IOException;
    java.util.Iterator<org.apache.hadoop.hdfs.server.common.Storage.StorageDirectory> dirIterator(org.apache.hadoop.hdfs.server.common.Storage.StorageDirType arg0, boolean arg1);
    void writeProperties(java.io.File arg0, java.util.Properties arg1) throws java.io.IOException;
    void deleteDir(java.io.File arg0) throws java.io.IOException;
    void setStorageDirectories(java.util.Collection<java.net.URI> arg0, java.util.Collection<java.net.URI> arg1, java.util.Collection<java.net.URI> arg2) throws java.io.IOException;
    java.io.File getFinalizedEditsFile(StorageDirectoryInterface arg0, long arg1, long arg2);
    java.util.Collection<java.net.URI> getDirectories(org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeDirType arg0) throws java.io.IOException;
    void setMostRecentCheckpointInfo(long arg0, long arg1);
    void reportErrorOnFile(java.io.File arg0);
    void attemptRestoreRemovedStorage();
    void writeProperties(java.io.File arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    void setLayoutVersion(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.IncorrectVersionException, org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.lang.String getBlockPoolID();
    java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
    boolean isPreUpgradableLayout(StorageDirectoryInterface arg0) throws java.io.IOException;
    boolean is203LayoutVersion(int arg0);
    void setStorageInfo(StorageInfoInterface arg0);
    void setStorageDirectories(java.util.Collection<java.net.URI> arg0, java.util.Collection<java.net.URI> arg1) throws java.io.IOException;
    void reportErrorsOnDirectory(StorageDirectoryInterface arg0);
    void setServiceLayoutVersion(int arg0);
    java.util.Properties readPropertiesFile(java.io.File arg0) throws java.io.IOException;
    java.lang.String toColonSeparatedString();
    boolean confirmFormat(java.lang.Iterable<? extends org.apache.hadoop.hdfs.server.common.Storage.FormatConfirmable> arg0, boolean arg1, boolean arg2) throws java.io.IOException;
    void writeTransactionIdFileToStorage(long arg0, org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeDirType arg1);
    java.io.File getInProgressEditsFile(StorageDirectoryInterface arg0, long arg1);
    void setcTime(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void nativeCopyFileUnbuffered(java.io.File arg0, java.io.File arg1, boolean arg2) throws java.io.IOException;
    void setRestoreFailedStorage(boolean arg0);
    boolean versionSupportsFederation(java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> arg0);
    void setFieldsFromProperties(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    void setBlockPoolID(java.io.File arg0, java.lang.String arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setDisablePreUpgradableLayoutCheck(boolean arg0);
    boolean containsStorageDir(StorageLocationInterface arg0) throws java.io.IOException;
    int getNsIdFromColonSeparatedString(java.lang.String arg0);
    java.io.File findImageFile(org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg0, long arg1);
    java.io.File findFinalizedEditsFile(long arg0, long arg1) throws java.io.IOException;
    long getMostRecentCheckpointTxId();
    boolean getRestoreFailedStorage();
    java.lang.String toString();
    void setNamespaceID(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void writeAll() throws java.io.IOException;
    void format() throws java.io.IOException;
    java.io.File getHighestFsImageName();
    java.io.File getImageFile(StorageDirectoryInterface arg0, org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeFile arg1, long arg2);
    java.io.File findFile(org.apache.hadoop.hdfs.server.namenode.NNStorage.NameNodeDirType arg0, java.lang.String arg1);
    java.util.Collection<java.net.URI> getEditsDirectories() throws java.io.IOException;
    void readProperties(StorageDirectoryInterface arg0, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.StartupOption arg1) throws java.io.IOException;
    java.util.Iterator<StorageDirectoryInterface> dirIterator(org.apache.hadoop.hdfs.server.common.Storage.StorageDirType arg0);
    java.lang.String getRegistrationID(StorageInfoInterface arg0);
}
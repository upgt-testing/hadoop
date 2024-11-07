package org.apache.hadoop.hdfs.remoteProxies;

public interface CheckpointSignatureInterface {
    boolean equals(java.lang.Object arg0);
    long getCTime();
    void setStorageInfo(StorageInfoInterface arg0);
    java.lang.String getClusterIdFromColonSeparatedString(java.lang.String arg0);
    int getNsIdFromColonSeparatedString(java.lang.String arg0);
    void readPreviousVersionProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    void readProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.lang.String getBlockpoolID();
    long getMostRecentCheckpointTxId();
    void setLayoutVersion(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.IncorrectVersionException, org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    boolean isSameCluster(FSImageInterface arg0);
    int getLayoutVersion();
    void setClusterId(java.util.Properties arg0, int arg1, StorageDirectoryInterface arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void validateStorageInfo(FSImageInterface arg0) throws java.io.IOException;
    boolean versionSupportsFederation(java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> arg0);
    void setFieldsFromProperties(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    void setServiceLayoutVersion(int arg0);
    int getServiceLayoutVersion();
    java.lang.String toColonSeparatedString();
    java.lang.String getProperty(java.util.Properties arg0, StorageDirectoryInterface arg1, java.lang.String arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    boolean namespaceIdMatches(FSImageInterface arg0);
    void setcTime(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setBlockpoolID(java.lang.String arg0);
    void checkStorageType(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    boolean storageVersionMatches(StorageInfoInterface arg0) throws java.io.IOException;
    int hashCode();
    void setNamespaceID(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    long getCurSegmentTxId();
    java.lang.String toString();
    java.util.Properties readPropertiesFile(java.io.File arg0) throws java.io.IOException;
    java.lang.String getClusterID();
    java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
    int getNamespaceID();
    int compareTo(CheckpointSignatureInterface arg0);
}
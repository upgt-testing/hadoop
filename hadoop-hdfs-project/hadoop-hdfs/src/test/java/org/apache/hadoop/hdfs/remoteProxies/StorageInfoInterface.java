package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageInfoInterface {
    java.lang.String getClusterIdFromColonSeparatedString(java.lang.String arg0);
    void setServiceLayoutVersion(int arg0);
    void checkStorageType(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.lang.String getProperty(java.util.Properties arg0, StorageDirectoryInterface arg1, java.lang.String arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    boolean versionSupportsFederation(java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> arg0);
    int getNsIdFromColonSeparatedString(java.lang.String arg0);
    int getLayoutVersion();
    void setcTime(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setStorageInfo(StorageInfoInterface arg0);
    java.lang.String getClusterID();
    int getServiceLayoutVersion();
    java.lang.String toString();
    long getCTime();
    void readPreviousVersionProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    void setNamespaceID(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.util.Properties readPropertiesFile(java.io.File arg0) throws java.io.IOException;
    void setClusterId(java.util.Properties arg0, int arg1, StorageDirectoryInterface arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
    int getNamespaceID();
    java.lang.String toColonSeparatedString();
    void readProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    void setFieldsFromProperties(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    void setLayoutVersion(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.IncorrectVersionException, org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface NamenodeRegistrationInterface {
    java.lang.String getHttpAddress();
    java.lang.String getClusterID();
    java.util.Properties readPropertiesFile(java.io.File arg0) throws java.io.IOException;
    void setClusterId(java.util.Properties arg0, int arg1, StorageDirectoryInterface arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.lang.String toString();
    void checkStorageType(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    int getNamespaceID();
    int getNsIdFromColonSeparatedString(java.lang.String arg0);
    void setNamespaceID(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void readProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.lang.String toColonSeparatedString();
    boolean isRole(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.NamenodeRole arg0);
    int getServiceLayoutVersion();
    java.lang.String getAddress();
    void setServiceLayoutVersion(int arg0);
    void setLayoutVersion(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.IncorrectVersionException, org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    int getLayoutVersion();
    int getVersion();
    void setStorageInfo(StorageInfoInterface arg0);
    java.lang.String getProperty(java.util.Properties arg0, StorageDirectoryInterface arg1, java.lang.String arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    long getCTime();
    void readPreviousVersionProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    void setFieldsFromProperties(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
    boolean versionSupportsFederation(java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> arg0);
    java.lang.String getClusterIdFromColonSeparatedString(java.lang.String arg0);
    org.apache.hadoop.hdfs.server.common.HdfsServerConstants.NamenodeRole getRole();
    void setcTime(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.lang.String getRegistrationID();
}
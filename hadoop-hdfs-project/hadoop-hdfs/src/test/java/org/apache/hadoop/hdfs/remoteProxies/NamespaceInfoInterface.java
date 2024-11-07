package org.apache.hadoop.hdfs.remoteProxies;

public interface NamespaceInfoInterface {
    void setClusterID(java.lang.String arg0);
    void checkStorageType(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    int getServiceLayoutVersion();
    java.lang.String getProperty(java.util.Properties arg0, StorageDirectoryInterface arg1, java.lang.String arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setNamespaceID(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setBlockPoolID(java.lang.String arg0);
    void setClusterId(java.util.Properties arg0, int arg1, StorageDirectoryInterface arg2) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    java.lang.String getBuildVersion();
    void setcTime(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void setFieldsFromProperties(java.util.Properties arg0, StorageDirectoryInterface arg1) throws java.io.IOException;
    boolean isCapabilitySupported(org.apache.hadoop.hdfs.server.protocol.NamespaceInfo.Capability arg0);
    java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
    int getNamespaceID();
    java.lang.String getClusterID();
    java.util.Properties readPropertiesFile(java.io.File arg0) throws java.io.IOException;
    int getLayoutVersion();
    java.lang.String getSoftwareVersion();
    boolean versionSupportsFederation(java.util.Map<java.lang.Integer, java.util.SortedSet<org.apache.hadoop.hdfs.protocol.LayoutVersion.LayoutFeature>> arg0);
    void setLayoutVersion(java.util.Properties arg0, StorageDirectoryInterface arg1) throws org.apache.hadoop.hdfs.server.common.IncorrectVersionException, org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
    void readProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.lang.String toColonSeparatedString();
    java.lang.String getBlockPoolID();
    void setServiceLayoutVersion(int arg0);
    org.apache.hadoop.ha.HAServiceProtocol.HAServiceState getState();
    void setState(org.apache.hadoop.ha.HAServiceProtocol.HAServiceState arg0);
    void readPreviousVersionProperties(StorageDirectoryInterface arg0) throws java.io.IOException;
    long getSupportedCapabilities();
    java.lang.String toString();
    void setCapabilities(long arg0);
    long getCTime();
    long getCapabilities();
    int getNsIdFromColonSeparatedString(java.lang.String arg0);
    void setStorageInfo(StorageInfoInterface arg0);
    java.lang.String getClusterIdFromColonSeparatedString(java.lang.String arg0);
    void validateStorage(NNStorageInterface arg0) throws java.io.IOException;
}
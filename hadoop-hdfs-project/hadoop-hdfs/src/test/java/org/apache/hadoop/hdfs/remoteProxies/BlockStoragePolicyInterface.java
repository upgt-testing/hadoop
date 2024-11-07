package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockStoragePolicyInterface {
    org.apache.hadoop.fs.StorageType[] getReplicationFallbacks();
    boolean equals(java.lang.Object arg0);
    org.apache.hadoop.fs.StorageType[] getCreationFallbacks();
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    byte getId();
    java.util.List<org.apache.hadoop.fs.StorageType> chooseStorageTypes(short arg0, java.lang.Iterable<org.apache.hadoop.fs.StorageType> arg1, java.util.List<org.apache.hadoop.fs.StorageType> arg2);
    java.lang.String toString();
    java.util.List<org.apache.hadoop.fs.StorageType> chooseStorageTypes(short arg0, java.lang.Iterable<org.apache.hadoop.fs.StorageType> arg1);
    boolean isCopyOnCreateFile();
    java.util.List<org.apache.hadoop.fs.StorageType> chooseExcess(short arg0, java.lang.Iterable<org.apache.hadoop.fs.StorageType> arg1);
    org.apache.hadoop.fs.StorageType getReplicationFallback(java.util.EnumSet<org.apache.hadoop.fs.StorageType> arg0);
    java.util.List<org.apache.hadoop.fs.StorageType> chooseStorageTypes(short arg0);
    java.lang.String getName();
    java.util.List<org.apache.hadoop.fs.StorageType> chooseStorageTypes(short arg0, java.lang.Iterable<org.apache.hadoop.fs.StorageType> arg1, java.util.EnumSet<org.apache.hadoop.fs.StorageType> arg2, boolean arg3);
    org.apache.hadoop.fs.StorageType getFallback(java.util.EnumSet<org.apache.hadoop.fs.StorageType> arg0, org.apache.hadoop.fs.StorageType[] arg1);
    int hashCode();
    void diff(java.util.List<org.apache.hadoop.fs.StorageType> arg0, java.lang.Iterable<org.apache.hadoop.fs.StorageType> arg1, java.util.List<org.apache.hadoop.fs.StorageType> arg2);
    org.apache.hadoop.fs.StorageType getCreationFallback(java.util.EnumSet<org.apache.hadoop.fs.StorageType> arg0);
}
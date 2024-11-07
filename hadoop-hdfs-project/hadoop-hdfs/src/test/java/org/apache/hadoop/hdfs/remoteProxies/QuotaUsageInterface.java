package org.apache.hadoop.hdfs.remoteProxies;

public interface QuotaUsageInterface {
    boolean isTypeQuotaSet();
    boolean equals(java.lang.Object arg0);
    boolean isTypeConsumedAvailable();
    java.lang.String formatSize(long arg0, boolean arg1);
    java.lang.String getHeader();
    long getSpaceConsumed();
    java.lang.String getTypesQuotaUsage(boolean arg0, java.util.List<org.apache.hadoop.fs.StorageType> arg1);
    long getFileAndDirectoryCount();
    long getQuota();
    void setSpaceConsumed(long arg0);
    java.lang.String toString();
    void setQuota(long arg0);
    long getTypeQuota(org.apache.hadoop.fs.StorageType arg0);
    java.lang.String toString(boolean arg0);
    java.lang.String toString(boolean arg0, boolean arg1, java.util.List<org.apache.hadoop.fs.StorageType> arg2);
    long getTypeConsumed(org.apache.hadoop.fs.StorageType arg0);
    java.lang.String getQuotaUsage(boolean arg0);
    long getSpaceQuota();
    void setSpaceQuota(long arg0);
    int hashCode();
    java.lang.String getStorageTypeHeader(java.util.List<org.apache.hadoop.fs.StorageType> arg0);
}
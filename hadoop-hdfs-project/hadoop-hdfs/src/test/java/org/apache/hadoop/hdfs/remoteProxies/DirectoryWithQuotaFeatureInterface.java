package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryWithQuotaFeatureInterface {
    void checkStoragespace(INodeDirectoryInterface arg0, long arg1);
    java.lang.String typeSpaceString();
    QuotaCountsInterface getSpaceAllowed();
    ContentSummaryComputationContextInterface computeContentSummary(INodeDirectoryInterface arg0, ContentSummaryComputationContextInterface arg1) throws org.apache.hadoop.security.AccessControlException;
    void verifyQuota(QuotaCountsInterface arg0) throws org.apache.hadoop.hdfs.protocol.QuotaExceededException;
    boolean isQuotaSet();
    void setQuota(EnumCountersInterface<org.apache.hadoop.fs.StorageType> arg0);
    void setQuota(long arg0, org.apache.hadoop.fs.StorageType arg1);
    boolean isQuotaByStorageTypeSet();
    void setQuota(long arg0, long arg1);
    java.lang.String toString();
    void setQuota(long arg0, long arg1, org.apache.hadoop.fs.StorageType arg2);
    java.lang.String storagespaceString();
    void addSpaceConsumed2Cache(QuotaCountsInterface arg0);
    void verifyStoragespaceQuota(long arg0) throws org.apache.hadoop.hdfs.protocol.DSQuotaExceededException;
    void setSpaceConsumed(QuotaCountsInterface arg0);
    void setSpaceConsumed(long arg0, long arg1, EnumCountersInterface<org.apache.hadoop.fs.StorageType> arg2);
    boolean isQuotaByStorageTypeSet(org.apache.hadoop.fs.StorageType arg0);
    QuotaCountsInterface getQuota();
    QuotaCountsInterface AddCurrentSpaceUsage(QuotaCountsInterface arg0);
    QuotaCountsInterface getSpaceConsumed();
    void verifyNamespaceQuota(long arg0) throws org.apache.hadoop.hdfs.protocol.NSQuotaExceededException;
    void verifyQuotaByStorageType(EnumCountersInterface<org.apache.hadoop.fs.StorageType> arg0) throws org.apache.hadoop.hdfs.protocol.QuotaByStorageTypeExceededException;
    java.lang.String namespaceString();
}
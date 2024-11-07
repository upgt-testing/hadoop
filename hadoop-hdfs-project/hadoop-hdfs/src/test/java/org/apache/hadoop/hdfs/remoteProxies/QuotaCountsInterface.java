package org.apache.hadoop.hdfs.remoteProxies;

public interface QuotaCountsInterface {
    QuotaCountsInterface subtract(QuotaCountsInterface arg0);
    void addTypeSpace(org.apache.hadoop.fs.StorageType arg0, long arg1);
    EnumCountersInterface<org.apache.hadoop.fs.StorageType> getTypeSpaces();
    long getTypeSpace(org.apache.hadoop.fs.StorageType arg0);
    QuotaCountsInterface negation();
    java.lang.String toString();
    long getNameSpace();
    void addStorageSpace(long arg0);
    EnumCountersInterface<org.apache.hadoop.hdfs.server.namenode.Quota> setQuotaCounter(EnumCountersInterface<org.apache.hadoop.hdfs.server.namenode.Quota> arg0, org.apache.hadoop.hdfs.server.namenode.Quota arg1, org.apache.hadoop.hdfs.server.namenode.Quota arg2, long arg3);
    long getStorageSpace();
    boolean equals(java.lang.Object arg0);
    QuotaCountsInterface add(QuotaCountsInterface arg0);
    void setTypeSpace(org.apache.hadoop.fs.StorageType arg0, long arg1);
    void addNameSpace(long arg0);
//    <T> EnumCountersInterface<T> modify(EnumCountersInterface<T> arg0, java.util.function.Consumer<org.apache.hadoop.hdfs.util.EnumCounters<T>> arg1);
    void setTypeSpaces(EnumCountersInterface<org.apache.hadoop.fs.StorageType> arg0);
    int hashCode();
    boolean anyTypeSpaceCountGreaterOrEqual(long arg0);
    void setNameSpace(long arg0);
    boolean anyNsSsCountGreaterOrEqual(long arg0);
    void setStorageSpace(long arg0);
}
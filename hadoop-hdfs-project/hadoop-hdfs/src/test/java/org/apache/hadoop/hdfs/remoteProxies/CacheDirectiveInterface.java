package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheDirectiveInterface {
//    void setPrev(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0, org.apache.hadoop.util.IntrusiveCollection.Element arg1);
    short getReplication();
    CachePoolInterface getPool();
    long getId();
    java.lang.String toString();
    int hashCode();
    long getExpiryTime();
    CacheDirectiveEntryInterface toEntry();
    CacheDirectiveStatsInterface toStats();
    void addFilesNeeded(long arg0);
//    boolean isInList(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0);
    boolean equals(java.lang.Object arg0);
//    org.apache.hadoop.util.IntrusiveCollection.Element getNext(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0);
    long getBytesCached();
//    org.apache.hadoop.util.IntrusiveCollection.Element getPrev(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0);
//    void insertInternal(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0, org.apache.hadoop.util.IntrusiveCollection.Element arg1, org.apache.hadoop.util.IntrusiveCollection.Element arg2);
//    void removeInternal(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0);
    java.lang.String getExpiryTimeString();
    long getBytesNeeded();
    void addFilesCached(long arg0);
    void addBytesCached(long arg0);
    long getFilesNeeded();
//    void setNext(IntrusiveCollectionInterface<? extends IntrusiveCollectionInterface.Element> arg0, org.apache.hadoop.util.IntrusiveCollection.Element arg1);
    java.lang.String getPath();
    long getFilesCached();
    CacheDirectiveInfoInterface toInfo();
    void addBytesNeeded(long arg0);
    void resetStatistics();
}
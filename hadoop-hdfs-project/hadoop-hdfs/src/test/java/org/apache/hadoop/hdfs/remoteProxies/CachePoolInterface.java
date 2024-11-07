package org.apache.hadoop.hdfs.remoteProxies;

public interface CachePoolInterface {
    DirectiveListInterface getDirectiveList();
    java.lang.String getPoolName();
    CachePoolInterface createFromInfo(CachePoolInfoInterface arg0);
    CachePoolInterface setMode(FsPermissionInterface arg0);
    long getBytesNeeded();
    void addBytesNeeded(long arg0);
    CachePoolStatsInterface getStats();
    void addBytesCached(long arg0);
    java.lang.String toString();
    CachePoolInterface createFromInfoAndDefaults(CachePoolInfoInterface arg0) throws java.io.IOException;
    long getFilesNeeded();
    long getLimit();
    void addFilesCached(long arg0);
    long getBytesOverlimit();
    long getMaxRelativeExpiryMs();
    void resetStatistics();
    CachePoolInterface setLimit(long arg0);
    CachePoolInterface setMaxRelativeExpiryMs(long arg0);
    void setDefaultReplication(short arg0);
    CachePoolInterface setOwnerName(java.lang.String arg0);
    long getBytesCached();
    long getFilesCached();
    java.lang.String getOwnerName();
    short getDefaultReplication();
    FsPermissionInterface getMode();
    void addFilesNeeded(long arg0);
    java.lang.String getGroupName();
    CachePoolEntryInterface getEntry(FSPermissionCheckerInterface arg0);
    CachePoolInfoInterface getInfo(boolean arg0);
    CachePoolInterface setGroupName(java.lang.String arg0);
}
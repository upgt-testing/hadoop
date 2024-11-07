package org.apache.hadoop.hdfs.remoteProxies;

public interface CachePoolStatsInterface {
    long getFilesNeeded();
    java.lang.String toString();
    long getBytesNeeded();
    long getFilesCached();
    long getBytesOverlimit();
    long getBytesCached();
}
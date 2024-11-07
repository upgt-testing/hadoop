package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheDirectiveStatsInterface {
    java.lang.String toString();
    boolean hasExpired();
    long getBytesNeeded();
    long getFilesCached();
    long getBytesCached();
    long getFilesNeeded();
}
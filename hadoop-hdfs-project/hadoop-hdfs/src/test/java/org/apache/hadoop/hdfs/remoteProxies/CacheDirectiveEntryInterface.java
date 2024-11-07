package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheDirectiveEntryInterface {
    CacheDirectiveInfoInterface getInfo();
    CacheDirectiveStatsInterface getStats();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface CachePoolEntryInterface {
    CachePoolInfoInterface getInfo();
    CachePoolStatsInterface getStats();
}
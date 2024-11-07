package org.apache.hadoop.hdfs.remoteProxies;

public interface RetryCacheMetricsInterface {
    long getCacheCleared();
    java.lang.String getName();
    long getCacheHit();
    void incrCacheUpdated();
    void incrCacheHit();
    RetryCacheMetricsInterface create(RetryCacheInterface arg0);
    long getCacheUpdated();
    void incrCacheCleared();
}
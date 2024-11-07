package org.apache.hadoop.hdfs.remoteProxies;

public interface RetryCacheInterface {
    void clear(RetryCacheInterface arg0);
    void addCacheEntryWithPayload(byte[] arg0, int arg1, java.lang.Object arg2);
    void setState(CacheEntryWithPayloadInterface arg0, boolean arg1, java.lang.Object arg2);
    CacheEntryWithPayloadInterface waitForCompletion(RetryCacheInterface arg0, java.lang.Object arg1);
    void addCacheEntry(byte[] arg0, int arg1);
    RetryCacheMetricsInterface getMetricsForTests();
    CacheEntryInterface waitForCompletion(RetryCacheInterface arg0);
    CacheEntryInterface newEntry(long arg0);
    void lock();
    void incrCacheClearedCounter();
    java.lang.String getCacheName();
    CacheEntryWithPayloadInterface newEntry(java.lang.Object arg0, long arg1);
    LightWeightGSetInterface<org.apache.hadoop.ipc.RetryCache.CacheEntry, org.apache.hadoop.ipc.RetryCache.CacheEntry> getCacheSet();
    void unlock();
    boolean skipRetryCache();
    CacheEntryInterface waitForCompletion(CacheEntryInterface arg0);
    void setState(CacheEntryInterface arg0, boolean arg1);
}
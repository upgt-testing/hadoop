package org.apache.hadoop.ipc.metrics;

public interface RetryCacheMetricsJVMInterface {
    long getCacheHit();
    long getCacheUpdated();
    long getCacheCleared();
}

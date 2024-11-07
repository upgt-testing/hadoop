package org.apache.hadoop.hdfs.remoteProxies;

public interface RpcDetailedMetricsInterface {
    void addProcessingTime(java.lang.String arg0, long arg1);
    RpcDetailedMetricsInterface create(int arg0);
    void shutdown();
    void init(java.lang.Class<?> arg0);
    void addDeferredProcessingTime(java.lang.String arg0, long arg1);
    java.lang.String name();
}
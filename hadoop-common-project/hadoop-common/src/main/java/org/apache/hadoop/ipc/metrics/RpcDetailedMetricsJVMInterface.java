package org.apache.hadoop.ipc.metrics;

public interface RpcDetailedMetricsJVMInterface {

    void addDeferredProcessingTime(java.lang.String arg0, long arg1);

    void shutdown();

    void addProcessingTime(java.lang.String arg0, long arg1);

    void init(java.lang.Class<?> arg0);

    java.lang.String name();
}

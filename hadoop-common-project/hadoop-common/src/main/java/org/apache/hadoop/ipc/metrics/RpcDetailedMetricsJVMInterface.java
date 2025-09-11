package org.apache.hadoop.ipc.metrics;

public interface RpcDetailedMetricsJVMInterface {

    void shutdown();

    void addProcessingTime(java.lang.String arg0, int arg1);

    void init(java.lang.Class<?> arg0);

    java.lang.String name();
}

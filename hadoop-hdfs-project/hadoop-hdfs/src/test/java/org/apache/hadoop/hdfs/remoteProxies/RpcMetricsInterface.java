package org.apache.hadoop.hdfs.remoteProxies;

public interface RpcMetricsInterface {
    RpcMetricsInterface create(ServerInterface arg0, ConfigurationInterface arg1);
    void addRpcLockWaitTime(long arg0);
    void incrAuthenticationFailures();
    void incrAuthorizationSuccesses();
    void incrAuthorizationFailures();
    void addRpcQueueTime(long arg0);
    void incrClientBackoff();
    long getRpcSlowCalls();
    MutableRateInterface getDeferredRpcProcessingTime();
    long numDroppedConnections();
    double getDeferredRpcProcessingMean();
    double getDeferredRpcProcessingStdDev();
    long getProcessingSampleCount();
    MutableRateInterface getRpcProcessingTime();
    void incrReceivedBytes(int arg0);
    void incrSlowRpc();
    double getProcessingMean();
    void incrAuthenticationSuccesses();
    java.util.concurrent.TimeUnit getMetricsTimeUnit(ConfigurationInterface arg0);
    long getTotalRequestsPerSecond();
    long getDeferredRpcProcessingSampleCount();
    int numOpenConnections();
    java.util.concurrent.TimeUnit getMetricsTimeUnit();
    double getProcessingStdDev();
    java.lang.String name();
    java.lang.String numOpenConnectionsPerUser();
    void addDeferredRpcProcessingTime(long arg0);
    long getTotalRequests();
    MetricsTagInterface getTag(java.lang.String arg0);
    int callQueueLength();
    void shutdown();
    void incrSentBytes(int arg0);
    void addRpcProcessingTime(long arg0);
}
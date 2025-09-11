package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    double getProcessingMean();

    void addRpcLockWaitTime(long arg0);

    void incrSentBytes(int arg0);

    void addDeferredRpcProcessingTime(long arg0);

    double getDeferredRpcProcessingMean();

    void incrClientBackoff();

    long getTotalRequests();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

    void incrAuthenticationSuccesses();

    void incrSlowRpc();

    long getProcessingSampleCount();

    void incrAuthenticationFailures();

    long numDroppedConnections();

    void incrAuthorizationSuccesses();

    void incrAuthorizationFailures();

    long getTotalRequestsPerSecond();

    java.util.concurrent.TimeUnit getMetricsTimeUnit();

    org.apache.hadoop.metrics2.MetricsTagJVMInterface getTag(java.lang.String arg0);

    java.lang.String numOpenConnectionsPerUser();

    long getRpcSlowCalls();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getRpcProcessingTime();

    java.lang.String name();

    void addRpcQueueTime(long arg0);

    int numOpenConnections();

    int callQueueLength();

    double getProcessingStdDev();

    double getDeferredRpcProcessingStdDev();

    void shutdown();

    void addRpcProcessingTime(long arg0);

    void incrReceivedBytes(int arg0);

    long getDeferredRpcProcessingSampleCount();
}

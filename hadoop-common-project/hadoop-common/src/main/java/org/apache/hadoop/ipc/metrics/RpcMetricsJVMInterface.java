package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    void addRpcLockWaitTime(long arg0);

    double getProcessingMean();

    void addDeferredRpcProcessingTime(long arg0);

    void incrSentBytes(int arg0);

    void incrClientBackoff();

    double getDeferredRpcProcessingMean();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

    void incrSlowRpc();

    void incrAuthenticationSuccesses();

    long getProcessingSampleCount();

    void incrAuthenticationFailures();

    long numDroppedConnections();

    void incrAuthorizationSuccesses();

    void incrAuthorizationFailures();

    java.lang.String numOpenConnectionsPerUser();

    long getRpcSlowCalls();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getRpcProcessingTime();

    java.lang.String name();

    int numOpenConnections();

    void addRpcQueueTime(long arg0);

    int callQueueLength();

    double getProcessingStdDev();

    double getDeferredRpcProcessingStdDev();

    void shutdown();

    void addRpcProcessingTime(long arg0);

    void incrReceivedBytes(int arg0);

    long getDeferredRpcProcessingSampleCount();
}

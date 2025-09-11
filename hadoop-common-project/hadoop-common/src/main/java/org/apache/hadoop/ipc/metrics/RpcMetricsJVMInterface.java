package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    void addRpcLockWaitTime(long arg0);

    double getProcessingMean();

    void incrSentBytes(int arg0);

    void addDeferredRpcProcessingTime(long arg0);

    double getDeferredRpcProcessingMean();

    void incrClientBackoff();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

    void incrSlowRpc();

    void incrAuthenticationSuccesses();

    long getProcessingSampleCount();

    void incrAuthenticationFailures();

    long numDroppedConnections();

    void incrAuthorizationSuccesses();

    void incrAuthorizationFailures();

    java.lang.String numOpenConnectionsPerUser();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getRpcProcessingTime();

    long getRpcSlowCalls();

    java.lang.String name();

    int numOpenConnections();

    void addRpcQueueTime(long arg0);

    int callQueueLength();

    double getProcessingStdDev();

    double getDeferredRpcProcessingStdDev();

    void shutdown();

    void incrReceivedBytes(int arg0);

    void addRpcProcessingTime(long arg0);

    long getDeferredRpcProcessingSampleCount();
}

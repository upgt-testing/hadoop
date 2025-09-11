package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    double getProcessingMean();

    void incrSentBytes(int arg0);

    void addDeferredRpcProcessingTime(long arg0);

    void incrClientBackoff();

    double getDeferredRpcProcessingMean();

    void addRpcQueueTime(int arg0);

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

    void incrAuthenticationSuccesses();

    void incrSlowRpc();

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

    int callQueueLength();

    void addRpcProcessingTime(int arg0);

    double getProcessingStdDev();

    double getDeferredRpcProcessingStdDev();

    void shutdown();

    void incrReceivedBytes(int arg0);

    long getDeferredRpcProcessingSampleCount();
}

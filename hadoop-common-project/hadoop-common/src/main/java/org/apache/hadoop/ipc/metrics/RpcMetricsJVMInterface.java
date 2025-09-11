package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    double getProcessingMean();

    void addDeferredRpcProcessingTime(long arg0);

    void incrSentBytes(int arg0);

    double getDeferredRpcProcessingMean();

    void incrClientBackoff();

    void addRpcQueueTime(int arg0);

    void incrAuthenticationSuccesses();

    void incrSlowRpc();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

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

    int callQueueLength();

    void addRpcProcessingTime(int arg0);

    double getProcessingStdDev();

    double getDeferredRpcProcessingStdDev();

    void shutdown();

    void incrReceivedBytes(int arg0);

    long getDeferredRpcProcessingSampleCount();
}

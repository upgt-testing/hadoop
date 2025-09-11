package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    void addRpcLockWaitTime(long arg0);

    double getProcessingMean();

    void incrSentBytes(int arg0);

    void addDeferredRpcProcessingTime(long arg0);

    double getDeferredRpcProcessingMean();

    void incrClientBackoff();

    void incrAuthenticationSuccesses();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getDeferredRpcProcessingTime();

    void incrSlowRpc();

    long getProcessingSampleCount();

    void incrAuthenticationFailures();

    long numDroppedConnections();

    void incrAuthorizationSuccesses();

    void incrAuthorizationFailures();

    org.apache.hadoop.metrics2.MetricsTagJVMInterface getTag(java.lang.String arg0);

    java.lang.String numOpenConnectionsPerUser();

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getRpcProcessingTime();

    long getRpcSlowCalls();

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

package org.apache.hadoop.ipc.metrics;

public interface RpcMetricsJVMInterface {

    double getProcessingMean();

    void incrSentBytes(int arg0);

    java.lang.String numOpenConnectionsPerUser();

    void incrClientBackoff();

    long getRpcSlowCalls();

    void addRpcQueueTime(int arg0);

    org.apache.hadoop.metrics2.lib.MutableRateJVMInterface getRpcProcessingTime();

    java.lang.String name();

    int numOpenConnections();

    int callQueueLength();

    void incrAuthenticationSuccesses();

    void addRpcProcessingTime(int arg0);

    void incrSlowRpc();

    double getProcessingStdDev();

    long getProcessingSampleCount();

    void incrAuthenticationFailures();

    void shutdown();

    void incrReceivedBytes(int arg0);

    long numDroppedConnections();

    void incrAuthorizationSuccesses();

    void incrAuthorizationFailures();
}

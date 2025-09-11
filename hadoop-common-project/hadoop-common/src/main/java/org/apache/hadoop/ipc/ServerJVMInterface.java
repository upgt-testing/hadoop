package org.apache.hadoop.ipc;

public interface ServerJVMInterface {

    void refreshServiceAcl_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0, org.apache.hadoop.security.authorize.PolicyProviderJVMInterface arg1);

    java.lang.Object call_bridge(java.lang.Object arg0, java.lang.String arg1, java.lang.Object arg2, long arg3) throws java.lang.Exception;

    int getMaxQueueSize();

    void setTracer(org.apache.htrace.core.Tracer arg0);

    void refreshCallQueue_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    int getNumOpenConnections();

    void stop();

    void join() throws java.lang.InterruptedException;

    void setSocketSendBufSize(int arg0);

    int getPort();

    int getCallQueueLen();

    int getNumReaders();

    org.apache.hadoop.security.authorize.ServiceAuthorizationManagerJVMInterface getServiceAuthorizationManager();

    java.lang.Object call_bridge(java.lang.Object arg0, long arg1) throws java.lang.Exception;

    long getNumDroppedConnections();

    void setClientBackoffEnabled(boolean arg0);

    boolean isClientBackoffEnabled();

    void addTerseExceptions(java.lang.Class<?>[] arg0);

    void start();

    org.apache.hadoop.ipc.metrics.RpcMetricsJVMInterface getRpcMetrics();

    void addSuppressedLoggingExceptions(java.lang.Class<?>[] arg0);

    org.apache.hadoop.ipc.metrics.RpcDetailedMetricsJVMInterface getRpcDetailedMetrics();

    void refreshServiceAclWithLoadedConfiguration_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0, org.apache.hadoop.security.authorize.PolicyProviderJVMInterface arg1);

    java.lang.Class getRpcRequestWrapper_bridge(java.lang.Object arg0);

    void queueCall_bridge(java.lang.Object arg0) throws java.io.IOException, java.lang.InterruptedException;

    java.net.InetSocketAddress getListenerAddress();

    java.lang.String getNumOpenConnectionsPerUser();
}

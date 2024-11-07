package org.apache.hadoop.hdfs.remoteProxies;

public interface ExternalCallInterface<T> {
    java.lang.Void run() throws java.io.IOException;
    int getRemotePort();
    java.lang.String getDetailedMetricsName();
    java.lang.String getHostAddress();
    void markCallCoordinated(boolean arg0);
    void sendResponse() throws java.io.IOException;
    void setPriorityLevel(int arg0);
    boolean isDone();
    boolean isResponseDeferred();
    UserGroupInformationInterface getUserGroupInformation();
    long getTimestampNanos();
//    T run() throws java.lang.Exception;
    boolean isCallCoordinated();
    void waitForCompletion() throws java.lang.InterruptedException;
    java.net.InetAddress getHostInetAddress();
    void abortResponse(java.lang.Throwable arg0) throws java.io.IOException;
    boolean isOpen();
    java.lang.String toString();
    void postponeResponse();
    void setDetailedMetricsName(java.lang.String arg0);
    UserGroupInformationInterface getRemoteUser();
    void doResponse(java.lang.Throwable arg0) throws java.io.IOException;
    void setClientStateId(long arg0);
    T get() throws java.lang.InterruptedException, java.util.concurrent.ExecutionException;
    ProcessingDetailsInterface getProcessingDetails();
    void doResponse(java.lang.Throwable arg0, org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto arg1);
    int getPriorityLevel();
    java.lang.String getProtocol();
    void setDeferredResponse(org.apache.hadoop.io.Writable arg0);
    long getClientStateId();
    void deferResponse();
    void setDeferredError(java.lang.Throwable arg0);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface CallInterface {
    long getClientStateId();
    void markCallCoordinated(boolean arg0);
    UserGroupInformationInterface getUserGroupInformation();
    void setDeferredResponse(org.apache.hadoop.io.Writable arg0);
    java.lang.String getDetailedMetricsName();
    java.lang.String getProtocol();
    void setPriorityLevel(int arg0);
    boolean isOpen();
    void doResponse(java.lang.Throwable arg0, org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto arg1) throws java.io.IOException;
    void sendResponse() throws java.io.IOException;
    long getTimestampNanos();
    void abortResponse(java.lang.Throwable arg0) throws java.io.IOException;
    java.lang.Void run() throws java.lang.Exception;
    java.lang.String getHostAddress();
    java.lang.String toString();
    void setClientStateId(long arg0);
    boolean isResponseDeferred();
    void deferResponse();
    ProcessingDetailsInterface getProcessingDetails();
    java.net.InetAddress getHostInetAddress();
    int getRemotePort();
    void postponeResponse();
    void doResponse(java.lang.Throwable arg0) throws java.io.IOException;
    UserGroupInformationInterface getRemoteUser();
//    T run() throws java.lang.Exception;
    boolean isCallCoordinated();
    void setDeferredError(java.lang.Throwable arg0);
    void setDetailedMetricsName(java.lang.String arg0);
    int getPriorityLevel();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface RpcCallInterface {
    void sendDeferedResponse();
    void deferResponse();
    UserGroupInformationInterface getUserGroupInformation();
    boolean isResponseDeferred();
    void abortResponse(java.lang.Throwable arg0) throws java.io.IOException;
    void setClientStateId(long arg0);
    void setDetailedMetricsName(java.lang.String arg0);
    int getPriorityLevel();
    void populateResponseParamsOnError(java.lang.Throwable arg0, ResponseParamsInterface arg1);
    void setPriorityLevel(int arg0);
    java.lang.Void run() throws java.lang.Exception;
    java.net.InetAddress getHostInetAddress();
    UserGroupInformationInterface getRemoteUser();
//    T run() throws java.lang.Exception;
    void doResponse(java.lang.Throwable arg0) throws java.io.IOException;
    void doResponse(java.lang.Throwable arg0, org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcStatusProto arg1) throws java.io.IOException;
    java.lang.String getProtocol();
    void setResponse(java.nio.ByteBuffer arg0) throws java.io.IOException;
    void sendResponse() throws java.io.IOException;
    void setResponseFields(org.apache.hadoop.io.Writable arg0, ResponseParamsInterface arg1);
    int getRemotePort();
    boolean isOpen();
    boolean isCallCoordinated();
    long getTimestampNanos();
    void setDeferredError(java.lang.Throwable arg0);
    java.lang.String getDetailedMetricsName();
    void setDeferredResponse(org.apache.hadoop.io.Writable arg0);
    java.lang.String toString();
    void markCallCoordinated(boolean arg0);
    java.lang.String getHostAddress();
    void postponeResponse();
    long getClientStateId();
    ProcessingDetailsInterface getProcessingDetails();
}
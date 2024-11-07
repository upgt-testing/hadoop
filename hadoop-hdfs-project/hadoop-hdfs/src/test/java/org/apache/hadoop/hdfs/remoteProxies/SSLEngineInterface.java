package org.apache.hadoop.hdfs.remoteProxies;

public interface SSLEngineInterface {
    void setEnabledProtocols(java.lang.String[] arg0);
    java.lang.String[] getEnabledProtocols();
    java.lang.String getPeerHost();
    SSLEngineResultInterface unwrap(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1) throws javax.net.ssl.SSLException;
    void closeInbound() throws javax.net.ssl.SSLException;
    void setWantClientAuth(boolean arg0);
    boolean isOutboundDone();
    java.lang.String getHandshakeApplicationProtocol();
    boolean getEnableSessionCreation();
    void setSSLParameters(SSLParametersInterface arg0);
    void setUseClientMode(boolean arg0);
    java.lang.String getApplicationProtocol();
    java.util.function.BiFunction<javax.net.ssl.SSLEngine, java.util.List<java.lang.String>, java.lang.String> getHandshakeApplicationProtocolSelector();
    SSLEngineResultInterface wrap(java.nio.ByteBuffer[] arg0, int arg1, int arg2, java.nio.ByteBuffer arg3) throws javax.net.ssl.SSLException;
    void beginHandshake() throws javax.net.ssl.SSLException;
    boolean getWantClientAuth();
    SSLEngineResultInterface unwrap(java.nio.ByteBuffer arg0, java.nio.ByteBuffer[] arg1) throws javax.net.ssl.SSLException;
    javax.net.ssl.SSLSession getHandshakeSession();
    void setEnabledCipherSuites(java.lang.String[] arg0);
    java.lang.Runnable getDelegatedTask();
    java.lang.String[] getSupportedProtocols();
    java.lang.String[] getEnabledCipherSuites();
    javax.net.ssl.SSLEngineResult.HandshakeStatus getHandshakeStatus();
    SSLParametersInterface getSSLParameters();
    SSLEngineResultInterface wrap(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1) throws javax.net.ssl.SSLException;
    SSLEngineResultInterface wrap(java.nio.ByteBuffer[] arg0, java.nio.ByteBuffer arg1) throws javax.net.ssl.SSLException;
    int getPeerPort();
    boolean getNeedClientAuth();
    javax.net.ssl.SSLSession getSession();
    void setEnableSessionCreation(boolean arg0);
    void setNeedClientAuth(boolean arg0);
    boolean getUseClientMode();
    SSLEngineResultInterface unwrap(java.nio.ByteBuffer arg0, java.nio.ByteBuffer[] arg1, int arg2, int arg3) throws javax.net.ssl.SSLException;
    void closeOutbound();
    void setHandshakeApplicationProtocolSelector(java.util.function.BiFunction<javax.net.ssl.SSLEngine, java.util.List<java.lang.String>, java.lang.String> arg0);
    boolean isInboundDone();
    java.lang.String[] getSupportedCipherSuites();
}
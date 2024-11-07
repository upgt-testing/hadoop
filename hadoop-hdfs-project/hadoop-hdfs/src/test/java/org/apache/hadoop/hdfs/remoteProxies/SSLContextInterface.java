package org.apache.hadoop.hdfs.remoteProxies;

public interface SSLContextInterface {
    SSLParametersInterface getSupportedSSLParameters();
    SSLEngineInterface createSSLEngine();
    SSLContextInterface getDefault() throws java.security.NoSuchAlgorithmException;
    SSLSocketFactoryInterface getSocketFactory();
    SSLParametersInterface getDefaultSSLParameters();
    SSLEngineInterface createSSLEngine(java.lang.String arg0, int arg1);
    SSLContextInterface getInstance(java.lang.String arg0, java.security.Provider arg1) throws java.security.NoSuchAlgorithmException;
    java.lang.String getProtocol();
    void setDefault(SSLContextInterface arg0);
    SSLContextInterface getInstance(java.lang.String arg0, java.lang.String arg1) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException;
    javax.net.ssl.SSLSessionContext getClientSessionContext();
    SSLServerSocketFactoryInterface getServerSocketFactory();
    SSLContextInterface getInstance(java.lang.String arg0) throws java.security.NoSuchAlgorithmException;
    void init(javax.net.ssl.KeyManager[] arg0, javax.net.ssl.TrustManager[] arg1, java.security.SecureRandom arg2) throws java.security.KeyManagementException;
    javax.net.ssl.SSLSessionContext getServerSessionContext();
    java.security.Provider getProvider();
}
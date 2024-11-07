package org.apache.hadoop.hdfs.remoteProxies;

public interface TrustManagerFactoryInterface {
    java.lang.String getDefaultAlgorithm();
    TrustManagerFactoryInterface getInstance(java.lang.String arg0) throws java.security.NoSuchAlgorithmException;
    java.lang.String getAlgorithm();
    TrustManagerFactoryInterface getInstance(java.lang.String arg0, java.lang.String arg1) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException;
    javax.net.ssl.TrustManager[] getTrustManagers();
    void init(javax.net.ssl.ManagerFactoryParameters arg0) throws java.security.InvalidAlgorithmParameterException;
    TrustManagerFactoryInterface getInstance(java.lang.String arg0, java.security.Provider arg1) throws java.security.NoSuchAlgorithmException;
    void init(java.security.KeyStore arg0) throws java.security.KeyStoreException;
    java.security.Provider getProvider();
}
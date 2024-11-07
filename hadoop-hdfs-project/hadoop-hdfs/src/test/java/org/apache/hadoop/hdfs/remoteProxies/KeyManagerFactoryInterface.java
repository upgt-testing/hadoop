package org.apache.hadoop.hdfs.remoteProxies;

public interface KeyManagerFactoryInterface {
    void init(javax.net.ssl.ManagerFactoryParameters arg0) throws java.security.InvalidAlgorithmParameterException;
    KeyManagerFactoryInterface getInstance(java.lang.String arg0, java.lang.String arg1) throws java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException;
    java.lang.String getAlgorithm();
    javax.net.ssl.KeyManager[] getKeyManagers();
    KeyManagerFactoryInterface getInstance(java.lang.String arg0, java.security.Provider arg1) throws java.security.NoSuchAlgorithmException;
    void init(java.security.KeyStore arg0, char[] arg1) throws java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.UnrecoverableKeyException;
    java.lang.String getDefaultAlgorithm();
    java.security.Provider getProvider();
    KeyManagerFactoryInterface getInstance(java.lang.String arg0) throws java.security.NoSuchAlgorithmException;
}
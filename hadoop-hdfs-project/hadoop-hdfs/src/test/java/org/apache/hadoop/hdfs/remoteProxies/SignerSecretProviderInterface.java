package org.apache.hadoop.hdfs.remoteProxies;

public interface SignerSecretProviderInterface {
    void init(java.util.Properties arg0, javax.servlet.ServletContext arg1, long arg2) throws java.lang.Exception;
    byte[] getCurrentSecret();
    byte[][] getAllSecrets();
    void destroy();
}
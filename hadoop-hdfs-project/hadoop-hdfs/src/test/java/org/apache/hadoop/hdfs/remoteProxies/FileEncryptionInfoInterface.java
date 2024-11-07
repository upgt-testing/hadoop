package org.apache.hadoop.hdfs.remoteProxies;

public interface FileEncryptionInfoInterface {
    org.apache.hadoop.crypto.CryptoProtocolVersion getCryptoProtocolVersion();
    org.apache.hadoop.crypto.CipherSuite getCipherSuite();
    java.lang.String toStringStable();
    java.lang.String getKeyName();
    byte[] getIV();
    java.lang.String getEzKeyVersionName();
    byte[] getEncryptedDataEncryptionKey();
    java.lang.String toString();
}
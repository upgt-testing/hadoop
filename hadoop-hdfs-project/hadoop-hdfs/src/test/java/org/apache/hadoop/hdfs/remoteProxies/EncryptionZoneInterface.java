package org.apache.hadoop.hdfs.remoteProxies;

public interface EncryptionZoneInterface {
    int hashCode();
    java.lang.String getKeyName();
    org.apache.hadoop.crypto.CryptoProtocolVersion getVersion();
    java.lang.String getPath();
    org.apache.hadoop.crypto.CipherSuite getSuite();
    boolean equals(java.lang.Object arg0);
    long getId();
    java.lang.String toString();
}
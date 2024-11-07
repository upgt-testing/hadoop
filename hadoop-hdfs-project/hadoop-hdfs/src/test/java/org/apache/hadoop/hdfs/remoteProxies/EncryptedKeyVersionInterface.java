package org.apache.hadoop.hdfs.remoteProxies;

public interface EncryptedKeyVersionInterface {
    byte[] getEncryptedKeyIv();
    java.lang.String getEncryptionKeyVersionName();
    EncryptedKeyVersionInterface createForDecryption(java.lang.String arg0, java.lang.String arg1, byte[] arg2, byte[] arg3);
    java.lang.String getEncryptionKeyName();
    byte[] deriveIV(byte[] arg0);
    KeyVersionInterface getEncryptedKeyVersion();
}
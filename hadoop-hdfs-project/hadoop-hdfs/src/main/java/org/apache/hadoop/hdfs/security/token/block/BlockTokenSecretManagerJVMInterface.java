package org.apache.hadoop.hdfs.security.token.block;

public interface BlockTokenSecretManagerJVMInterface {
    void setTokenLifetime(long tokenLifetime);
    void setKeyUpdateIntervalForTesting(long millis);
    void clearAllKeysForTesting();
    boolean hasKey(int keyId);
}

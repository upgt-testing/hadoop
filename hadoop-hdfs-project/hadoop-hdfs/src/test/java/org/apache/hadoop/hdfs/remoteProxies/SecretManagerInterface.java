package org.apache.hadoop.hdfs.remoteProxies;

public interface SecretManagerInterface<T> {
    javax.crypto.SecretKey createSecretKey(byte[] arg0);
    T createIdentifier();
    byte[] retriableRetrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken, org.apache.hadoop.ipc.StandbyException, org.apache.hadoop.ipc.RetriableException, java.io.IOException;
    void checkAvailableForRead() throws org.apache.hadoop.ipc.StandbyException;
    byte[] createPassword(T arg0);
    byte[] createPassword(byte[] arg0, javax.crypto.SecretKey arg1);
    byte[] retrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    javax.crypto.SecretKey generateSecret();
}
package org.apache.hadoop.security.token;

public interface SecretManagerJVMInterface<T> {

    byte[] retriableRetrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken, org.apache.hadoop.ipc.StandbyException, org.apache.hadoop.ipc.RetriableException, java.io.IOException;

    byte[] retrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;

    T createIdentifier();

    void checkAvailableForRead() throws org.apache.hadoop.ipc.StandbyException;
}

package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockTokenSecretManagerInterface {
    DataEncryptionKeyInterface generateDataEncryptionKey();
    javax.crypto.SecretKey generateSecret();
    javax.crypto.SecretKey createSecretKey(byte[] arg0);
    int getSerialNoForTesting();
    void setSerialNo(int arg0);
    void addKeys(ExportedBlockKeysInterface arg0) throws java.io.IOException;
    boolean isExpired(long arg0);
    void setBlockPoolId(java.lang.String arg0);
    BlockKeyInterface getCurrentKey();
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    boolean updateKeys() throws java.io.IOException;
    byte[] retrieveDataEncryptionKey(int arg0, byte[] arg1) throws org.apache.hadoop.hdfs.protocol.datatransfer.InvalidEncryptionKeyException;
    void removeExpiredKeys();
    boolean isTokenExpired(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0) throws java.io.IOException;
    void setTokenLifetime(long arg0);
    void checkAccess(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void checkAccess(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4, java.lang.String[] arg5) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void generateKeys();
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> generateToken(java.lang.String arg0, ExtendedBlockInterface arg1, java.util.EnumSet<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode> arg2, org.apache.hadoop.fs.StorageType[] arg3, java.lang.String[] arg4);
    void clearAllKeysForTesting();
    boolean hasKey(int arg0);
    byte[] retrievePassword(BlockTokenIdentifierInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    byte[] createPassword(byte[] arg0, javax.crypto.SecretKey arg1);
    BlockTokenIdentifierInterface createIdentifier();
    void setKeyUpdateIntervalForTesting(long arg0);
//    T createIdentifier();
    <T> void checkAccess(T[] arg0, T[] arg1, java.lang.String arg2) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    boolean updateKeys(long arg0) throws java.io.IOException;
    ExportedBlockKeysInterface exportKeys();
    void checkAvailableForRead() throws org.apache.hadoop.ipc.StandbyException;
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4, java.lang.String[] arg5) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    byte[] createPassword(BlockTokenIdentifierInterface arg0);
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> generateToken(ExtendedBlockInterface arg0, java.util.EnumSet<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode> arg1, org.apache.hadoop.fs.StorageType[] arg2, java.lang.String[] arg3) throws java.io.IOException;
//    byte[] retriableRetrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken, org.apache.hadoop.ipc.StandbyException, org.apache.hadoop.ipc.RetriableException, java.io.IOException;
}
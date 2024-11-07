package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockPoolTokenSecretManagerInterface {
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4, java.lang.String[] arg5) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    byte[] retrievePassword(BlockTokenIdentifierInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void checkAvailableForRead() throws org.apache.hadoop.ipc.StandbyException;
    void checkAccess(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    byte[] retrieveDataEncryptionKey(int arg0, java.lang.String arg1, byte[] arg2) throws java.io.IOException;
    byte[] createPassword(BlockTokenIdentifierInterface arg0);
    void checkAccess(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4, java.lang.String[] arg5) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void addKeys(java.lang.String arg0, ExportedBlockKeysInterface arg1) throws java.io.IOException;
    BlockTokenSecretManagerInterface get(java.lang.String arg0);
    boolean isBlockPoolRegistered(java.lang.String arg0);
    byte[] createPassword(byte[] arg0, javax.crypto.SecretKey arg1);
    void clearAllKeysForTesting();
    BlockTokenIdentifierInterface createIdentifier();
//    byte[] retriableRetrievePassword(T arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken, org.apache.hadoop.ipc.StandbyException, org.apache.hadoop.ipc.RetriableException, java.io.IOException;
    javax.crypto.SecretKey generateSecret();
    DataEncryptionKeyInterface generateDataEncryptionKey(java.lang.String arg0);
//    T createIdentifier();
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    void checkAccess(BlockTokenIdentifierInterface arg0, java.lang.String arg1, ExtendedBlockInterface arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3, org.apache.hadoop.fs.StorageType[] arg4) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
    javax.crypto.SecretKey createSecretKey(byte[] arg0);
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> generateToken(ExtendedBlockInterface arg0, java.util.EnumSet<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode> arg1, org.apache.hadoop.fs.StorageType[] arg2, java.lang.String[] arg3) throws java.io.IOException;
    void addBlockPool(java.lang.String arg0, BlockTokenSecretManagerInterface arg1);
}
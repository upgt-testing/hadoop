package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.block.ExportedBlockKeys;
import org.apache.hadoop.security.token.Token;

public interface BlockTokenSecretManagerInterface {

    void setSerialNo(int nextNo);

    void setBlockPoolId(String blockPoolId);

    ExportedBlockKeysInterface exportKeys();

    void addKeys(ExportedBlockKeys exportedKeys);

    boolean updateKeys(long updateTime);

    TokenInterface generateToken(ExtendedBlockInterface block, EnumSet<BlockTokenIdentifier.AccessMode> modes, StorageType[] storageTypes, String[] storageIds);

    Token<BlockTokenIdentifier> generateToken(String userId, ExtendedBlock block, EnumSet<BlockTokenIdentifier.AccessMode> modes, StorageType[] storageTypes, String[] storageIds);

    void checkAccess(BlockTokenIdentifierInterface id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes, String[] storageIds);

    void checkAccess(BlockTokenIdentifier id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes);

    void checkAccess(BlockTokenIdentifier id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode);

    void checkAccess(Token<BlockTokenIdentifier> token, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes, String[] storageIds);

    void checkAccess(Token<BlockTokenIdentifier> token, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode);

    void setTokenLifetime(long tokenLifetime);

    BlockTokenIdentifierInterface createIdentifier();

    byte[] retrievePassword(BlockTokenIdentifier identifier);

    DataEncryptionKeyInterface generateDataEncryptionKey();

    byte[] retrieveDataEncryptionKey(int keyId, byte[] nonce);

    BlockKeyInterface getCurrentKey();

    void setKeyUpdateIntervalForTesting(long millis);

    void clearAllKeysForTesting();

    boolean hasKey(int keyId);

    int getSerialNoForTesting();
}

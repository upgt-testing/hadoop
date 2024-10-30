package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.block.ExportedBlockKeys;
import org.apache.hadoop.security.token.Token;
//import org.apache.hadoop.yarn.api.records.Token;

public interface BlockPoolTokenSecretManagerInterface {

    void addBlockPool(String bpid, BlockTokenSecretManagerInterface secretMgr);

    BlockTokenSecretManagerInterface get(String bpid);

    boolean isBlockPoolRegistered(String bpid);

    BlockTokenIdentifierInterface createIdentifier();

    byte[] createPassword(BlockTokenIdentifier identifier);

    byte[] retrievePassword(BlockTokenIdentifier identifier);

    void checkAccess(BlockTokenIdentifier id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes, String[] storageIds);

    void checkAccess(BlockTokenIdentifier id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes);

    void checkAccess(BlockTokenIdentifier id, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode);

    void checkAccess(Token<BlockTokenIdentifier> token, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode);

    void checkAccess(Token<BlockTokenIdentifier> token, String userId, ExtendedBlock block, BlockTokenIdentifier.AccessMode mode, StorageType[] storageTypes, String[] storageIds);

    void addKeys(String bpid, ExportedBlockKeys exportedKeys);

    Token<BlockTokenIdentifier> generateToken(ExtendedBlock b, EnumSet<BlockTokenIdentifier.AccessMode> of, StorageType[] storageTypes, String[] storageIds);

    void clearAllKeysForTesting();

    DataEncryptionKeyInterface generateDataEncryptionKey(String blockPoolId);

    byte[] retrieveDataEncryptionKey(int keyId, String blockPoolId, byte[] nonce);
}

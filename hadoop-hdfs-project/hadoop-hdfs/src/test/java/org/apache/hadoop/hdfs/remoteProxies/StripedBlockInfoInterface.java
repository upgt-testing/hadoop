package org.apache.hadoop.hdfs.remoteProxies;

public interface StripedBlockInfoInterface {
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier>[] getBlockTokens();
    byte[] getBlockIndices();
    DatanodeInfoInterface[] getDatanodes();
    ExtendedBlockInterface getBlock();
    ErasureCodingPolicyInterface getErasureCodingPolicy();
}
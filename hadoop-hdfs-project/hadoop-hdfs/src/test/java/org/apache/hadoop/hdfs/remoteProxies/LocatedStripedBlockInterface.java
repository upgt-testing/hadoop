package org.apache.hadoop.hdfs.remoteProxies;

public interface LocatedStripedBlockInterface {
    DatanodeInfoWithStorageInterface[] getLocations();
    long getStartOffset();
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> getBlockToken();
    void addCachedLoc(DatanodeInfoInterface arg0);
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier>[] getBlockTokens();
    void setBlockTokens(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier>[] arg0);
    long getBlockSize();
    java.lang.String toString();
    void setStartOffset(long arg0);
    ExtendedBlockInterface getBlock();
    void setCorrupt(boolean arg0);
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    void setBlockToken(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0);
    org.apache.hadoop.hdfs.protocol.BlockType getBlockType();
    DatanodeInfoInterface[] getCachedLocations();
    byte[] getBlockIndices();
    void moveProvidedToEnd(int arg0);
    void updateCachedStorageInfo();
    boolean isCorrupt();
    boolean isStriped();
    java.lang.String[] getStorageIDs();
    DatanodeInfoWithStorageInterface[] convert(DatanodeInfoInterface[] arg0, java.lang.String[] arg1, org.apache.hadoop.fs.StorageType[] arg2);
}
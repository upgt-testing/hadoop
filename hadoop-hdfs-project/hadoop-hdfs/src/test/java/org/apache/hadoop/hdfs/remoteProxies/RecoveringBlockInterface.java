package org.apache.hadoop.hdfs.remoteProxies;

public interface RecoveringBlockInterface {
    ExtendedBlockInterface getBlock();
    boolean isCorrupt();
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> getBlockToken();
    long getBlockSize();
    void setBlockToken(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0);
    java.lang.String toString();
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    org.apache.hadoop.hdfs.protocol.BlockType getBlockType();
    void setCorrupt(boolean arg0);
    DatanodeInfoWithStorageInterface[] convert(DatanodeInfoInterface[] arg0, java.lang.String[] arg1, org.apache.hadoop.fs.StorageType[] arg2);
    java.lang.String[] getStorageIDs();
    void addCachedLoc(DatanodeInfoInterface arg0);
    DatanodeInfoInterface[] getCachedLocations();
    DatanodeInfoWithStorageInterface[] getLocations();
    long getNewGenerationStamp();
    void moveProvidedToEnd(int arg0);
    BlockInterface getNewBlock();
    void setStartOffset(long arg0);
    void updateCachedStorageInfo();
    long getStartOffset();
    boolean isStriped();
}
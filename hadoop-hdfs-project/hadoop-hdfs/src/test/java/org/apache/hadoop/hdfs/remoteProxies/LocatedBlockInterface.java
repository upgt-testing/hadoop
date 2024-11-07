package org.apache.hadoop.hdfs.remoteProxies;

public interface LocatedBlockInterface {
    boolean isCorrupt();
    long getStartOffset();
    void moveProvidedToEnd(int arg0);
    ExtendedBlockInterface getBlock();
    DatanodeInfoWithStorageInterface[] getLocations();
    TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> getBlockToken();
    org.apache.hadoop.hdfs.protocol.BlockType getBlockType();
    void setStartOffset(long arg0);
    DatanodeInfoWithStorageInterface[] convert(DatanodeInfoInterface[] arg0, java.lang.String[] arg1, org.apache.hadoop.fs.StorageType[] arg2);
    void setCorrupt(boolean arg0);
    void setBlockToken(TokenInterface<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier> arg0);
    long getBlockSize();
    DatanodeInfoInterface[] getCachedLocations();
    void updateCachedStorageInfo();
    java.lang.String[] getStorageIDs();
    java.lang.String toString();
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    void addCachedLoc(DatanodeInfoInterface arg0);
    boolean isStriped();
}
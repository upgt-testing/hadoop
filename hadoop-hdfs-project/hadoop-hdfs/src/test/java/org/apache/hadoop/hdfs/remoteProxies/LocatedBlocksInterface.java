package org.apache.hadoop.hdfs.remoteProxies;

public interface LocatedBlocksInterface {
    boolean isLastBlockComplete();
    int findBlock(long arg0);
    int getInsertIndex(int arg0);
    boolean isUnderConstruction();
    java.lang.String toString();
    long getFileLength();
    int locatedBlockCount();
    java.util.List<LocatedBlockInterface> getLocatedBlocks();
    LocatedBlockInterface get(int arg0);
    void insertRange(int arg0, java.util.List<org.apache.hadoop.hdfs.protocol.LocatedBlock> arg1);
    FileEncryptionInfoInterface getFileEncryptionInfo();
    LocatedBlockInterface getLastLocatedBlock();
    ErasureCodingPolicyInterface getErasureCodingPolicy();
}
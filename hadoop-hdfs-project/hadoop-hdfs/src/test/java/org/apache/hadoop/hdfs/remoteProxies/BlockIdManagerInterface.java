package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockIdManagerInterface {
    boolean isStripedBlock(BlockInterface arg0);
    void clear();
    SequentialBlockIdGeneratorInterface getBlockIdGenerator();
    long getGenerationStamp();
    void applyImpendingGenerationStamp();
    long getLastAllocatedContiguousBlockId();
    long nextBlockId(org.apache.hadoop.hdfs.protocol.BlockType arg0);
    long getGenerationStampAtblockIdSwitch();
    long getLastAllocatedStripedBlockId();
    long getLegacyGenerationStamp();
    void setImpendingGenerationStamp(long arg0);
    void setLastAllocatedStripedBlockId(long arg0);
    void setLegacyGenerationStamp(long arg0);
    SequentialBlockGroupIdGeneratorInterface getBlockGroupIdGenerator();
    boolean isLegacyBlock(BlockInterface arg0);
    boolean isLegacyBlock(org.apache.hadoop.hdfs.protocol.Block arg0);
    void setLastAllocatedContiguousBlockId(long arg0);
    long getNextGenerationStamp();
    boolean isGenStampInFuture(BlockInterface arg0);
    boolean isStripedBlockID(long arg0);
    void setLegacyGenerationStampLimit(long arg0);
    byte getBlockIndex(BlockInterface arg0);
    long nextGenerationStamp(boolean arg0) throws java.io.IOException;
    void setGenerationStamp(long arg0);
    long getNextLegacyGenerationStamp() throws java.io.IOException;
    long upgradeLegacyGenerationStamp();
    long convertToStripedID(long arg0);
    long getLegacyGenerationStampLimit();
    void setGenerationStampIfGreater(long arg0);
    long getImpendingGenerationStamp();
}
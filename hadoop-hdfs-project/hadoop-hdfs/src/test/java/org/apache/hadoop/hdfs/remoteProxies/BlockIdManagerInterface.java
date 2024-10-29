package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.Block;

public interface BlockIdManagerInterface {

    long upgradeLegacyGenerationStamp();

    void setLegacyGenerationStampLimit(long stamp);

    long getGenerationStampAtblockIdSwitch();

    void setLastAllocatedContiguousBlockId(long blockId);

    long getLastAllocatedContiguousBlockId();

    void setLastAllocatedStripedBlockId(long blockId);

    long getLastAllocatedStripedBlockId();

    void setLegacyGenerationStamp(long stamp);

    long getLegacyGenerationStamp();

    void setGenerationStamp(long stamp);

    void setImpendingGenerationStamp(long stamp);

    void applyImpendingGenerationStamp();

    long getImpendingGenerationStamp();

    void setGenerationStampIfGreater(long stamp);

    long getGenerationStamp();

    long getLegacyGenerationStampLimit();

    boolean isStripedBlock(Block block);
}

package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BlockManagerJVMInterface {
    long getLowRedundancyBlocksCount();
    long getExcessBlocksCount();
    long getPendingDeletionBlocksCount();
    int getPendingDataNodeMessageCount();
    void metaSave(PrintWriter out);;
    int computeBlockReconstructionWork(int blocksToProcess);
    int computeInvalidateWork(int nodesToProcess);
    int computeDatanodeWork();
    void setInitializedReplQueues(boolean v);
    long getCorruptBlocks();
    BlockTokenSecretManagerJVMInterface getBlockTokenSecretManager();
    DatanodeManagerJVMInterface getDatanodeManager();
    int getUnderReplicatedNotMissingBlocks();
    void updateState();
    long getCorruptECBlockGroups();
    void clear();
    BlockIdManagerJVMInterface getBlockIdManager();
    int getMaxReplicationStreams();
    int getReplicationStreamsHardLimit();
    int getBlocksReplWorkMultiplier();
    //int getReconstructionPendingTimeout();
}

package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;
import org.apache.hadoop.util.DeamonJVMInterface;

import java.io.PrintWriter;
import java.util.Queue;

public interface BlockManagerJVMInterface {
    DeamonJVMInterface getRedundancyThread();
    void enableRMTerminationForTesting();
    int getTotalBlocks();
    boolean isPopulatingReplQueues();
    void rescanPostponedMisreplicatedBlocks();
    Queue<?> getMarkedDeleteQueue();
    BlockPlacementPolicyJVMInterface getBlockPlacementPolicy();
    long getMissingBlocksCount();
    int numOfUnderReplicatedBlocks();
    long getPendingReconstructionBlocksCount();
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

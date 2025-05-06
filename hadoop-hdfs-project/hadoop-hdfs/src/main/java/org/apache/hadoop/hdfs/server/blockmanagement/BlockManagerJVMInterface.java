package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;
import org.apache.hadoop.util.DeamonJVMInterface;

import java.io.PrintWriter;

public interface BlockManagerJVMInterface {
    int computeReplicationWork(int blocksToProcess);
    long getPendingReplicationBlocksCount();
    long getUnderReplicatedBlocksCount();
    void enableRMTerminationForTesting();
    int getTotalBlocks();
    boolean isPopulatingReplQueues();
    void rescanPostponedMisreplicatedBlocks();
    BlockPlacementPolicyJVMInterface getBlockPlacementPolicy();
    long getMissingBlocksCount();
    int numOfUnderReplicatedBlocks();
    long getExcessBlocksCount();
    long getPendingDeletionBlocksCount();
    int getPendingDataNodeMessageCount();
    void metaSave(PrintWriter out);;
    int computeInvalidateWork(int nodesToProcess);
    int computeDatanodeWork();
    void setInitializedReplQueues(boolean v);
    BlockTokenSecretManagerJVMInterface getBlockTokenSecretManager();
    DatanodeManagerJVMInterface getDatanodeManager();
    int getUnderReplicatedNotMissingBlocks();
    void updateState();
    void clear();
    BlockIdManagerJVMInterface getBlockIdManager();
    int getMaxReplicationStreams();
    //int getReconstructionPendingTimeout();
}

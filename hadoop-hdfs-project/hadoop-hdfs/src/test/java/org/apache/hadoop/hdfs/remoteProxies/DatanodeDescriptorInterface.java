package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo;
import org.apache.hadoop.hdfs.server.protocol.BlockECReconstructionCommand;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.DatanodeID;

public interface DatanodeDescriptorInterface {

    DatanodeDescriptor.CachedBlocksList getPendingCached();

    DatanodeDescriptor.CachedBlocksList getCached();

    DatanodeDescriptor.CachedBlocksList getPendingUncached();

    boolean isAlive();

    void setAlive(boolean isAlive);

    boolean needKeyUpdate();

    void setNeedKeyUpdate(boolean needKeyUpdate);

    DatanodeDescriptor.LeavingServiceStatus getLeavingServiceStatus();

    boolean isHeartbeatedSinceRegistration();

    DatanodeStorageInfoInterface getStorageInfo(String storageID);

    DatanodeStorageInfoInterface[] getStorageInfos();

    EnumSet<StorageType> getStorageTypes();

    StorageReportInterface[] getStorageReports();

    void resetBlocks();

    void clearBlockQueues();

    int numBlocks();

    void incrementPendingReplicationWithoutTargets();

    void decrementPendingReplicationWithoutTargets();

    void addBlockToBeReplicated(Block block, DatanodeStorageInfo[] targets);

    int getNumberOfBlocksToBeErasureCoded();

    int getNumberOfReplicateBlocks();

    List<BlockECReconstructionCommand.BlockECReconstructionInfo> getErasureCodeCommand(int maxTransfers);

    BlockInfoInterface[] getLeaseRecoveryCommand(int maxTransfers);

    BlockInterface[] getInvalidateBlocks(int maxblocks);

    boolean containsInvalidateBlock(Block block);

    DatanodeStorageInfoInterface chooseStorage4Block(StorageType t, long blockSize);

    int getBlocksScheduled(StorageType t);

    int getBlocksScheduled();

    int hashCode();

    boolean equals(Object obj);

    void setDisallowed(boolean flag);

    boolean isDisallowed();

    int getVolumeFailures();

    VolumeFailureSummaryInterface getVolumeFailureSummary();

    void updateRegInfo(DatanodeID nodeReg);

    long getBalancerBandwidth();

    void setBalancerBandwidth(long bandwidth);

    String dumpDatanode();

    long getLastCachingDirectiveSentTimeMs();

    void setLastCachingDirectiveSentTimeMs(long time);

    boolean checkBlockReportReceived();

    void setForceRegistration(boolean force);

    boolean isRegistered();

    boolean hasStorageType(StorageType type);
}

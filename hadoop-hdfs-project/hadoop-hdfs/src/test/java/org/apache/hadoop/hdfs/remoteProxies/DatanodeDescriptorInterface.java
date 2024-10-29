package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.DatanodeID;

public interface DatanodeDescriptorInterface {

    CachedBlocksList getPendingCached();

    CachedBlocksList getCached();

    CachedBlocksList getPendingUncached();

    boolean isAlive();

    void setAlive(boolean isAlive);

    boolean needKeyUpdate();

    void setNeedKeyUpdate(boolean needKeyUpdate);

    LeavingServiceStatus getLeavingServiceStatus();

    boolean isHeartbeatedSinceRegistration();

    DatanodeStorageInfo getStorageInfo(String storageID);

    DatanodeStorageInfo[] getStorageInfos();

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

    List<BlockECReconstructionInfo> getErasureCodeCommand(int maxTransfers);

    BlockInfo[] getLeaseRecoveryCommand(int maxTransfers);

    Block[] getInvalidateBlocks(int maxblocks);

    boolean containsInvalidateBlock(Block block);

    DatanodeStorageInfo chooseStorage4Block(StorageType t, long blockSize);

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

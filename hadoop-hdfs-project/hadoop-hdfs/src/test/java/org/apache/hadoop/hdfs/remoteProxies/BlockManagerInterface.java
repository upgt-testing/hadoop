package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.AddBlockFlag;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.fs.FileEncryptionInfo;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.block.ExportedBlockKeys;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.security.token.block.DataEncryptionKey;
import org.apache.hadoop.hdfs.server.blockmanagement.*;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.protocol.BlocksWithLocations;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.hdfs.protocol.BlockType;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;
import org.apache.hadoop.hdfs.server.protocol.BlockReportContext;
import org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks;

public interface BlockManagerInterface {

    long getPendingReconstructionBlocksCount();

    long getLowRedundancyBlocksCount();

    long getCorruptReplicaBlocksCount();

    long getScheduledReplicationBlocksCount();

    long getPendingDeletionBlocksCount();

    long getStartupDelayBlockDeletionInMs();

    long getExcessBlocksCount();

    long getPostponedMisreplicatedBlocksCount();

    int getPendingDataNodeMessageCount();

    long getNumTimedOutPendingReconstructions();

    long getLowRedundancyBlocks();

    long getCorruptBlocks();

    long getMissingBlocks();

    long getMissingReplicationOneBlocks();

    long getPendingDeletionReplicatedBlocks();

    long getTotalReplicatedBlocks();

    long getLowRedundancyECBlockGroups();

    long getCorruptECBlockGroups();

    long getMissingECBlockGroups();

    long getPendingDeletionECBlocks();

    long getTotalECBlockGroups();

    BlockStoragePolicy getStoragePolicy(String policyName);

    BlockStoragePolicy getStoragePolicy(byte policyId);

    BlockStoragePolicy[] getStoragePolicies();

    void setBlockPoolId(String blockPoolId);

    String getBlockPoolId();

    BlockStoragePolicySuiteInterface getStoragePolicySuite();

    BlockTokenSecretManagerInterface getBlockTokenSecretManager();

    void activate(Configuration conf, long blockTotal);

    void close();

    DatanodeManagerInterface getDatanodeManager();

    BlockPlacementPolicyInterface getBlockPlacementPolicy();

    BlockPlacementPolicyInterface getStriptedBlockPlacementPolicy();

    void refreshBlockPlacementPolicy(Configuration conf);

    void metaSave(PrintWriter out);

    int getMaxReplicationStreams();

    void setMaxReplicationStreams(int newVal);

    int getReplicationStreamsHardLimit();

    void setReplicationStreamsHardLimit(int newVal);

    int getBlocksReplWorkMultiplier();

    void setBlocksReplWorkMultiplier(int newVal);

    int getDefaultStorageNum(BlockInfo block);

    short getMinReplication();

    short getMinStorageNum(BlockInfo block);

    short getMinReplicationToBeInMaintenance();

    boolean hasMinStorage(BlockInfo block);

    boolean hasMinStorage(BlockInfo block, int liveNum);

    boolean commitOrCompleteLastBlock(BlockCollectionInterface bc, Block commitBlock, INodesInPathInterface iip);

    void addExpectedReplicasToPending(BlockInfo blk);

    void forceCompleteBlock(BlockInfo block);

    LocatedBlockInterface convertLastBlockToUnderConstruction(BlockCollection bc, long bytesToRemove);

    LocatedBlocksInterface createLocatedBlocks(BlockInfo[] blocks, long fileSizeExcludeBlocksUnderConstruction, boolean isFileUnderConstruction, long offset, long length, boolean needBlockToken, boolean inSnapshot, FileEncryptionInfo feInfo, ErasureCodingPolicy ecPolicy);

    ExportedBlockKeys getBlockKeys();

    void setBlockToken(LocatedBlock b, BlockTokenIdentifier.AccessMode mode);

    DataEncryptionKey generateDataEncryptionKey();

    short adjustReplication(short replication);

    void verifyReplication(String src, short replication, String clientName);

    boolean isSufficientlyReplicated(BlockInfo b);

    BlocksWithLocations getBlocksWithLocations(DatanodeID datanode, long size, long minBlockSize);

    void findAndMarkBlockAsCorrupt(ExtendedBlock blk, DatanodeInfo dn, String storageID, String reason);

    void setPostponeBlocksFromFuture(boolean postpone);

    int getUnderReplicatedNotMissingBlocks();

    //DatanodeStorageInfoInterface[] chooseTarget4WebHDFS(String src, DatanodeDescriptor clientnode, Set<Node> excludes, long blocksize);

    DatanodeStorageInfoInterface[] chooseTarget4AdditionalDatanode(String src, int numAdditionalNodes, Node clientnode, List<DatanodeStorageInfo> chosen, Set<Node> excludes, long blocksize, byte storagePolicyID, BlockType blockType);

    DatanodeStorageInfoInterface[] chooseTarget4NewBlock(String src, int numOfReplicas, Node client, Set<Node> excludedNodes, long blocksize, List<String> favoredNodes, byte storagePolicyID, BlockType blockType, ErasureCodingPolicy ecPolicy, EnumSet<AddBlockFlag> flags);

    long requestBlockReportLeaseId(DatanodeRegistration nodeReg);

    void registerDatanode(DatanodeRegistration nodeReg);

    void setBlockTotal(long total);

    boolean isInSafeMode();

    String getSafeModeTip();

    boolean leaveSafeMode(boolean force);

    void checkSafeMode();

    long getBytesInFuture();

    long getBytesInFutureReplicatedBlocks();

    long getBytesInFutureECBlockGroups();

    void removeBlocksAndUpdateSafemodeTotal(INode.BlocksMapUpdateInfo blocks);

    long getProvidedCapacity();

    boolean checkBlockReportLease(BlockReportContextInterface context, DatanodeID nodeID);

    boolean processReport(DatanodeID nodeID, DatanodeStorage storage, BlockListAsLongsInterface newReport, BlockReportContext context);

    void removeBRLeaseIfNeeded(DatanodeID nodeID, BlockReportContext context);

    void markBlockReplicasAsCorrupt(Block oldBlock, BlockInfo block, long oldGenerationStamp, long oldNumBytes, DatanodeStorageInfo[] newStorages);

    void processQueuedMessagesForBlock(Block b);

    void processAllPendingDNMessages();

    void processMisReplicatedBlocks();

    void stopReconstructionInitializer();

    double getReconstructionQueuesInitProgress();

    boolean hasNonEcBlockUsingStripedID();

    int processMisReplicatedBlocks(List<BlockInfo> blocks);

    void setReplication(short oldRepl, short newRepl, BlockInfo b);

    void removeStoredBlock(BlockInfo storedBlock, DatanodeDescriptor node);

    void addBlock(DatanodeStorageInfo storageInfo, Block block, String delHint);

    void processIncrementalBlockReport(DatanodeID nodeID, StorageReceivedDeletedBlocks srdb);

    NumberReplicasInterface countNodes(BlockInfo b);

    boolean isExcess(DatanodeDescriptor dn, BlockInfo blk);

    int getActiveBlockCount();

    DatanodeStorageInfo[] getStorages(BlockInfo block);

    Iterable<DatanodeStorageInfo> getStorages(Block block);

    int getTotalBlocks();

    void removeBlock(BlockInfo block);

    BlockInfo getStoredBlock(Block block);

    void updateLastBlock(BlockInfo lastBlock, ExtendedBlock newBlock);

    void checkRedundancy(BlockCollection bc);

    boolean containsInvalidateBlock(DatanodeInfo dn, Block block);

    short getExpectedLiveRedundancyNum(BlockInfo block, NumberReplicas numberReplicas);

    short getExpectedRedundancyNum(BlockInfo block);

    long getMissingBlocksCount();

    long getMissingReplOneBlocksCount();

    long getHighestPriorityReplicatedBlockCount();

    long getHighestPriorityECBlockCount();

    BlockInfo addBlockCollection(BlockInfo block, BlockCollection bc);

    BlockInfo addBlockCollectionWithCheck(BlockInfo block, BlockCollection bc);

    int numCorruptReplicas(Block block);

    void removeBlockFromMap(BlockInfo block);

    int getCapacity();

    Iterator<BlockInfo> getCorruptReplicaBlockIterator();

    Collection<DatanodeDescriptor> getCorruptReplicas(Block block);

    String getCorruptReason(Block block, DatanodeDescriptor node);

    int numOfUnderReplicatedBlocks();

    long getLastRedundancyMonitorTS();

    void clearQueues();

    void shutdown();

    void clear();

    BlockReportLeaseManagerInterface getBlockReportLeaseManager();

    //Map<StorageType, StorageTypeStats> getStorageTypeStats();

    void initializeReplQueues();

    boolean isPopulatingReplQueues();

    void setInitializedReplQueues(boolean v);

    boolean shouldPopulateReplQueues();

    void enqueueBlockOp(Runnable action);

    //T runBlockOp(Callable<T> action);

    void successfulBlockRecovery(BlockInfo block);

    boolean addBlockRecoveryAttempt(BlockInfo b);

    void flushBlockOps();

    int getBlockOpQueueLength();

    BlockIdManagerInterface getBlockIdManager();

    ConcurrentLinkedQueue<List<BlockInfoInterface>> getMarkedDeleteQueue();

    void addBLocksToMarkedDeleteQueue(List<BlockInfo> blockInfos);

    long nextGenerationStamp(boolean legacyBlock);

    boolean isLegacyBlock(Block block);

    long nextBlockId(BlockType blockType);

    void setBlockRecoveryTimeout(long blockRecoveryTimeout);

    ProvidedStorageMapInterface getProvidedStorageMap();

    boolean createSPSManager(Configuration conf, String spsMode);

    void disableSPS();

    StoragePolicySatisfyManagerInterface getSPSManager();

    void setExcludeSlowNodesEnabled(boolean enable);

    boolean getExcludeSlowNodesEnabled(BlockType blockType);
}

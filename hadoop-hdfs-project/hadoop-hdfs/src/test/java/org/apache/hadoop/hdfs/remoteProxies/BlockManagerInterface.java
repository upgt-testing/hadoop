package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;

public interface BlockManagerInterface {
    void processFirstBlockReport(DatanodeStorageInfoInterface arg0, BlockListAsLongsInterface arg1) throws java.io.IOException;
    BlockInterface getBlockOnStorage(BlockInfoInterface arg0, DatanodeStorageInfoInterface arg1);
    short getMinStorageNum(BlockInfoInterface arg0);
    void setBlockRecoveryTimeout(long arg0);
    void setMaxReplicationStreams(int arg0);
    long getPostponedMisreplicatedBlocksCount();
    long nextGenerationStamp(boolean arg0) throws java.io.IOException;
    LocatedBlockInterface convertLastBlockToUnderConstruction(org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg0, long arg1) throws java.io.IOException;
    int invalidateWorkForOneNode(DatanodeInfoInterface arg0);
    java.lang.String getCorruptReason(BlockInterface arg0, DatanodeDescriptorInterface arg1);
    ExportedBlockKeysInterface getBlockKeys();
    org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection getBlockCollection(BlockInfoInterface arg0);
    int getActiveBlockCount();
    boolean validateReconstructionWork(BlockReconstructionWorkInterface arg0);
    StoragePolicySatisfyManagerInterface getSPSManager();
    void removeBlocksAssociatedTo(DatanodeStorageInfoInterface arg0);
    void verifyReplication(java.lang.String arg0, short arg1, java.lang.String arg2) throws java.io.IOException;
    boolean isPopulatingReplQueues();
    long getMissingReplOneBlocksCount();
    long getCorruptReplicaBlocksCount();
    short adjustReplication(short arg0);
    boolean checkBlockReportLease(BlockReportContextInterface arg0, DatanodeIDInterface arg1) throws org.apache.hadoop.hdfs.protocol.UnregisteredNodeException;
    long getMissingReplicationOneBlocks();
    DatanodeStorageInfoInterface[] chooseTarget4AdditionalDatanode(java.lang.String arg0, int arg1, org.apache.hadoop.net.Node arg2, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg3, java.util.Set<org.apache.hadoop.net.Node> arg4, long arg5, byte arg6, org.apache.hadoop.hdfs.protocol.BlockType arg7);
    void addToInvalidates(BlockInterface arg0, DatanodeInfoInterface arg1);
    void addToInvalidates(org.apache.hadoop.hdfs.protocol.Block arg0, DatanodeDescriptorInterface arg1);
    void setBlockPoolId(java.lang.String arg0);
    BlocksWithLocationsInterface getBlocksWithLocations(DatanodeIDInterface arg0, long arg1, long arg2) throws org.apache.hadoop.hdfs.protocol.UnregisteredNodeException;
//    void reportDiff(DatanodeStorageInfoInterface arg0, BlockListAsLongsInterface arg1, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.BlockInfoToAdd> arg2, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo> arg3, java.util.Collection<org.apache.hadoop.hdfs.protocol.Block> arg4, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockToMarkCorrupt> arg5, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.StatefulBlockInfo> arg6);
    int getCapacity();
    BlockStoragePolicyInterface[] getStoragePolicies();
    void countLiveAndDecommissioningReplicas(NumberReplicasInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.NumberReplicas.StoredReplicaState arg1, java.util.BitSet arg2, java.util.BitSet arg3, byte arg4);
    java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> getCorruptReplicas(BlockInterface arg0);
    java.util.concurrent.ConcurrentLinkedQueue<java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo>> getMarkedDeleteQueue();
    int computeReconstructionWorkForBlocks(java.util.List<java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo>> arg0);
    boolean isGenStampInFuture(BlockInterface arg0);
    void removeBlocksAndUpdateSafemodeTotal(BlocksMapUpdateInfoInterface arg0);
    boolean processReport(DatanodeIDInterface arg0, DatanodeStorageInterface arg1, BlockListAsLongsInterface arg2, BlockReportContextInterface arg3) throws java.io.IOException;
    BlockIdManagerInterface getBlockIdManager();
    void removeBlock(BlockInfoInterface arg0);
    long getPendingDeletionBlocksCount();
    long getProvidedCapacity();
    LocatedBlockInterface createLocatedBlock(LocatedBlockBuilderInterface arg0, BlockInfoInterface arg1, long arg2) throws java.io.IOException;
//    void removeStaleReplicas(java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.ReplicaUnderConstruction> arg0, BlockInfoInterface arg1);
    boolean leaveSafeMode(boolean arg0);
    void setInitializedReplQueues(boolean arg0);
    void addToInvalidates(BlockInfoInterface arg0);
    void processExtraRedundancyBlocksOnInService(DatanodeDescriptorInterface arg0);
    NumberReplicasInterface countNodes(BlockInfoInterface arg0, boolean arg1);
    boolean invalidateBlock(BlockToMarkCorruptInterface arg0, DatanodeInfoInterface arg1, NumberReplicasInterface arg2) throws java.io.IOException;
    void addStoredBlockImmediate(BlockInfoInterface arg0, BlockInterface arg1, DatanodeStorageInfoInterface arg2) throws java.io.IOException;
    int getDefaultStorageNum(BlockInfoInterface arg0);
    void processPendingReconstructions();
    BlockStoragePolicyInterface getStoragePolicy(byte arg0);
    boolean processAndHandleReportedBlock(DatanodeStorageInfoInterface arg0, BlockInterface arg1, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg2, DatanodeDescriptorInterface arg3) throws java.io.IOException;
    short getMinReplication();
    BlockStoragePolicyInterface getStoragePolicy(java.lang.String arg0);
    void dumpBlockMeta(BlockInterface arg0, java.io.PrintWriter arg1);
    void removeStoredBlock(BlockInfoInterface arg0, DatanodeDescriptorInterface arg1);
//    BlockInfoInterface processReportedBlock(DatanodeStorageInfoInterface arg0, BlockInterface arg1, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg2, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.BlockInfoToAdd> arg3, java.util.Collection<org.apache.hadoop.hdfs.protocol.Block> arg4, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockToMarkCorrupt> arg5, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.StatefulBlockInfo> arg6);
    DatanodeStorageInfoInterface[] chooseTarget4NewBlock(java.lang.String arg0, int arg1, org.apache.hadoop.net.Node arg2, java.util.Set<org.apache.hadoop.net.Node> arg3, long arg4, java.util.List<java.lang.String> arg5, byte arg6, org.apache.hadoop.hdfs.protocol.BlockType arg7, ErasureCodingPolicyInterface arg8, java.util.EnumSet<org.apache.hadoop.hdfs.AddBlockFlag> arg9) throws java.io.IOException;
    DatanodeStorageInfoInterface[] chooseTarget4NewBlock(java.lang.String arg0, int arg1, NodeInterface arg2, java.util.Set<org.apache.hadoop.net.Node> arg3, long arg4, java.util.List<java.lang.String> arg5, byte arg6, org.apache.hadoop.hdfs.protocol.BlockType arg7, ErasureCodingPolicyInterface arg8, java.util.EnumSet<org.apache.hadoop.hdfs.AddBlockFlag> arg9) throws java.io.IOException;
    void updateLastBlock(BlockInfoInterface arg0, ExtendedBlockInterface arg1);
    boolean isSufficientlyReplicated(BlockInfoInterface arg0);
//    void processQueuedMessages(java.lang.Iterable<org.apache.hadoop.hdfs.server.blockmanagement.PendingDataNodeMessages.ReportedBlockInfo> arg0) throws java.io.IOException;
    void removeBlocksAssociatedTo(DatanodeDescriptorInterface arg0);
    long getStartupDelayBlockDeletionInMs();
    LocatedBlocksInterface createLocatedBlocks(BlockInfoInterface[] arg0, long arg1, boolean arg2, long arg3, long arg4, boolean arg5, boolean arg6, FileEncryptionInfoInterface arg7, ErasureCodingPolicyInterface arg8) throws java.io.IOException;
    void refreshBlockPlacementPolicy(ConfigurationInterface arg0);
    int getExcessSize4Testing(java.lang.String arg0);
    long getMissingECBlockGroups();
    BlockTokenSecretManagerInterface createBlockTokenSecretManager(ConfigurationInterface arg0) throws java.io.IOException;
    DatanodeStorageInfoInterface[] chooseTarget4WebHDFS(java.lang.String arg0, DatanodeDescriptorInterface arg1, java.util.Set<org.apache.hadoop.net.Node> arg2, long arg3);
    void processMisReplicatedBlocks();
    BlockInterface addStoredBlock(BlockInfoInterface arg0, BlockInterface arg1, DatanodeStorageInfoInterface arg2, DatanodeDescriptorInterface arg3, boolean arg4) throws java.io.IOException;
    int numCorruptReplicas(BlockInterface arg0);
    void chooseExcessRedundancies(java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg0, BlockInfoInterface arg1, short arg2, DatanodeDescriptorInterface arg3, DatanodeDescriptorInterface arg4);
    void setBlocksReplWorkMultiplier(int arg0);
    void shutdown();
    void removeBlockFromMap(BlockInfoInterface arg0);
    boolean isLegacyBlock(BlockInterface arg0);
    short getExpectedLiveRedundancyNum(BlockInfoInterface arg0, NumberReplicasInterface arg1);
    java.util.Iterator<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo> getCorruptReplicaBlockIterator();
    boolean isBlockTokenEnabled();
    org.apache.hadoop.hdfs.server.blockmanagement.NumberReplicas.StoredReplicaState checkReplicaOnStorage(NumberReplicasInterface arg0, BlockInfoInterface arg1, DatanodeStorageInfoInterface arg2, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> arg3, boolean arg4);
    void processChosenExcessRedundancy(java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg0, DatanodeStorageInfoInterface arg1, BlockInfoInterface arg2);
    void enableRMTerminationForTesting();
    void chooseExcessRedundancyStriped(org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg0, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg1, BlockInfoInterface arg2, DatanodeDescriptorInterface arg3);
    <T> T runBlockOp(java.util.concurrent.Callable<T> arg0) throws java.io.IOException;
    void processQueuedMessagesForBlock(BlockInterface arg0) throws java.io.IOException;
    void metaSave(java.io.PrintWriter arg0);
    void invalidateCorruptReplicas(BlockInfoInterface arg0, BlockInterface arg1, NumberReplicasInterface arg2);
    void setPostponeBlocksFromFuture(boolean arg0);
    boolean shouldPopulateReplQueues();
    ProvidedStorageMapInterface getProvidedStorageMap();
    java.lang.String getBlockPoolId();
    void countReplicasForStripedBlock(NumberReplicasInterface arg0, BlockInfoStripedInterface arg1, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> arg2, boolean arg3);
    void removeBRLeaseIfNeeded(DatanodeIDInterface arg0, BlockReportContextInterface arg1) throws java.io.IOException;
    void updateHeartbeatState(DatanodeDescriptorInterface arg0, StorageReportInterface[] arg1, long arg2, long arg3, int arg4, int arg5, VolumeFailureSummaryInterface arg6);
    void addBlock(DatanodeStorageInfoInterface arg0, BlockInterface arg1, java.lang.String arg2) throws java.io.IOException;
//    org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.MisReplicationResult processMisReplicatedBlock(BlockInfoInterface arg0);
    long getMissingBlocks();
    org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementStatus getBlockPlacementStatus(BlockInfoInterface arg0);
    void processAllPendingDNMessages() throws java.io.IOException;
    void processIncrementalBlockReport(DatanodeIDInterface arg0, StorageReceivedDeletedBlocksInterface arg1) throws java.io.IOException;
    int getTotalBlocks();
    int computeBlockReconstructionWork(int arg0);
    void processIncrementalBlockReport(DatanodeDescriptorInterface arg0, StorageReceivedDeletedBlocksInterface arg1) throws java.io.IOException;
    void setBlockToken(LocatedBlockInterface arg0, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg1) throws java.io.IOException;
    void setBlockToken(org.apache.hadoop.hdfs.protocol.LocatedBlock arg0, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg1) throws java.io.IOException;
    void checkSafeMode();
    BlockPlacementPolicyInterface getStriptedBlockPlacementPolicy();
    DataEncryptionKeyInterface generateDataEncryptionKey();
    LocatedBlockInterface newLocatedBlock(ExtendedBlockInterface arg0, DatanodeStorageInfoInterface[] arg1, long arg2, boolean arg3);
    long getBytesInFutureECBlockGroups();
    LocatedStripedBlockInterface newLocatedStripedBlock(ExtendedBlockInterface arg0, DatanodeStorageInfoInterface[] arg1, byte[] arg2, long arg3, boolean arg4);
    long getPendingDeletionReplicatedBlocks();
    void setReplicationStreamsHardLimit(int arg0);
    BlockReconstructionWorkInterface scheduleReconstruction(BlockInfoInterface arg0, int arg1);
    void updateHeartbeat(DatanodeDescriptorInterface arg0, StorageReportInterface[] arg1, long arg2, long arg3, int arg4, int arg5, VolumeFailureSummaryInterface arg6);
    int computeInvalidateWork(int arg0);
    void rescanPostponedMisreplicatedBlocks();
    void processExtraRedundancyBlock(BlockInfoInterface arg0, short arg1, DatanodeDescriptorInterface arg2, DatanodeDescriptorInterface arg3);
    boolean getShouldPostponeBlocksFromFuture();
    boolean isNodeHealthyForDecommissionOrMaintenance(DatanodeDescriptorInterface arg0);
    long getLastRedundancyMonitorTS();
    LocatedBlockInterface newLocatedBlock(ExtendedBlockInterface arg0, BlockInfoInterface arg1, DatanodeStorageInfoInterface[] arg2, long arg3) throws java.io.IOException;
    boolean createSPSManager(ConfigurationInterface arg0);
    DatanodeManagerInterface getDatanodeManager();
    void chooseExcessRedundancyContiguous(java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg0, BlockInfoInterface arg1, short arg2, DatanodeDescriptorInterface arg3, DatanodeDescriptorInterface arg4, java.util.List<org.apache.hadoop.fs.StorageType> arg5);
    long getLowRedundancyECBlockGroups();
    void queueReportedBlock(DatanodeStorageInfoInterface arg0, BlockInterface arg1, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg2, java.lang.String arg3);
    void setExcludeSlowNodesEnabled(boolean arg0);
    long getBytesInFuture();
    long getLowRedundancyBlocks();
    boolean commitBlock(BlockInfoInterface arg0, BlockInterface arg1) throws java.io.IOException;
    long getBytesInFutureReplicatedBlocks();
    long getPendingReconstructionBlocksCount();
    int processMisReplicatedBlocks(java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo> arg0);
    void setReplication(short arg0, short arg1, BlockInfoInterface arg2);
    void addStoredBlockUnderConstruction(StatefulBlockInfoInterface arg0, DatanodeStorageInfoInterface arg1) throws java.io.IOException;
    long addBlock(BlockInfoInterface arg0, java.util.List<org.apache.hadoop.hdfs.server.protocol.BlocksWithLocations.BlockWithLocations> arg1);
    long getCorruptECBlockGroups();
    void findAndMarkBlockAsCorrupt(ExtendedBlockInterface arg0, DatanodeInfoInterface arg1, java.lang.String arg2, java.lang.String arg3) throws java.io.IOException;
    void findAndMarkBlockAsCorrupt(ExtendedBlock arg0, DatanodeInfo arg1, java.lang.String arg2, java.lang.String arg3) throws java.io.IOException;
    BlockTokenSecretManagerInterface getBlockTokenSecretManager();
    short getMinReplicationToBeInMaintenance();
    int setBlockIndices(BlockInfoInterface arg0, byte[] arg1, int arg2, DatanodeStorageInfoInterface arg3);
    boolean isNeededReconstruction(BlockInfoInterface arg0, NumberReplicasInterface arg1);
    void markBlockAsCorrupt(BlockToMarkCorruptInterface arg0, DatanodeStorageInfoInterface arg1, DatanodeDescriptorInterface arg2) throws java.io.IOException;
    int getBlocksReplWorkMultiplier();
    boolean isPlacementPolicySatisfied(BlockInfoInterface arg0);
    int getReplicationStreamsHardLimit();
    void processMisReplicatesAsync() throws java.lang.InterruptedException;
    BlockReportLeaseManagerInterface getBlockReportLeaseManager();
    BlockInfoInterface addBlockCollection(BlockInfoInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg1);
    void activate(ConfigurationInterface arg0, long arg1);
    void updateState();
    void setBlockTotal(long arg0);
    double getReconstructionQueuesInitProgress();
    long getPendingDeletionECBlocks();
    boolean isNeededReconstruction(BlockInfoInterface arg0, NumberReplicasInterface arg1, int arg2);
    void close();
    boolean hasNonEcBlockUsingStripedID();
    DaemonInterface getRedundancyThread();
    void forceCompleteBlock(BlockInfoInterface arg0) throws java.io.IOException;
    boolean shouldProcessExtraRedundancy(NumberReplicasInterface arg0, int arg1);
    DatanodeDescriptorInterface getDatanodeDescriptorFromStorage(DatanodeStorageInfoInterface arg0);
    int getPendingDataNodeMessageCount();
    boolean shouldUpdateBlockKey(long arg0) throws java.io.IOException;
    DatanodeStorageInfoInterface[] getStorages(BlockInfoInterface arg0);
    void stopReconstructionInitializer();
    NumberReplicasInterface countNodes(BlockInfoInterface arg0);
    BlockPlacementPolicyInterface getBlockPlacementPolicy();
    int numOfUnderReplicatedBlocks();
    void completeBlock(BlockInfoInterface arg0, INodesInPathInterface arg1, boolean arg2) throws java.io.IOException;
    short getExpectedRedundancyNum(BlockInfoInterface arg0);
    long getScheduledReplicationBlocksCount();
    long getNumTimedOutPendingReconstructions();
    void adjustSrcNodesAndIndices(BlockInfoStripedInterface arg0, DatanodeDescriptorInterface[] arg1, java.util.List<java.lang.Byte> arg2, DatanodeDescriptorInterface[] arg3, byte[] arg4);
    BlockToMarkCorruptInterface checkReplicaCorrupt(BlockInterface arg0, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg1, BlockInfoInterface arg2, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState arg3, DatanodeDescriptorInterface arg4);
    long getCorruptBlocks();
    void addExpectedReplicasToPending(BlockInfoInterface arg0);
    long getBlockRecoveryTimeout(long arg0);
    void ensurePositiveInt(int arg0, java.lang.String arg1);
    void markBlockReplicasAsCorrupt(BlockInterface arg0, BlockInfoInterface arg1, long arg2, long arg3, DatanodeStorageInfoInterface[] arg4) throws java.io.IOException;
    int computeDatanodeWork();
    int countLiveNodes(BlockInfoInterface arg0);
    long nextBlockId(org.apache.hadoop.hdfs.protocol.BlockType arg0);
    java.lang.String getSafeModeTip();
    BlockStoragePolicySuiteInterface getStoragePolicySuite();
    boolean isReplicaCorrupt(BlockInfoInterface arg0, DatanodeDescriptorInterface arg1);
    void checkRedundancy(org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg0);
    BlockInfoInterface addBlockCollectionWithCheck(BlockInfoInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg1);
    long getExcessBlocksCount();
    void successfulBlockRecovery(BlockInfoInterface arg0);
    boolean isBlockUnderConstruction(BlockInfoInterface arg0, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState arg1, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg2);
    boolean createSPSManager(ConfigurationInterface arg0, java.lang.String arg1);
    boolean hasMinStorage(BlockInfoInterface arg0);
    void removeStoredBlock(DatanodeStorageInfoInterface arg0, BlockInterface arg1, DatanodeDescriptorInterface arg2);
    boolean isNeededReconstructionForMaintenance(BlockInfoInterface arg0, NumberReplicasInterface arg1);
    void updateNeededReconstructions(BlockInfoInterface arg0, int arg1, int arg2);
    long requestBlockReportLeaseId(DatanodeRegistrationInterface arg0);
    long getTotalECBlockGroups();
    void clear();
    short getMinMaintenanceStorageNum(BlockInfoInterface arg0);
    java.lang.Iterable<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> getStorages(BlockInterface arg0);
    org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementStatus getBlockPlacementStatus(BlockInfoInterface arg0, DatanodeStorageInfoInterface[] arg1);
    long getLowRedundancyBlocksCount();
    void disableSPS();
    DatanodeDescriptorInterface[] chooseSourceDatanodes(BlockInfoInterface arg0, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> arg1, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg2, NumberReplicasInterface arg3, java.util.List<java.lang.Byte> arg4, java.util.List<java.lang.Byte> arg5, int arg6);
    boolean hasEnoughEffectiveReplicas(BlockInfoInterface arg0, NumberReplicasInterface arg1, int arg2);
    void addKeyUpdateCommand(java.util.List<org.apache.hadoop.hdfs.server.protocol.DatanodeCommand> arg0, DatanodeDescriptorInterface arg1);
    java.util.Map<org.apache.hadoop.fs.StorageType, org.apache.hadoop.hdfs.server.blockmanagement.StorageTypeStats> getStorageTypeStats();
    java.util.Collection<org.apache.hadoop.hdfs.protocol.Block> processReport(DatanodeStorageInfoInterface arg0, BlockListAsLongsInterface arg1) throws java.io.IOException;
    BlockInfoInterface getStoredBlock(BlockInterface arg0);
    BlockInfoInterface getStoredBlock(org.apache.hadoop.hdfs.protocol.Block arg0);
    long getHighestPriorityECBlockCount();
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> getDatanodeDescriptors(java.util.List<java.lang.String> arg0);
    void addBLocksToMarkedDeleteQueue(java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo> arg0);
    long getTotalReplicatedBlocks();
    void postponeBlock(BlockInterface arg0);
    boolean hasMinStorage(BlockInfoInterface arg0, int arg1);
    void createLocatedBlockList(LocatedBlockBuilderInterface arg0, BlockInfoInterface[] arg1, long arg2, long arg3, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg4) throws java.io.IOException;
    boolean getExcludeSlowNodesEnabled(org.apache.hadoop.hdfs.protocol.BlockType arg0);
    boolean isInSafeMode();
    boolean addBlockRecoveryAttempt(BlockInfoInterface arg0);
    void clearQueues();
    void enqueueBlockOp(java.lang.Runnable arg0) throws java.io.IOException;
    int getMaxReplicationStreams();
    void initializeReplQueues();
    void convertToCompleteBlock(BlockInfoInterface arg0, INodesInPathInterface arg1) throws java.io.IOException;
    boolean containsInvalidateBlock(DatanodeInfoInterface arg0, BlockInterface arg1);
    int getBlockOpQueueLength();
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> getValidLocations(BlockInfoInterface arg0);
    long getHighestPriorityReplicatedBlockCount();
    void registerDatanode(DatanodeRegistrationInterface arg0) throws java.io.IOException;
    int getUnderReplicatedNotMissingBlocks();
    boolean commitOrCompleteLastBlock(org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg0, BlockInterface arg1, INodesInPathInterface arg2) throws java.io.IOException;
    long getMissingBlocksCount();
    LocatedBlockInterface createLocatedBlock(LocatedBlockBuilderInterface arg0, BlockInfoInterface[] arg1, long arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws java.io.IOException;
    void flushBlockOps() throws java.io.IOException;
    boolean isExcess(DatanodeDescriptorInterface arg0, BlockInfoInterface arg1);
    LocatedBlockInterface createLocatedBlock(LocatedBlockBuilderInterface arg0, BlockInfoInterface arg1, long arg2, org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode arg3) throws java.io.IOException;
}
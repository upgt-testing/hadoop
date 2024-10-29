package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.namenode.AuditLogger;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.security.token.delegation.web.DelegationTokenIdentifier;
import org.apache.hadoop.hdfs.server.namenode.ha.EditLogTailer;
import org.apache.hadoop.hdfs.protocol.SnapshottableDirectoryStatus;

public interface FSNamesystemInterface {

    long getLazyPersistFileScrubberTS();

    boolean isHaEnabled();

    List<AuditLogger> getAuditLoggers();

    RetryCacheInterface getRetryCache();

    long getLeaseRecheckIntervalMs();

    long getMaxLockHoldToReleaseLeaseMs();

    int getMaxListOpenFilesResponses();

    KeyProviderCryptoExtensionInterface getProvider();

    void startSecretManagerIfNecessary();

    boolean inTransitionToActive();

    void checkOperation(NameNode.OperationCategory op);

    void readLock();

    void readLockInterruptibly();

    void readUnlock();

    void readUnlock(String opName);

    void writeLock();

    void writeLockInterruptibly();

    void writeUnlock();

    void writeUnlock(String opName);

    void writeUnlock(String opName, boolean suppressWriteLockReport);

    boolean hasWriteLock();

    boolean hasReadLock();

    int getReadHoldCount();

    int getWriteHoldCount();

    void cpLock();

    void cpLockInterruptibly();

    void cpUnlock();

    boolean isRunning();

    boolean isInStandbyState();

    BlocksWithLocationsInterface getBlocks(DatanodeIDInterface datanode, long size, long minimumBlockSize);

    //BatchedListEntries<OpenFileEntry> getFilesBlockingDecom(long prevId, String path);

    FsServerDefaultsInterface getServerDefaults();

    boolean isInSnapshot(long blockCollectionID);

    INodeFileInterface getBlockCollection(long id);

    byte[] getSrcPathsHash(String[] srcs);

    FSImageInterface getFSImage();

    FSEditLogInterface getEditLog();

    long getMissingBlocksCount();

    long getMissingReplOneBlocksCount();

    int getExpiredHeartbeats();

    long getTransactionsSinceLastCheckpoint();

    long getTransactionsSinceLastLogRoll();

    long getLastWrittenTransactionId();

    long getLastCheckpointTime();

    long getCapacityTotal();

    float getCapacityTotalGB();

    long getCapacityUsed();

    float getCapacityUsedGB();

    long getCapacityRemaining();

    long getProvidedCapacityTotal();

    float getCapacityRemainingGB();

    long getCapacityUsedNonDFS();

    int getTotalLoad();

    int getNumSnapshottableDirs();

    int getNumSnapshots();

    String getSnapshotStats();

    int getNumEncryptionZones();

    long getCurrentTokensCount();

    int getFsLockQueueLength();

    long getNumOfReadLockLongHold();

    long getNumOfWriteLockLongHold();

    long getBlocksTotal();

    long getNumFilesUnderConstruction();

    long getNumActiveClients();

    long getCompleteBlocksTotal();

    boolean isInSafeMode();

    boolean isInStartupSafeMode();

    void processIncrementalBlockReport(DatanodeID nodeID, StorageReceivedDeletedBlocksInterface srdb);

    long getMaxObjects();

    long getFilesTotal();

    long getPendingReplicationBlocks();

    long getPendingReconstructionBlocks();

    long getUnderReplicatedBlocks();

    long getLowRedundancyBlocks();

    long getCorruptReplicaBlocks();

    long getScheduledReplicationBlocks();

    long getPendingDeletionBlocks();

    long getLowRedundancyReplicatedBlocks();

    long getCorruptReplicatedBlocks();

    long getMissingReplicatedBlocks();

    long getMissingReplicationOneBlocks();

    long getHighestPriorityLowRedundancyReplicatedBlocks();

    long getHighestPriorityLowRedundancyECBlocks();

    long getBytesInFutureReplicatedBlocks();

    long getPendingDeletionReplicatedBlocks();

    long getTotalReplicatedBlocks();

    long getLowRedundancyECBlockGroups();

    long getCorruptECBlockGroups();

    long getMissingECBlockGroups();

    long getBytesInFutureECBlockGroups();

    long getPendingDeletionECBlocks();

    long getTotalECBlockGroups();

    String getEnabledEcPolicies();

    long getBlockDeletionStartTime();

    long getExcessBlocks();

    long getNumTimedOutPendingReconstructions();

    long getPostponedMisreplicatedBlocks();

    int getPendingDataNodeMessageCount();

    String getHAState();

    long getMillisSinceLastLoadedEdits();

    int getBlockCapacity();

    //HAServiceState getState();

    String getFSState();

    boolean testRMIPrint(String message);

    int getNumLiveDataNodes();

    int getNumDeadDataNodes();

    int getNumDecomLiveDataNodes();

    int getNumDecomDeadDataNodes();

    int getNumInServiceLiveDataNodes();

    int getVolumeFailuresTotal();

    long getEstimatedCapacityLostTotal();

    int getNumDecommissioningDataNodes();

    int getNumStaleDataNodes();

    int getNumStaleStorages();

    String getTopUserOpCounts();

    void logUpdateMasterKey(DelegationKeyInterface key);

    void logExpireDelegationToken(DelegationTokenIdentifierInterface id);

    String getVersion();

    long getUsed();

    long getFree();

    long getTotal();

    long getProvidedCapacity();

    String getSafemode();

    boolean isUpgradeFinalized();

    long getNonDfsUsedSpace();

    float getPercentUsed();

    long getBlockPoolUsedSpace();

    float getPercentBlockPoolUsed();

    float getPercentRemaining();

    long getCacheCapacity();

    long getCacheUsed();

    long getTotalBlocks();

    long getNumberOfMissingBlocks();

    long getNumberOfMissingBlocksWithReplicationFactorOne();

    int getThreads();

    String getLiveNodes();

    String getDeadNodes();

    String getDecomNodes();

    String getEnteringMaintenanceNodes();

    String getClusterId();

    String getBlockPoolId();

    String getNameDirStatuses();

    String getNodeUsage();

    String getNameJournalStatus();

    String getJournalTransactionInfo();

    long getNNStartedTimeInMillis();

    String getCompileInfo();

    BlockManagerInterface getBlockManager();

    void setBlockManagerForTesting(BlockManager bm);

    FSDirectoryInterface getFSDirectory();

    void setFSDirectory(FSDirectory dir);

    CacheManagerInterface getCacheManager();

    ErasureCodingPolicyManagerInterface getErasureCodingPolicyManager();

    HAContextInterface getHAContext();

    String getCorruptFiles();

    int getCorruptFilesCount();

    long getNumberOfSnapshottableDirs();

    int getDistinctVersionCount();

    Map<String, Integer> getDistinctVersions();

    String getSoftwareVersion();

    String getNameDirSize();

    void verifyToken(DelegationTokenIdentifier identifier, byte[] password);

    EditLogTailerInterface getEditLogTailer();

    void setEditLogTailerForTests(EditLogTailer tailer);

    //ReentrantReadWriteLock getFsLockForTests();

    //ReentrantLock getCpLockForTests();

    void setNNResourceChecker(NameNodeResourceCheckerInterface nnResourceChecker);

    SnapshotManagerInterface getSnapshotManager();

    SnapshottableDirectoryStatus[] getSnapshottableDirListing();

    void setCreatedRollbackImages(boolean created);

    RollingUpgradeInfoInterface getRollingUpgradeInfo();

    boolean isNeedRollbackFsImage();

    void setNeedRollbackFsImage(boolean needRollbackFsImage);

    //RollingUpgradeInfo.Bean getRollingUpgradeStatus();

    boolean isRollingUpgrade();

    int getEffectiveLayoutVersion();

    ECTopologyVerifierResultInterface getECTopologyResultForPolicies(String[] policyNames);

    void removeXattr(long id, String xattrName);

    long getTotalSyncCount();

    String getTotalSyncTimes();

    long getBytesInFuture();

    int getNumInMaintenanceLiveDataNodes();

    int getNumInMaintenanceDeadDataNodes();

    int getNumEnteringMaintenanceDataNodes();

    String getVerifyECWithTopologyResult();

    void checkErasureCodingSupported(String operationName);
}

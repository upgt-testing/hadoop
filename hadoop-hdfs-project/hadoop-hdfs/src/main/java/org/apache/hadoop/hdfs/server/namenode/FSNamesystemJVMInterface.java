package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.fs.permission.AclStatusJVMInterface;
import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenSecretManagerJVMInterface;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.ha.EditLogTailerJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManagerJVMInterface;


import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface FSNamesystemJVMInterface {
    void leaveSafeMode(boolean force);
    SnapshotManagerJVMInterface getSnapshotManager();
    CacheManagerJVMInterface getCacheManager();
    int getPendingDataNodeMessageCount();
    long getMillisSinceLastLoadedEdits();
    String getHAState();
    EditLogTailerJVMInterface getEditLogTailer();
    long getFilesTotal();
    String getClusterId();
    //float getReconstructionQueuesInitProgress();
    boolean hasWriteLock();
    void writeLock();
    void writeUnlock();
    AclStatusJVMInterface getAclStatus(String src) throws IOException;
    FSEditLogJVMInterface getEditLog();
    boolean saveNamespace(final long timeWindow, final long txGap) throws IOException;
    void enterSafeMode(boolean resourcesLow) throws IOException;
    int getVolumeFailuresTotal();
    long getCompleteBlocksTotal();
    DelegationTokenSecretManagerJVMInterface getDelegationTokenSecretManager();
    ReentrantReadWriteLock getFsLockForTests();
    void setFsLockForTests(ReentrantReadWriteLock lock);
    boolean isInSafeMode();
    long getPendingReplicationBlocks();
    long getCorruptReplicaBlocks();
    long getMissingBlocksCount();
    FSImageJVMInterface getFSImage();
    void readLock();
    BlockManagerJVMInterface getBlockManager();
    void readUnlock();
    LeaseManagerJVMInterface getLeaseManager();
    long[] getStats();
    boolean isHaEnabled();
    String getBlockPoolId();
    FSDirectoryJVMInterface getFSDirectory();
    String getSafemode();
    long getBlocksTotal();
    long getPendingDeletionBlocks();
    long getNNStartedTimeInMillis();
    long getBlockDeletionStartTime();
    long getUnderReplicatedBlocks();
}

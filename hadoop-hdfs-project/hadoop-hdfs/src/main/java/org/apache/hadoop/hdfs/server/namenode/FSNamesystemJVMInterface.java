package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenSecretManagerJVMInterface;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerJVMInterface;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManagerJVMInterface;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface FSNamesystemJVMInterface {
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
}

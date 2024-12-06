package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerJVMInterface;

public interface FSNamesystemJVMInterface {
    void readLock();
    BlockManagerJVMInterface getBlockManager();
    void readUnlock();
    LeaseManagerJVMInterface getLeaseManager();
    long[] getStats();
    boolean isHaEnabled();
}

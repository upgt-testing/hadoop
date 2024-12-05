package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;

public interface FSNamesystemInterface {
    void readLock();
    BlockManager getBlockManager();
    void readUnlock();
    LeaseManager getLeaseManager();
}

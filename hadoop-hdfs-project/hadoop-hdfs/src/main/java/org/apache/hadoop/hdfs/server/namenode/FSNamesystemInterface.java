package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerInterface;

public interface FSNamesystemInterface {
    void readLock();
    BlockManagerInterface getBlockManager();
    void readUnlock();
    LeaseManagerInterface getLeaseManager();
    long[] getStats();
}

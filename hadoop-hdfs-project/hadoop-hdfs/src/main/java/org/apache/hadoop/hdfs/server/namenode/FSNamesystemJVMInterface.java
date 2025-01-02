package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerJVMInterface;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManagerJVMInterface;

public interface FSNamesystemJVMInterface {
    void readLock();
    BlockManagerJVMInterface getBlockManager();
    void readUnlock();
    LeaseManagerJVMInterface getLeaseManager();
    long[] getStats();
    boolean isHaEnabled();
    String getBlockPoolId();
}

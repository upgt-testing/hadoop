package org.apache.hadoop.hdfs.server.blockmanagement;

public interface DatanodeAdminManagerJVMInterface {
    int getBlocksPerLock();
    int getPendingRepLimit();
}

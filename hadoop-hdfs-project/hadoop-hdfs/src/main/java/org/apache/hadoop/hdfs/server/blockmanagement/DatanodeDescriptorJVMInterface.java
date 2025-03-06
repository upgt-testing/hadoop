package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.protocol.DatanodeInfoJVMInterface;

import java.util.UUID;

public interface DatanodeDescriptorJVMInterface extends DatanodeInfoJVMInterface {
    DatanodeStorageInfoJVMInterface[] getStorageInfos();
    boolean checkBlockReportReceived();
    boolean isAlive();
    String getDatanodeUuid();
    String getXferAddr();
    int getBlocksScheduled();
    String getName();
}

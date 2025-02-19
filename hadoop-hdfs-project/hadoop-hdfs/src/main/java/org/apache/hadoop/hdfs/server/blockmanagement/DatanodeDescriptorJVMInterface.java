package org.apache.hadoop.hdfs.server.blockmanagement;

import java.util.UUID;

public interface DatanodeDescriptorJVMInterface {
    boolean checkBlockReportReceived();
    boolean isAlive();
    String getDatanodeUuid();
    String getXferAddr();
}

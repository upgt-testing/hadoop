package org.apache.hadoop.hdfs.server.blockmanagement;

public interface HeartbeatManagerJVMInterface {
    void restartHeartbeatStopWatch();
    void heartbeatCheck();
    DatanodeDescriptorJVMInterface[] getDatanodes();
}

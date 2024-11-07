package org.apache.hadoop.hdfs.remoteProxies;

public interface HeartbeatResponseInterface {
    RollingUpgradeStatusInterface getRollingUpdateStatus();
    long getFullBlockReportLeaseId();
    DatanodeCommandInterface[] getCommands();
    NNHAStatusHeartbeatInterface getNameNodeHaState();
}
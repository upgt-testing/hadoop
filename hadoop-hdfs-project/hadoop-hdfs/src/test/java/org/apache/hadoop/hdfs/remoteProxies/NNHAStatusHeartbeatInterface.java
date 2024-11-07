package org.apache.hadoop.hdfs.remoteProxies;

public interface NNHAStatusHeartbeatInterface {
    long getTxId();
    org.apache.hadoop.ha.HAServiceProtocol.HAServiceState getState();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockToMarkCorruptInterface {
    BlockInfoInterface getStored();
    java.lang.String getReason();
    BlockInterface getCorrupted();
    org.apache.hadoop.hdfs.server.blockmanagement.CorruptReplicasMap.Reason getReasonCode();
    boolean isCorruptedDuringWrite();
    java.lang.String toString();
}
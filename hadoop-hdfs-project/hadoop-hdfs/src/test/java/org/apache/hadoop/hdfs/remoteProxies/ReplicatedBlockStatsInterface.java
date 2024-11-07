package org.apache.hadoop.hdfs.remoteProxies;

public interface ReplicatedBlockStatsInterface {
    java.lang.String toString();
    boolean hasHighestPriorityLowRedundancyBlocks();
    long getMissingReplicationOneBlocks();
    long getCorruptBlocks();
    ReplicatedBlockStatsInterface merge(java.util.Collection<org.apache.hadoop.hdfs.protocol.ReplicatedBlockStats> arg0);
    java.lang.Long getHighestPriorityLowRedundancyBlocks();
    long getBytesInFutureBlocks();
    long getMissingReplicaBlocks();
    long getPendingDeletionBlocks();
    long getLowRedundancyBlocks();
}
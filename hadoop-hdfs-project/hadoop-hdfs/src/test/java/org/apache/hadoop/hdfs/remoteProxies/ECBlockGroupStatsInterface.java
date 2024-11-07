package org.apache.hadoop.hdfs.remoteProxies;

public interface ECBlockGroupStatsInterface {
    long getPendingDeletionBlocks();
    int hashCode();
    long getCorruptBlockGroups();
    boolean equals(java.lang.Object arg0);
    java.lang.Long getHighestPriorityLowRedundancyBlocks();
    long getMissingBlockGroups();
    java.lang.String toString();
    long getBytesInFutureBlockGroups();
    ECBlockGroupStatsInterface merge(java.util.Collection<org.apache.hadoop.hdfs.protocol.ECBlockGroupStats> arg0);
    long getLowRedundancyBlockGroups();
    boolean hasHighestPriorityLowRedundancyBlocks();
}
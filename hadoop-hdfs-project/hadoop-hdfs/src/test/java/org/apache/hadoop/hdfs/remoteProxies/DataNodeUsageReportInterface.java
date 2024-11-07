package org.apache.hadoop.hdfs.remoteProxies;

public interface DataNodeUsageReportInterface {
    boolean equals(java.lang.Object arg0);
    long getTimestamp();
    long getReadTime();
    long getBlocksWrittenPerSec();
    long getBytesWrittenPerSec();
    long getBytesReadPerSec();
    int hashCode();
    long getWriteTime();
    long getBlocksReadPerSec();
    java.lang.String toString();
}
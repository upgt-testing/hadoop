package org.apache.hadoop.hdfs.remoteProxies;

public interface DiffStatsInterface {
    long getTotalFilesProcessed();
    long getTotalFilesCompared();
    long getTotalDirsProcessed();
    long getTotalDirsCompared();
    long getTotalChildrenListingTime();
}
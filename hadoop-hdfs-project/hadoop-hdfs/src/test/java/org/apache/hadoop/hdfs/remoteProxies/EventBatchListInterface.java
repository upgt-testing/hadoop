package org.apache.hadoop.hdfs.remoteProxies;

public interface EventBatchListInterface {
    long getLastTxid();
    java.util.List<org.apache.hadoop.hdfs.inotify.EventBatch> getBatches();
    long getFirstTxid();
    long getSyncTxid();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface PerStorageIBRInterface {
    void increaseBlocksCounter(ReceivedDeletedBlockInfoInterface arg0);
    int putMissing(ReceivedDeletedBlockInfoInterface[] arg0);
    ReceivedDeletedBlockInfoInterface remove(BlockInterface arg0);
    ReceivedDeletedBlockInfoInterface[] removeAll();
    void put(ReceivedDeletedBlockInfoInterface arg0);
}
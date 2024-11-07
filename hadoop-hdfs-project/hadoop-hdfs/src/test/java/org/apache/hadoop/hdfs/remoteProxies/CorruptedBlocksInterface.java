package org.apache.hadoop.hdfs.remoteProxies;

public interface CorruptedBlocksInterface {
    void addCorruptedBlock(ExtendedBlockInterface arg0, DatanodeInfoInterface arg1);
    java.util.Map<org.apache.hadoop.hdfs.protocol.ExtendedBlock, java.util.Set<org.apache.hadoop.hdfs.protocol.DatanodeInfo>> getCorruptionMap();
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockRecoveryCommandInterface {
    int getAction();
    void add(RecoveringBlockInterface arg0);
    java.lang.String toString();
    java.util.Collection<org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand.RecoveringBlock> getRecoveringBlocks();
}
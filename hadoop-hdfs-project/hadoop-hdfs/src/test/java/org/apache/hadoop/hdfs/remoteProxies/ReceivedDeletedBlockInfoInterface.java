package org.apache.hadoop.hdfs.remoteProxies;

public interface ReceivedDeletedBlockInfoInterface {
    java.lang.String toString();
    void setDelHints(java.lang.String arg0);
    java.lang.String getDelHints();
    boolean blockEquals(BlockInterface arg0);
    void setBlock(BlockInterface arg0);
    BlockInterface getBlock();
    boolean equals(java.lang.Object arg0);
    boolean isDeletedBlock();
    org.apache.hadoop.hdfs.server.protocol.ReceivedDeletedBlockInfo.BlockStatus getStatus();
    int hashCode();
}
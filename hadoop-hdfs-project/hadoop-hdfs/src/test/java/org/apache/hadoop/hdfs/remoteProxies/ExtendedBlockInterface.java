package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtendedBlockInterface {
    void set(java.lang.String arg0, BlockInterface arg1);
    long getGenerationStamp();
    BlockInterface getLocalBlock(ExtendedBlockInterface arg0);
    void setGenerationStamp(long arg0);
    java.lang.String getBlockPoolId();
    int hashCode();
    java.lang.String getBlockName();
    void setBlockId(long arg0);
    long getBlockId();
    boolean equals(java.lang.Object arg0);
    long getNumBytes();
    java.lang.String toString();
    void setNumBytes(long arg0);
    BlockInterface getLocalBlock();
}
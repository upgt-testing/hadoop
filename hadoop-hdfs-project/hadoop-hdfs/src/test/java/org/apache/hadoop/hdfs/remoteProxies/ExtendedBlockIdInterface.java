package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtendedBlockIdInterface {
    boolean equals(java.lang.Object arg0);
    int hashCode();
    java.lang.String toString();
    ExtendedBlockIdInterface fromExtendedBlock(ExtendedBlockInterface arg0);
    long getBlockId();
    java.lang.String getBlockPoolId();
}
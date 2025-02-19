package org.apache.hadoop.hdfs.protocol;

public interface ExtendedBlockJVMInterface {
    String getBlockPoolId();
    long getBlockId();
    String getBlockName();
}

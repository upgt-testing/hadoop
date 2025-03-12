package org.apache.hadoop.hdfs.protocol;

public interface BlockJVMInterface {
    long getBlockId();
    long getNumBytes();
    long getGenerationStamp();
}

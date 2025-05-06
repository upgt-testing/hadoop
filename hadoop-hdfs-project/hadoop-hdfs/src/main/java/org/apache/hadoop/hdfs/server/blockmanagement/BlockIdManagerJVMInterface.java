package org.apache.hadoop.hdfs.server.blockmanagement;

public interface BlockIdManagerJVMInterface {
    SequentialBlockIdGeneratorJVMInterface getBlockIdGenerator();
    long getGenerationStamp();
}

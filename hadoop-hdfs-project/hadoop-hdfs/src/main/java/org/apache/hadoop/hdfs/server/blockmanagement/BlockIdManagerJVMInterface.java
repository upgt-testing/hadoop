package org.apache.hadoop.hdfs.server.blockmanagement;

public interface BlockIdManagerJVMInterface {
    SequentialBlockGroupIdGeneratorJVMInterface getBlockGroupIdGenerator();
    SequentialBlockIdGeneratorJVMInterface getBlockIdGenerator();

}

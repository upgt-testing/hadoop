package org.apache.hadoop.hdfs.server.blockmanagement;

public interface SequentialBlockIdGeneratorJVMInterface {
    long nextValue();
    void setCurrentValue(long value);
    long getCurrentValue();
}

package org.apache.hadoop.hdfs.server.blockmanagement;

public interface SequentialBlockGroupIdGeneratorJVMInterface {
    void setCurrentValue(long value);
    long getCurrentValue();
    void skipTo(long value);
}

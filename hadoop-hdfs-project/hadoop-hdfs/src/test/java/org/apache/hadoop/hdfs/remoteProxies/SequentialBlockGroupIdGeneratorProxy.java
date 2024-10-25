package org.apache.hadoop.hdfs.remoteProxies;

public interface SequentialBlockGroupIdGeneratorProxy {
    long getCurrentValue();
    void setCurrentValue(long value);
    void skipTo(long newValue) throws IllegalStateException;
}


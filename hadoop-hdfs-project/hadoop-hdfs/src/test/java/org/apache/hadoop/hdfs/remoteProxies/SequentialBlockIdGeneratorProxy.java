package org.apache.hadoop.hdfs.remoteProxies;

public interface SequentialBlockIdGeneratorProxy {
    void setCurrentValue(long value);
    int getCurrentValue();
}

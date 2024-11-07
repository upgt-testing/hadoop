package org.apache.hadoop.hdfs.remoteProxies;

public interface SequentialBlockIdGeneratorInterface {
    long nextValue();
    void setCurrentValue(long arg0);
    int hashCode();
    void skipTo(long arg0) throws java.lang.IllegalStateException;
    long getCurrentValue();
    boolean setIfGreater(long arg0);
    boolean isValidBlock(BlockInterface arg0);
    boolean equals(java.lang.Object arg0);
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface SequentialBlockGroupIdGeneratorInterface {
    boolean hasValidBlockInRange(BlockInterface arg0);
    boolean setIfGreater(long arg0);
    void setCurrentValue(long arg0);
    int hashCode();
    boolean equals(java.lang.Object arg0);
    long getCurrentValue();
    long nextValue();
    void skipTo(long arg0) throws java.lang.IllegalStateException;
}
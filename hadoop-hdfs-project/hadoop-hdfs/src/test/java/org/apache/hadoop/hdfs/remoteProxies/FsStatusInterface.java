package org.apache.hadoop.hdfs.remoteProxies;

public interface FsStatusInterface {
    long getCapacity();
    long getRemaining();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    long getUsed();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface ExportedBlockKeysInterface {
    boolean isBlockTokenEnabled();
    BlockKeyInterface[] getAllKeys();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    long getKeyUpdateInterval();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    BlockKeyInterface getCurrentKey();
    long getTokenLifetime();
}
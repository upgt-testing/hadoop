package org.apache.hadoop.hdfs.remoteProxies;

public interface FileChecksumInterface {
    boolean equals(java.lang.Object arg0);
    int hashCode();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    byte[] getBytes();
    int getLength();
    ChecksumOptInterface getChecksumOpt();
    java.lang.String getAlgorithmName();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
}
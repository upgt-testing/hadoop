package org.apache.hadoop.hdfs.remoteProxies;

public interface FsServerDefaultsInterface {
    long getBlockSize();
    boolean getEncryptDataTransfer();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    short getReplication();
    java.lang.String getKeyProviderUri();
    int getWritePacketSize();
    int getFileBufferSize();
    org.apache.hadoop.util.DataChecksum.Type getChecksumType();
    long getTrashInterval();
    byte getDefaultStoragePolicyId();
    int getBytesPerChecksum();
}
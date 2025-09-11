package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FsServerDefaultsJVMInterface extends WritableJVMInterface {

    boolean getEncryptDataTransfer();

    int getWritePacketSize();

    java.lang.Object getChecksumType();

    java.lang.String getKeyProviderUri();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    int getFileBufferSize();

    byte getDefaultStoragePolicyId();

    short getReplication();

    long getBlockSize();

    long getTrashInterval();

    int getBytesPerChecksum();
}

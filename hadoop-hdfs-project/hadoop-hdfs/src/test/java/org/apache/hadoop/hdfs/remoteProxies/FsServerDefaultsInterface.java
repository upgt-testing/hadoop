package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface FsServerDefaultsInterface {

    long getBlockSize();

    int getBytesPerChecksum();

    int getWritePacketSize();

    short getReplication();

    int getFileBufferSize();

    boolean getEncryptDataTransfer();

    long getTrashInterval();

    //DataChecksum.Type getChecksumType();

    String getKeyProviderUri();

    byte getDefaultStoragePolicyId();

    void write(DataOutput out);

    void readFields(DataInput in);
}

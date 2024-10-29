package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.StorageType;

public interface BlockTokenIdentifierInterface {

    Text getKind();

    UserGroupInformationInterface getUser();

    long getExpiryDate();

    void setExpiryDate(long expiryDate);

    int getKeyId();

    void setKeyId(int keyId);

    String getUserId();

    String getBlockPoolId();

    long getBlockId();

    EnumSet<BlockTokenIdentifier.AccessMode> getAccessModes();

    StorageType[] getStorageTypes();

    String[] getStorageIds();

    byte[] getHandshakeMsg();

    void setHandshakeMsg(byte[] bytes);

    String toString();

    boolean equals(Object obj);

    int hashCode();

    void readFields(DataInput in);

    void write(DataOutput out);

    byte[] getBytes();
}

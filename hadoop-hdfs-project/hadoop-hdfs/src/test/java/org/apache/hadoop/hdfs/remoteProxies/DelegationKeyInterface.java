package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DelegationKeyInterface {

    int getKeyId();

    long getExpiryDate();

    //SecretKey getKey();

    byte[] getEncodedKey();

    void setExpiryDate(long expiryDate);

    void write(DataOutput out);

    void readFields(DataInput in);

    int hashCode();

    boolean equals(Object right);
}

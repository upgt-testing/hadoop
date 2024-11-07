package org.apache.hadoop.hdfs.remoteProxies;

public interface DelegationKeyInterface {
    byte[] getEncodedKey();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void setExpiryDate(long arg0);
    int getKeyId();
    long getExpiryDate();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    boolean equals(java.lang.Object arg0);
    javax.crypto.SecretKey getKey();
    int hashCode();
}
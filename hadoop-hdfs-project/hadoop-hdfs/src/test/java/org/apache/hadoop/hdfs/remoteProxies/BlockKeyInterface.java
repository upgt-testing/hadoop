package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockKeyInterface {
    int getKeyId();
    javax.crypto.SecretKey getKey();
    long getExpiryDate();
    boolean equals(java.lang.Object arg0);
    void setExpiryDate(long arg0);
    byte[] getEncodedKey();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    int hashCode();
}
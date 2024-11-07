package org.apache.hadoop.hdfs.remoteProxies;

public interface MD5HashInterface {
    MD5HashInterface read(java.io.DataInput arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    MD5HashInterface digest(byte[] arg0, int arg1, int arg2);
    MD5HashInterface digest(java.io.InputStream arg0) throws java.io.IOException;
    long halfDigest();
    MD5HashInterface digest(java.lang.String arg0);
    int hashCode();
    boolean equals(java.lang.Object arg0);
    MD5HashInterface digest(byte[][] arg0, int arg1, int arg2);
    MD5HashInterface digest(UTF8Interface arg0);
    int charToNibble(char arg0);
    int quarterDigest();
    void set(MD5HashInterface arg0);
    java.security.MessageDigest getDigester();
    int compareTo(MD5HashInterface arg0);
    void setDigest(java.lang.String arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    byte[] getDigest();
    MD5HashInterface digest(byte[] arg0);
    java.lang.String toString();
}
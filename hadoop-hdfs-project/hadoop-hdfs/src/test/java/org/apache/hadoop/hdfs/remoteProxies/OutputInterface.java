package org.apache.hadoop.hdfs.remoteProxies;

public interface OutputInterface {
    void flushLastBuffer();
    void flush() throws java.io.IOException;
    void reset();
    void flushFullBuffer(int arg0);
    java.io.OutputStream nullOutputStream();
    java.lang.String toString();
    byte[] copyArray(byte[] arg0, int arg1);
    void write(int arg0);
    int size();
    ByteStringInterface toByteString();
    void write(byte[] arg0, int arg1, int arg2);
    void close() throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
}
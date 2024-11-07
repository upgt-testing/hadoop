package org.apache.hadoop.hdfs.remoteProxies;

public interface FramedBufferInterface {
    int size();
    byte[] toByteArray();
    java.lang.String toString(java.nio.charset.Charset arg0);
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    void setSize(int arg0);
    void reset();
    void grow(int arg0);
    java.lang.String toString();
    void ensureCapacity(int arg0);
    void write(byte[] arg0, int arg1, int arg2);
    void setCapacity(int arg0);
    java.io.OutputStream nullOutputStream();
    java.lang.String toString(int arg0);
    void write(int arg0);
    void writeBytes(byte[] arg0);
    java.lang.String toString(java.lang.String arg0) throws java.io.UnsupportedEncodingException;
    int capacity();
    void close() throws java.io.IOException;
    int hugeCapacity(int arg0);
    void write(byte[] arg0) throws java.io.IOException;
    void flush() throws java.io.IOException;
}
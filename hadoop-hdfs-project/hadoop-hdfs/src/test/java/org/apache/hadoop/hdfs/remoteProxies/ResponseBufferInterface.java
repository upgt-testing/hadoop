package org.apache.hadoop.hdfs.remoteProxies;

public interface ResponseBufferInterface {
    ResponseBufferInterface reset();
    void writeFloat(float arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    void writeUTF(java.lang.String arg0) throws java.io.IOException;
    java.io.OutputStream nullOutputStream();
    void write(int arg0) throws java.io.IOException;
    int writeUTF(java.lang.String arg0, java.io.DataOutput arg1) throws java.io.IOException;
    void writeBytes(java.lang.String arg0) throws java.io.IOException;
    void writeBoolean(boolean arg0) throws java.io.IOException;
    void ensureCapacity(int arg0);
    int capacity();
    void writeDouble(double arg0) throws java.io.IOException;
    void setCapacity(int arg0);
    void writeShort(int arg0) throws java.io.IOException;
    int size();
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void writeTo(java.io.OutputStream arg0) throws java.io.IOException;
    void incCount(int arg0);
    void writeChar(int arg0) throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
    void writeLong(long arg0) throws java.io.IOException;
    void writeChars(java.lang.String arg0) throws java.io.IOException;
    byte[] toByteArray();
    void writeByte(int arg0) throws java.io.IOException;
    void writeInt(int arg0) throws java.io.IOException;
    void flush() throws java.io.IOException;
    FramedBufferInterface getFramedBuffer();
}
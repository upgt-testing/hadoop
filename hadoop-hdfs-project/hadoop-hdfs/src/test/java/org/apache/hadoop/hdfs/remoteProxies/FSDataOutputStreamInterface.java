package org.apache.hadoop.hdfs.remoteProxies;

public interface FSDataOutputStreamInterface {
    void writeBytes(java.lang.String arg0) throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
    long getPos();
    int writeUTF(java.lang.String arg0, java.io.DataOutput arg1) throws java.io.IOException;
    void write(int arg0) throws java.io.IOException;
    void writeChar(int arg0) throws java.io.IOException;
    org.apache.hadoop.fs.Abortable.AbortableResult abort();
    void hsync() throws java.io.IOException;
    void flush() throws java.io.IOException;
    int size();
    void writeLong(long arg0) throws java.io.IOException;
    java.io.OutputStream nullOutputStream();
    void close() throws java.io.IOException;
    void writeUTF(java.lang.String arg0) throws java.io.IOException;
    org.apache.hadoop.fs.statistics.IOStatistics getIOStatistics();
    java.io.OutputStream getWrappedStream();
    void writeChars(java.lang.String arg0) throws java.io.IOException;
    void writeShort(int arg0) throws java.io.IOException;
    void writeFloat(float arg0) throws java.io.IOException;
    void writeBoolean(boolean arg0) throws java.io.IOException;
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void setDropBehind(java.lang.Boolean arg0) throws java.io.IOException;
    void writeDouble(double arg0) throws java.io.IOException;
    java.lang.String toString();
    void writeInt(int arg0) throws java.io.IOException;
    void hflush() throws java.io.IOException;
    void writeByte(int arg0) throws java.io.IOException;
    boolean hasCapability(java.lang.String arg0);
    void incCount(int arg0);
}
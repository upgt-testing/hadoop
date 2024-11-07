package org.apache.hadoop.hdfs.remoteProxies;

public interface SocketOutputStreamInterface {
    boolean isOpen();
    int write(java.nio.ByteBuffer arg0) throws java.io.IOException;
    java.nio.channels.WritableByteChannel getChannel();
    void setTimeout(int arg0);
    void waitForWritable() throws java.io.IOException;
    void transferToFully(java.nio.channels.FileChannel arg0, long arg1, int arg2) throws java.io.IOException;
    void write(int arg0) throws java.io.IOException;
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void flush() throws java.io.IOException;
    java.io.OutputStream nullOutputStream();
    void close() throws java.io.IOException;
    void transferToFully(java.nio.channels.FileChannel arg0, long arg1, int arg2, LongWritableInterface arg3, LongWritableInterface arg4) throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
}
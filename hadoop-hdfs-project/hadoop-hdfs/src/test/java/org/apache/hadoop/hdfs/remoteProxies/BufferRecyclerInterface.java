package org.apache.hadoop.hdfs.remoteProxies;

public interface BufferRecyclerInterface {
    byte[] allocByteBuffer(int arg0);
    void releaseByteBuffer(int arg0, byte[] arg1);
    byte[] balloc(int arg0);
    int byteBufferLength(int arg0);
    void releaseCharBuffer(int arg0, char[] arg1);
    int charBufferLength(int arg0);
    char[] allocCharBuffer(int arg0);
    byte[] allocByteBuffer(int arg0, int arg1);
    char[] calloc(int arg0);
    char[] allocCharBuffer(int arg0, int arg1);
}
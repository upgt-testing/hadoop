package org.apache.hadoop.hdfs.remoteProxies;

public interface IOContextInterface {
    void releaseNameCopyBuffer(char[] arg0);
    void _verifyRelease(char[] arg0, char[] arg1);
    char[] allocNameCopyBuffer(int arg0);
    byte[] allocReadIOBuffer();
    char[] allocConcatBuffer();
    boolean isResourceManaged();
    void releaseReadIOBuffer(byte[] arg0);
    byte[] allocReadIOBuffer(int arg0);
    byte[] allocBase64Buffer();
    byte[] allocWriteEncodingBuffer();
    void setEncoding(com.fasterxml.jackson.core.JsonEncoding arg0);
    void releaseWriteEncodingBuffer(byte[] arg0);
    byte[] allocWriteEncodingBuffer(int arg0);
    ContentReferenceInterface contentReference();
    void _verifyAlloc(java.lang.Object arg0);
    char[] allocTokenBuffer();
    java.lang.Object getSourceReference();
    void releaseTokenBuffer(char[] arg0);
    IOContextInterface withEncoding(com.fasterxml.jackson.core.JsonEncoding arg0);
    char[] allocTokenBuffer(int arg0);
    byte[] allocBase64Buffer(int arg0);
    void _verifyRelease(byte[] arg0, byte[] arg1);
    void releaseConcatBuffer(char[] arg0);
    TextBufferInterface constructTextBuffer();
    com.fasterxml.jackson.core.JsonEncoding getEncoding();
    void releaseBase64Buffer(byte[] arg0);
    java.lang.IllegalArgumentException wrongBuf();
}
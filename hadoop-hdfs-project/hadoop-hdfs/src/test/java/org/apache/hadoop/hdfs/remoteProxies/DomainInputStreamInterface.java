package org.apache.hadoop.hdfs.remoteProxies;

public interface DomainInputStreamInterface {
    long skip(long arg0) throws java.io.IOException;
    byte[] readNBytes(int arg0) throws java.io.IOException;
    void mark(int arg0);
    int readNBytes(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    int read(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    int available() throws java.io.IOException;
    void reset() throws java.io.IOException;
    int read() throws java.io.IOException;
    long transferTo(java.io.OutputStream arg0) throws java.io.IOException;
    byte[] readAllBytes() throws java.io.IOException;
    boolean markSupported();
    java.io.InputStream nullInputStream();
    void close() throws java.io.IOException;
    int read(byte[] arg0) throws java.io.IOException;
}
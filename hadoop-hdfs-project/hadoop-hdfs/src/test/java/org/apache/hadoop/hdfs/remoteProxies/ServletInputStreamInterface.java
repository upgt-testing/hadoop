package org.apache.hadoop.hdfs.remoteProxies;

public interface ServletInputStreamInterface {
    void setReadListener(javax.servlet.ReadListener arg0);
    long skip(long arg0) throws java.io.IOException;
    boolean isReady();
    boolean markSupported();
    void close() throws java.io.IOException;
    int readNBytes(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void mark(int arg0);
    byte[] readNBytes(int arg0) throws java.io.IOException;
    int read(byte[] arg0) throws java.io.IOException;
    java.io.InputStream nullInputStream();
    long transferTo(java.io.OutputStream arg0) throws java.io.IOException;
    int available() throws java.io.IOException;
    void reset() throws java.io.IOException;
    boolean isFinished();
    int read(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    int read() throws java.io.IOException;
    byte[] readAllBytes() throws java.io.IOException;
    int readLine(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
}
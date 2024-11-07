package org.apache.hadoop.hdfs.remoteProxies;

public interface ServletOutputStreamInterface {
    void print(float arg0) throws java.io.IOException;
    void println(float arg0) throws java.io.IOException;
    void print(boolean arg0) throws java.io.IOException;
    java.io.OutputStream nullOutputStream();
    void println(char arg0) throws java.io.IOException;
    void print(double arg0) throws java.io.IOException;
    boolean isReady();
    void setWriteListener(javax.servlet.WriteListener arg0);
    void println(long arg0) throws java.io.IOException;
    void println(java.lang.String arg0) throws java.io.IOException;
    void print(long arg0) throws java.io.IOException;
    void println() throws java.io.IOException;
    void flush() throws java.io.IOException;
    void println(int arg0) throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
    void write(int arg0) throws java.io.IOException;
    void println(double arg0) throws java.io.IOException;
    void print(java.lang.String arg0) throws java.io.IOException;
    void print(int arg0) throws java.io.IOException;
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void close() throws java.io.IOException;
    void println(boolean arg0) throws java.io.IOException;
    void print(char arg0) throws java.io.IOException;
}
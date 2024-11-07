package org.apache.hadoop.hdfs.remoteProxies;

public interface DomainOutputStreamInterface {
    void write(int arg0) throws java.io.IOException;
    java.io.OutputStream nullOutputStream();
    void close() throws java.io.IOException;
    void flush() throws java.io.IOException;
    void write(byte[] arg0) throws java.io.IOException;
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
}
package org.apache.hadoop.hdfs.remoteProxies;

public interface ByteOutputInterface {
    void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    void write(byte arg0) throws java.io.IOException;
    void writeLazy(java.nio.ByteBuffer arg0) throws java.io.IOException;
    void write(java.nio.ByteBuffer arg0) throws java.io.IOException;
    void writeLazy(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
}
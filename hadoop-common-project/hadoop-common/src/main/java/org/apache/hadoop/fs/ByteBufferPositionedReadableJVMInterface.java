package org.apache.hadoop.fs;

public interface ByteBufferPositionedReadableJVMInterface {

    int read(long arg0, java.nio.ByteBuffer arg1) throws java.io.IOException;

    void readFully(long arg0, java.nio.ByteBuffer arg1) throws java.io.IOException;
}

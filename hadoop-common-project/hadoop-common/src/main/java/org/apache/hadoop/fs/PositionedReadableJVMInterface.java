package org.apache.hadoop.fs;

public interface PositionedReadableJVMInterface {

    void readFully(long arg0, byte[] arg1) throws java.io.IOException;

    int read(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;

    void readFully(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;
}

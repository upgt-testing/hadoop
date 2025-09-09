package org.apache.hadoop.fs;

public interface SeekableJVMInterface {

    long getPos() throws java.io.IOException;

    void seek(long arg0) throws java.io.IOException;

    boolean seekToNewSource(long arg0) throws java.io.IOException;
}

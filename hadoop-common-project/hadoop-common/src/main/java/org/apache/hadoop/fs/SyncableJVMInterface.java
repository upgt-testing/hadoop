package org.apache.hadoop.fs;

public interface SyncableJVMInterface {

    void hflush() throws java.io.IOException;

    void sync() throws java.io.IOException;

    void hsync() throws java.io.IOException;
}

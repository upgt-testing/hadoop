package org.apache.hadoop.fs;

public interface FSDataOutputStreamJVMInterface extends SyncableJVMInterface, CanSetDropBehindJVMInterface, StreamCapabilitiesJVMInterface {

    long getPos() throws java.io.IOException;

    java.io.OutputStream getWrappedStream();

    void hflush() throws java.io.IOException;

    void hsync() throws java.io.IOException;

    void sync() throws java.io.IOException;

    void setDropBehind(java.lang.Boolean arg0) throws java.io.IOException;

    boolean hasCapability(java.lang.String arg0);

    void close() throws java.io.IOException;
}

package org.apache.hadoop.fs;

import org.apache.hadoop.fs.statistics.IOStatisticsSourceJVMInterface;

public interface FSDataOutputStreamJVMInterface extends SyncableJVMInterface, CanSetDropBehindJVMInterface, StreamCapabilitiesJVMInterface, IOStatisticsSourceJVMInterface, AbortableJVMInterface {

    long getPos();

    java.lang.Object getIOStatistics();

    java.lang.String toString();

    java.io.OutputStream getWrappedStream();

    void hflush() throws java.io.IOException;

    void hsync() throws java.io.IOException;

    void setDropBehind(java.lang.Boolean arg0) throws java.io.IOException;

    boolean hasCapability(java.lang.String arg0);

    java.lang.Object abort();

    void close() throws java.io.IOException;
}

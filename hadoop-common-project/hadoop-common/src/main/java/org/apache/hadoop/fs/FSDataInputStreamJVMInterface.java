package org.apache.hadoop.fs;

import org.apache.hadoop.fs.statistics.IOStatisticsSourceJVMInterface;

public interface FSDataInputStreamJVMInterface extends SeekableJVMInterface, PositionedReadableJVMInterface, ByteBufferReadableJVMInterface, HasFileDescriptorJVMInterface, CanSetDropBehindJVMInterface, CanSetReadaheadJVMInterface, HasEnhancedByteBufferAccessJVMInterface, CanUnbufferJVMInterface, StreamCapabilitiesJVMInterface, ByteBufferPositionedReadableJVMInterface, IOStatisticsSourceJVMInterface {

    long getPos() throws java.io.IOException;

    int minSeekForVectorReads();

    int read(long arg0, java.nio.ByteBuffer arg1) throws java.io.IOException;

    java.io.FileDescriptor getFileDescriptor() throws java.io.IOException;

    java.lang.String toString();

    void setReadahead(java.lang.Long arg0) throws java.io.IOException, java.lang.UnsupportedOperationException;

    int read(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;

    int maxReadSizeForVectorReads();

    void setDropBehind(java.lang.Boolean arg0) throws java.io.IOException, java.lang.UnsupportedOperationException;

    java.nio.ByteBuffer read_bridge(java.lang.Object arg0, int arg1, java.util.EnumSet<org.apache.hadoop.fs.ReadOption> arg2) throws java.io.IOException, java.lang.UnsupportedOperationException;

    int read(java.nio.ByteBuffer arg0) throws java.io.IOException;

    void readVectored(java.util.List<? extends org.apache.hadoop.fs.FileRange> arg0, java.util.function.IntFunction<java.nio.ByteBuffer> arg1) throws java.io.IOException;

    void readFully(long arg0, byte[] arg1) throws java.io.IOException;

    java.lang.Object getIOStatistics();

    void readFully(long arg0, java.nio.ByteBuffer arg1) throws java.io.IOException;

    void seek(long arg0) throws java.io.IOException;

    java.nio.ByteBuffer read_bridge(java.lang.Object arg0, int arg1) throws java.io.IOException, java.lang.UnsupportedOperationException;

    boolean seekToNewSource(long arg0) throws java.io.IOException;

    void unbuffer();

    java.io.InputStream getWrappedStream();

    void readFully(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;

    boolean hasCapability(java.lang.String arg0);

    void releaseBuffer(java.nio.ByteBuffer arg0);
}

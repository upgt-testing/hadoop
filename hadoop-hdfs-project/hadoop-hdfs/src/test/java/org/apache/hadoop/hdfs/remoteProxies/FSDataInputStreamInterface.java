package org.apache.hadoop.hdfs.remoteProxies;

import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;
import java.util.function.IntFunction;

import org.apache.hadoop.fs.FileRange;
import org.apache.hadoop.fs.ReadOption;
import org.apache.hadoop.io.ByteBufferPool;

public interface FSDataInputStreamInterface {

    void seek(long desired);

    long getPos();

    int read(long position, byte[] buffer, int offset, int length);

    void readFully(long position, byte[] buffer, int offset, int length);

    void readFully(long position, byte[] buffer);

    boolean seekToNewSource(long targetPos);

    InputStream getWrappedStream();

    int read(ByteBuffer buf);

    FileDescriptor getFileDescriptor();

    void setReadahead(Long readahead);

    void setDropBehind(Boolean dropBehind);

    ByteBuffer read(ByteBufferPoolInterface bufferPool, int maxLength, EnumSet<ReadOption> opts);

    ByteBuffer read(ByteBufferPool bufferPool, int maxLength);

    void releaseBuffer(ByteBuffer buffer);

    void unbuffer();

    boolean hasCapability(String capability);

    String toString();

    int read(long position, ByteBuffer buf);

    void readFully(long position, ByteBuffer buf);

    IOStatisticsInterface getIOStatistics();

    int minSeekForVectorReads();

    int maxReadSizeForVectorReads();

    void readVectored(List<? extends FileRange> ranges, IntFunction<ByteBuffer> allocate);
}

package org.apache.hadoop.hdfs.remoteProxies;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.io.*;
import org.apache.hadoop.io.LongWritable;

public interface SocketOutputStreamInterface {

    void write(int b);

    void write(byte[] b, int off, int len);

    void close();

    WritableByteChannel getChannel();

    boolean isOpen();

    int write(ByteBuffer src);

    void waitForWritable();

    void transferToFully(FileChannel fileCh, long position, int count, LongWritableInterface waitForWritableTime, LongWritable transferToTime);

    void transferToFully(FileChannel fileCh, long position, int count);

    void setTimeout(int timeoutMs);
}

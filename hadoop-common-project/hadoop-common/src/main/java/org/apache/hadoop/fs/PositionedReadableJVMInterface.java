package org.apache.hadoop.fs;

public interface PositionedReadableJVMInterface {

    void readFully(long arg0, byte[] arg1) throws java.io.IOException;

    void readVectored(java.util.List<? extends org.apache.hadoop.fs.FileRange> arg0, java.util.function.IntFunction<java.nio.ByteBuffer> arg1) throws java.io.IOException;

    int minSeekForVectorReads();

    void readFully(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;

    int read(long arg0, byte[] arg1, int arg2, int arg3) throws java.io.IOException;

    int maxReadSizeForVectorReads();
}

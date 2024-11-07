package org.apache.hadoop.hdfs.remoteProxies;

public interface BufferInterface {
    RpcWritableInterface wrap(java.lang.Object arg0);
    java.nio.ByteBuffer getByteBuffer();
    <T> T newInstance(java.lang.Class<T> arg0, ConfigurationInterface arg1) throws java.io.IOException;
    BufferInterface wrap(java.nio.ByteBuffer arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void writeTo(ResponseBufferInterface arg0) throws java.io.IOException;
    int remaining();
    <T> T getValue(T arg0) throws java.io.IOException;
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    <T> T readFrom(java.nio.ByteBuffer arg0) throws java.io.IOException;
}
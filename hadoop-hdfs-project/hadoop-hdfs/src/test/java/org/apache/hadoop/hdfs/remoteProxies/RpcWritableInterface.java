package org.apache.hadoop.hdfs.remoteProxies;

public interface RpcWritableInterface {
    <T> T readFrom(java.nio.ByteBuffer arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void writeTo(ResponseBufferInterface arg0) throws java.io.IOException;
    RpcWritableInterface wrap(java.lang.Object arg0);
}
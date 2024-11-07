package org.apache.hadoop.hdfs.remoteProxies;

public interface DomainChannelInterface {
    void close() throws java.io.IOException;
    int read(java.nio.ByteBuffer arg0) throws java.io.IOException;
    boolean isOpen();
}
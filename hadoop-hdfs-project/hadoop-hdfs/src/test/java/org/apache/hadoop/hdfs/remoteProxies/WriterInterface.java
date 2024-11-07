package org.apache.hadoop.hdfs.remoteProxies;

public interface WriterInterface<U> {
    void store(U arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
}